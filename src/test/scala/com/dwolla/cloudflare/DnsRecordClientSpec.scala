package com.dwolla.cloudflare

import java.net.URI

import com.dwolla.cloudflare.model.{IdentifiedDnsRecord, UnidentifiedDnsRecord}
import com.dwolla.testutils.httpclient.SimpleHttpRequestMatcher.http
import org.apache.http.HttpVersion.HTTP_1_1
import org.apache.http.client.HttpClient
import org.apache.http.client.methods._
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.message.{BasicHttpResponse, BasicStatusLine}
import org.apache.http.{HttpEntity, HttpResponse, StatusLine}
import org.json4s.DefaultFormats
import org.slf4j.Logger
import org.specs2.concurrent.ExecutionEnv
import org.specs2.matcher.JsonMatchers
import org.specs2.mock.Mockito
import org.specs2.mock.mockito.ArgumentCapture
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

import scala.concurrent.Promise
import scala.io.Source
import scala.language.reflectiveCalls
import scala.reflect.ClassTag

class DnsRecordClientSpec(implicit ee: ExecutionEnv) extends Specification with Mockito with JsonMatchers {

  trait Setup extends Scope {
    implicit val formats = DefaultFormats ++ org.json4s.ext.JodaTimeSerializers.all
    implicit val mockHttpClient = mock[CloseableHttpClient]
    val mockLogger = mock[Logger]
    val fakeExecutor = new CloudflareApiExecutor(CloudflareAuthorization("email", "key")) {
      override lazy val httpClient: CloseableHttpClient = mockHttpClient
    }

    val client = new DnsRecordClient(fakeExecutor) {
      override protected lazy val logger: Logger = mockLogger
    }
  }

  "Cloudflare API Client lookup" should {

    "accept a domain name and find a Zone ID" in new Setup {
      private val domain = "dwollalabs.com"
      mockGetZoneId(domain)

      private val output = client.getZoneId(domain)

      output must be_==("fake-zone-id").await
    }

    "accept a domain name and return existing record" in new Setup {
      mockGetZoneId("dwolla.com")
      mockGetDnsRecords("fake-zone-id", "example.dwolla.com", SampleResponses.Successes.listDnsRecordsWithOneResult)

      val output = client.getExistingDnsRecord("example.dwolla.com")

      output must beSome(IdentifiedDnsRecord(
        physicalResourceId = "https://api.cloudflare.com/client/v4/zones/fake-zone-id/dns_records/fake-resource-id",
        zoneId = "fake-zone-id",
        resourceId = "fake-resource-id",
        name = "example.dwolla.com",
        content = "example.dwollalabs.com",
        recordType = "CNAME",
        ttl = Option(1),
        proxied = Option(true)
      )).await
    }

    "accept a domain name and return None when no matching record exists" in new Setup {
      mockGetZoneId("dwolla.com")
      mockGetDnsRecords("fake-zone-id", "example.dwolla.com", SampleResponses.Successes.listDnsRecordsWithNoResults)

      val output = client.getExistingDnsRecord("example.dwolla.com")

      output must beNone.await
    }

    "accept a domain name and throw an exception when multiple matching records exist" in new Setup {
      mockGetZoneId("dwolla.com")
      mockGetDnsRecords("fake-zone-id", "example.dwolla.com", SampleResponses.Successes.listDnsRecordsWithManyResults)

      val output = client.getExistingDnsRecord("example.dwolla.com")

      output must throwA[MultipleCloudflareRecordsExistForDomainNameException].like {
        case ex ⇒ ex.getMessage must_==
          """Multiple DNS records exist for domain name example.dwolla.com:
            |
            | - DnsRecordDTO(Some(fake-dns-record-id-1),example.dwolla.com,example.dwollalabs.com,CNAME,Some(1),Some(false))
            | - DnsRecordDTO(Some(fake-dns-record-id-2),example.dwolla.com,example2.dwollalabs.com,CNAME,Some(1),Some(true))
            |
            |This resource refuses to process multiple records because the intention is not clear.
            |Clean up the records manually or enhance the custom resource Lambda to handle multiple records.""".stripMargin
      }.await
    }
  }

  def mockGetZoneId(domain: String, response: HttpResponse = fakeResponse(new BasicStatusLine(HTTP_1_1, 200, "Ok"), new StringEntity(SampleResponses.Successes.getZones)))
                   (implicit mockHttpClient: HttpClient) = {
    mockHttpClient.execute(http(new HttpGet(s"https://api.cloudflare.com/client/v4/zones?name=$domain&status=active"))) returns response
  }

  def mockGetDnsRecords(zone: String, name: String, responseBody: String)(implicit mockHttpClient: HttpClient): Unit = {
    val response = fakeResponse(new BasicStatusLine(HTTP_1_1, 200, "Ok"), new StringEntity(responseBody))
    mockHttpClient.execute(http(new HttpGet(s"https://api.cloudflare.com/client/v4/zones/$zone/dns_records?name=$name"))) returns response
  }

  def mockExecuteWithCaptor[T <: HttpUriRequest : ClassTag](response: HttpResponse)(implicit mockHttpClient: HttpClient): ArgumentCapture[T] = {
    val captor = capture[T]
    mockHttpClient.execute(captor) returns response

    captor
  }

  "Cloudflare API client record create" should {
    "accept a DNS Record and return it with its new ID" in new Setup {
      val captor = mockExecuteWithCaptor[HttpPost](fakeResponse(new BasicStatusLine(HTTP_1_1, 201, "Created"), new StringEntity(SampleResponses.Successes.createDnsRecord)))
      mockGetZoneId("dwolla.com")

      val output = client.createDnsRecord(UnidentifiedDnsRecord(
        name = "example.dwolla.com",
        content = "example.dwollalabs.com",
        recordType = "CNAME",
        proxied = Option(true)
      ))

      output must be_==(IdentifiedDnsRecord(
        physicalResourceId = "https://api.cloudflare.com/client/v4/zones/fake-zone-id/dns_records/fake-record-id",
        zoneId = "fake-zone-id",
        resourceId = "fake-record-id",
        name = "example.dwollalabs.com",
        content = "example.dwollalabs.com",
        recordType = "CNAME",
        ttl = Option(1),
        proxied = Option(true)
      )).await

      private val httpPost = captor.value
      httpPost.getMethod must_== "POST"
      httpPost.getURI must_== new URI("https://api.cloudflare.com/client/v4/zones/fake-zone-id/dns_records")
      private val httpEntity = httpPost.getEntity

      httpEntity.getContentType.getValue must_== "application/json"
      val postedJson = Source.fromInputStream(httpEntity.getContent).mkString

      postedJson must /("name" → "example.dwolla.com")
      postedJson must /("content" → "example.dwollalabs.com")
      postedJson must /("type" → "CNAME")
      postedJson must /("proxied" → true)
    }
  }

  "Cloudflare API client record update" should {
    "accept a DNS Record and return it with its new ID" in new Setup {
      val captor = mockExecuteWithCaptor[HttpPut](fakeResponse(new BasicStatusLine(HTTP_1_1, 200, "Ok"), new StringEntity(SampleResponses.Successes.updateDnsRecord)))
      mockGetZoneId("dwolla.com")

      val inputRecord = IdentifiedDnsRecord(
        physicalResourceId = "https://api.cloudflare.com/client/v4/zones/fake-zone-id/dns_records/fake-record-id",
        name = "example.dwolla.com",
        content = "new-content.dwollalabs.com",
        recordType = "CNAME",
        zoneId = "fake-zone-id",
        resourceId = "fake-record-id"
      )

      val output = client.updateDnsRecord(inputRecord)

      output must be_==(IdentifiedDnsRecord(
        physicalResourceId = "https://api.cloudflare.com/client/v4/zones/fake-zone-id/dns_records/fake-record-id",
        name = "example.dwolla.com",
        content = "new-content.dwollalabs.com",
        recordType = "CNAME",
        zoneId = "fake-zone-id",
        resourceId = "fake-record-id",
        ttl = Option(1),
        proxied = Option(false)
      )).await

      private val httpPut = captor.value
      httpPut.getMethod must_== "PUT"
      httpPut.getURI must_== new URI("https://api.cloudflare.com/client/v4/zones/fake-zone-id/dns_records/fake-record-id")
      private val httpEntity = httpPut.getEntity

      httpEntity.getContentType.getValue must_== "application/json"
      val postedJson = Source.fromInputStream(httpEntity.getContent).mkString

      postedJson must /("name" → "example.dwolla.com")
      postedJson must /("content" → "new-content.dwollalabs.com")
      postedJson must /("type" → "CNAME")
      postedJson must not(/("proxied" → true))
      postedJson must not(/("proxied" → false))
      postedJson must not(/("id" → be))
    }
  }

  "Cloudflare API client delegation records delete" should {
    "accept a physical resource id and return the deleted ID" in new Setup {
      val captor = mockExecuteWithCaptor[HttpDelete](fakeResponse(new BasicStatusLine(HTTP_1_1, 201, "Created"), new StringEntity(SampleResponses.Successes.deleteDnsRecord)))

      val output = client.deleteDnsRecord("https://api.cloudflare.com/client/v4/zones/fake-zone-id/dns_records/fake-record-id")

      output must be_==("fake-record-id").await

      private val httpDelete = captor.value
      httpDelete.getMethod must_== "DELETE"
      httpDelete.getURI must_== new URI("https://api.cloudflare.com/client/v4/zones/fake-zone-id/dns_records/fake-record-id")
    }

    "throw an exception if the Record ID does not exist" in new Setup {
      val failure = SampleResponses.Failures.deleteDnsRecordButIdDoesNotExist
      val captor = mockExecuteWithCaptor[HttpDelete](fakeResponse(new BasicStatusLine(HTTP_1_1, failure.statusCode, "Bad Request"), new StringEntity(failure.json)))

      val output = client.deleteDnsRecord("https://api.cloudflare.com/client/v4/zones/fake-zone-id/dns_records/fake-record-id")

      output must throwA[DnsRecordIdDoesNotExistException].like {
        case ex: DnsRecordIdDoesNotExistException ⇒
          ex.getMessage must startWith("The given DNS record ID does not exist")
          ex.resourceId must_== "https://api.cloudflare.com/client/v4/zones/fake-zone-id/dns_records/fake-record-id"
      }.await
    }
  }

  def fakeResponse(statusLine: StatusLine, entity: HttpEntity) = {
    val res = new BasicHttpResponse(statusLine) with CloseableHttpResponse {
      val promisedClose = Promise[Unit]

      override def close(): Unit = promisedClose.success(Unit)

      def isClosed: Boolean = promisedClose.isCompleted
    }

    res.setEntity(entity)

    res
  }
}

private object SampleResponses {

  object Successes {
    val getZones =
      """{
        |  "result": [
        |    {
        |      "id": "fake-zone-id",
        |      "name": "dwolla.com",
        |      "status": "active",
        |      "paused": false,
        |      "type": "full",
        |      "development_mode": 0,
        |      "name_servers": [
        |        "eric.ns.cloudflare.com",
        |        "lucy.ns.cloudflare.com"
        |      ]
        |    }
        |  ],
        |  "result_info": {
        |    "page": 1,
        |    "per_page": 20,
        |    "total_pages": 1,
        |    "count": 1,
        |    "total_count": 1
        |  },
        |  "success": true,
        |  "errors": [],
        |  "messages": []
        |}
      """.stripMargin

    val listDnsRecordsWithOneResult =
      """{
        |  "result": [
        |    {
        |      "id": "fake-resource-id",
        |      "type": "CNAME",
        |      "name": "example.dwolla.com",
        |      "content": "example.dwollalabs.com",
        |      "proxiable": true,
        |      "proxied": true,
        |      "ttl": 1,
        |      "locked": false,
        |      "zone_id": "fake-zone-id",
        |      "zone_name": "dwolla.com",
        |      "modified_on": "2016-12-20T18:45:30.268036Z",
        |      "created_on": "2016-12-20T18:45:30.268036Z",
        |      "meta": {
        |        "auto_added": false
        |      }
        |    }
        |  ],
        |  "result_info": {
        |    "page": 1,
        |    "per_page": 20,
        |    "total_pages": 1,
        |    "count": 1,
        |    "total_count": 1
        |  },
        |  "success": true,
        |  "errors": [],
        |  "messages": []
        |}
      """.stripMargin

    val listDnsRecordsWithNoResults =
      """{
        |  "result": [],
        |  "result_info": {
        |    "page": 1,
        |    "per_page": 20,
        |    "total_pages": 0,
        |    "count": 0,
        |    "total_count": 0
        |  },
        |  "success": true,
        |  "errors": [],
        |  "messages": []
        |}
      """.stripMargin

    val listDnsRecordsWithManyResults =
      """{
        |  "result": [
        |    {
        |      "id": "fake-dns-record-id-1",
        |      "type": "CNAME",
        |      "name": "example.dwolla.com",
        |      "content": "example.dwollalabs.com",
        |      "proxiable": true,
        |      "proxied": false,
        |      "ttl": 1,
        |      "locked": false,
        |      "zone_id": "fake-zone-id",
        |      "zone_name": "dwolla.com",
        |      "modified_on": "2016-12-20T18:45:19.525129Z",
        |      "created_on": "2016-12-20T18:45:19.525129Z",
        |      "meta": {
        |        "auto_added": false
        |      }
        |    },
        |    {
        |      "id": "fake-dns-record-id-2",
        |      "type": "CNAME",
        |      "name": "example.dwolla.com",
        |      "content": "example2.dwollalabs.com",
        |      "proxiable": true,
        |      "proxied": true,
        |      "ttl": 1,
        |      "locked": false,
        |      "zone_id": "fake-zone-id",
        |      "zone_name": "dwolla.com",
        |      "modified_on": "2016-12-20T18:45:30.268036Z",
        |      "created_on": "2016-12-20T18:45:30.268036Z",
        |      "meta": {
        |        "auto_added": false
        |      }
        |    }
        |  ],
        |  "result_info": {
        |    "page": 1,
        |    "per_page": 2,
        |    "total_pages": 31,
        |    "count": 2,
        |    "total_count": 61
        |  },
        |  "success": true,
        |  "errors": [],
        |  "messages": []
        |}
      """.stripMargin

    val createDnsRecord =
      """{
        |  "success": true,
        |  "errors": [],
        |  "messages": [],
        |  "result": {
        |    "id": "fake-record-id",
        |    "type": "CNAME",
        |    "name": "example.dwollalabs.com",
        |    "content": "example.dwollalabs.com",
        |    "proxiable": true,
        |    "proxied": true,
        |    "ttl": 1,
        |    "locked": false,
        |    "zone_id": "fake-zone-id",
        |    "zone_name": "dwolla.com",
        |    "created_on": "2014-01-01T05:20:00.12345Z",
        |    "modified_on": "2014-01-01T05:20:00.12345Z",
        |    "data": {}
        |  }
        |}
      """.stripMargin

    val updateDnsRecord =
      """{
        |  "success": true,
        |  "errors": [],
        |  "messages": [],
        |  "result": {
        |    "id": "fake-record-id",
        |    "type": "CNAME",
        |    "name": "example.dwolla.com",
        |    "content": "new-content.dwollalabs.com",
        |    "proxiable": true,
        |    "proxied": false,
        |    "ttl": 1,
        |    "locked": false,
        |    "zone_id": "fake-zone-id",
        |    "zone_name": "dwolla.com",
        |    "created_on": "2014-01-01T05:20:00.12345Z",
        |    "modified_on": "2014-01-01T05:20:00.12345Z",
        |    "data": {}
        |  }
        |}
      """.stripMargin

    val deleteDnsRecord =
      """{
        |  "success": true,
        |  "errors": [],
        |  "messages": [],
        |  "result": {
        |    "id": "fake-record-id"
        |  }
        |}
      """.stripMargin
  }

  object Failures {

    case class Failure(statusCode: Int, json: String)

    val deleteDnsRecordButIdDoesNotExist = Failure(400,
      """{
        |  "success": false,
        |  "errors": [
        |    {
        |      "code": 1032,
        |      "message": "Invalid DNS record identifier"
        |    }
        |  ],
        |  "messages": [],
        |  "result": null
        |}
      """.stripMargin)

    val validationError = Failure(400,
      """{
        |    "success": false,
        |    "errors": [
        |        {
        |            "code": 1004,
        |            "message": "DNS Validation Error",
        |            "error_chain": [
        |                {
        |                    "code": 9021,
        |                    "message": "Invalid TTL. Must be between 120 and 2,147,483,647 seconds, or 1 for automatic"
        |                }
        |            ]
        |        }
        |    ],
        |    "messages": [],
        |    "result": null
        |}
      """.stripMargin)
  }

}
