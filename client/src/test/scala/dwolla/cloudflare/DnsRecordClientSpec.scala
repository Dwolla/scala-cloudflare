package dwolla.cloudflare

import cats._
import cats.data._
import cats.effect._
import cats.implicits._
import com.dwolla.cloudflare._
import com.dwolla.cloudflare.domain.model._
import org.http4s._
import org.http4s.client.Client
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

class DnsRecordClientSpec(implicit ee: ExecutionEnv) extends Specification {

  trait Setup extends Scope {
    val client = for {
      fakeExecutor ← Reader((fakeService: HttpService[IO]) ⇒ new StreamingCloudflareApiExecutor[IO](Client.fromHttpService(fakeService), authorization))
    } yield new DnsRecordClientImpl(fakeExecutor)
  }

  val authorization = CloudflareAuthorization("email", "key")
  val getZoneId = new FakeCloudflareService(authorization).listZones("dwolla.com", SampleResponses.Successes.getZones)

  "Cloudflare API Client lookup" should {

    "accept a domain name and find a Zone ID" in new Setup {
      private val domain = "dwolla.com"

      private val output = client(getZoneId).getZoneId(domain)
        .compile.toList.map(_.headOption).unsafeToFuture()

      output must beSome("fake-zone-id").await
    }

    "accept a domain name and return existing record" in new Setup {
      val getDnsRecordsForZone = new FakeCloudflareService(authorization).listRecordsForZone("fake-zone-id", "example.dwolla.com", SampleResponses.Successes.listDnsRecordsWithOneResult())
      val output = client(getDnsRecordsForZone <+> getZoneId)
        .getExistingDnsRecords("example.dwolla.com")
        .compile.toList.map(_.headOption).unsafeToFuture

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

    "accept a domain name and content and return existing record" in new Setup {
      val content = "different-example.dwollalabs.com"
      val getDnsRecordsForZone = new FakeCloudflareService(authorization)
        .listRecordsForZone(
          "fake-zone-id",
          "example.dwolla.com",
          SampleResponses.Successes.listDnsRecordsWithOneResult(content = content),
          contentFilter = Option(content),
        )
      val output = client(getDnsRecordsForZone <+> getZoneId)
        .getExistingDnsRecords("example.dwolla.com", content = Option(content))
        .compile.toList.map(_.headOption).unsafeToFuture

      output must beSome(IdentifiedDnsRecord(
        physicalResourceId = "https://api.cloudflare.com/client/v4/zones/fake-zone-id/dns_records/fake-resource-id",
        zoneId = "fake-zone-id",
        resourceId = "fake-resource-id",
        name = "example.dwolla.com",
        content = content,
        recordType = "CNAME",
        ttl = Option(1),
        proxied = Option(true)
      )).await
    }

    "accept a domain name and recordType and return existing record" in new Setup {
      val recordType = "A"
      val getDnsRecordsForZone = new FakeCloudflareService(authorization)
        .listRecordsForZone(
          "fake-zone-id",
          "example.dwolla.com",
          SampleResponses.Successes.listDnsRecordsWithOneResult(recordType = "A", content = "192.168.0.1"),
          recordTypeFilter = Option(recordType),
        )
      val output = client(getDnsRecordsForZone <+> getZoneId)
        .getExistingDnsRecords("example.dwolla.com", recordType = Option(recordType))
        .compile.toList.map(_.headOption).unsafeToFuture

      output must beSome(IdentifiedDnsRecord(
        physicalResourceId = "https://api.cloudflare.com/client/v4/zones/fake-zone-id/dns_records/fake-resource-id",
        zoneId = "fake-zone-id",
        resourceId = "fake-resource-id",
        name = "example.dwolla.com",
        content = "192.168.0.1",
        recordType = "A",
        ttl = Option(1),
        proxied = Option(true)
      )).await
    }

    "accept a domain name and return None when no matching record exists" in new Setup {
      val getDnsRecordsForZone = new FakeCloudflareService(authorization).listRecordsForZone("fake-zone-id", "example.dwolla.com", SampleResponses.Successes.listDnsRecordsWithNoResults)
      val output = client(getDnsRecordsForZone <+> getZoneId)
        .getExistingDnsRecords("example.dwolla.com")
        .compile.toList.map(_.headOption).unsafeToFuture

      output must beNone.await
    }

    "accept the URI of a DNS record and return it as an IdentifiedDnsRecord" in new Setup {
      private val fakeZoneId = "fake-zone-id"
      private val fakeRecordId = "fake-record-id"
      private val getDnsRecord = new FakeCloudflareService(authorization).getDnsRecordByUri(fakeZoneId, fakeRecordId)
      private val output = client(getDnsRecord)
        .getExistingDnsRecord(physicalResourceId(fakeZoneId, fakeRecordId))

      output.compile.toList.unsafeToFuture() must be_==(List(IdentifiedDnsRecord(
        physicalResourceId = s"https://api.cloudflare.com/client/v4/zones/$fakeZoneId/dns_records/$fakeRecordId",
        zoneId = fakeZoneId,
        resourceId = fakeRecordId,
        name = "example.hydragents.xyz",
        content = "content.hydragents.xyz",
        recordType = "CNAME",
      ))).await
    }

    "return an empty stream if the passed URI results in the dumb Cloudflare-equivalent of a 404" in new Setup {
      private val fakeZoneId = "fake-zone-id"
      private val fakeRecordId = "fake-record-id"
      private val getDnsRecord = new FakeCloudflareService(authorization).getDnsRecordByUri(fakeZoneId, fakeRecordId)
      private val output = client(getDnsRecord)
        .getExistingDnsRecord(physicalResourceId(fakeZoneId, "different-fake-resource-id"))

      output.compile.toList.unsafeToFuture() must be_==(List.empty).await
    }
  }

  private def physicalResourceId(zoneId: String, recordId: String): String =
    (Uri.uri("https://api.cloudflare.com") / "client" / "v4" / "zones" / zoneId / "dns_records" / recordId).toString

  "Cloudflare API client record create" should {
    "accept a DNS Record and return it with its new ID" in new Setup {
      val createDnsRecord = new FakeCloudflareService(authorization).createRecordInZone("fake-zone-id")
      val output = client(createDnsRecord <+> getZoneId)
        .createDnsRecord(UnidentifiedDnsRecord(
          name = "example.dwolla.com",
          content = "example.dwollalabs.com",
          recordType = "CNAME",
          proxied = Option(true),
        ))
        .compile
        .toList
        .unsafeToFuture()

      output must be_==(List(IdentifiedDnsRecord(
        physicalResourceId = "https://api.cloudflare.com/client/v4/zones/fake-zone-id/dns_records/fake-record-id",
        zoneId = "fake-zone-id",
        resourceId = "fake-record-id",
        name = "example.dwolla.com",
        content = "example.dwollalabs.com",
        recordType = "CNAME",
        ttl = Option(1),
        proxied = Option(true)
      ))).await
    }
  }

  "Cloudflare API client record update" should {
    "accept a DNS Record and return it with its new ID" in new Setup {
      val updateDnsRecord = new FakeCloudflareService(authorization).updateRecordInZone("fake-zone-id", "fake-record-id")
      val output = client(updateDnsRecord).updateDnsRecord(IdentifiedDnsRecord(
        physicalResourceId = "https://api.cloudflare.com/client/v4/zones/fake-zone-id/dns_records/fake-record-id",
        name = "example.dwolla.com",
        content = "new-content.dwollalabs.com",
        recordType = "CNAME",
        zoneId = "fake-zone-id",
        resourceId = "fake-record-id"
      ))
        .compile.toList.unsafeToFuture()

      output must be_==(List(IdentifiedDnsRecord(
        physicalResourceId = "https://api.cloudflare.com/client/v4/zones/fake-zone-id/dns_records/fake-record-id",
        name = "example.dwolla.com",
        content = "new-content.dwollalabs.com",
        recordType = "CNAME",
        zoneId = "fake-zone-id",
        resourceId = "fake-record-id",
        ttl = Option(1),
        proxied = Option(false)
      ))).await
    }
  }

  "Cloudflare API client delegation records delete" should {
    "accept a physical resource id and return the deleted ID" in new Setup {
      val deleteDnsRecord = new FakeCloudflareService(authorization).deleteRecordInZone("fake-zone-id", "fake-record-id")

      val output = client(deleteDnsRecord).deleteDnsRecord("https://api.cloudflare.com/client/v4/zones/fake-zone-id/dns_records/fake-record-id")
          .compile.toList.map(_.headOption).unsafeToFuture()

      output must beSome("fake-record-id").await
    }

    "throw an exception if the Record ID does not exist" in new Setup {
      val deleteDnsRecord = new FakeCloudflareService(authorization).failedDeleteRecordInZone("fake-zone-id", "fake-record-id", SampleResponses.Failures.deleteDnsRecordButIdDoesNotExist.json)

      val output = client(deleteDnsRecord).deleteDnsRecord("https://api.cloudflare.com/client/v4/zones/fake-zone-id/dns_records/fake-record-id")
          .compile.toList.attempt.unsafeToFuture()

      output must beLeft[Throwable].like {
        case ex: DnsRecordIdDoesNotExistException ⇒
          ex.getMessage must startWith("The given DNS record ID does not exist")
          ex.resourceId must_== "https://api.cloudflare.com/client/v4/zones/fake-zone-id/dns_records/fake-record-id"
      }.await
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

      def listDnsRecordsWithOneResult(content: String = "example.dwollalabs.com",
                                      recordType: String = "CNAME") =
        s"""{
           |  "result": [
           |    {
           |      "id": "fake-resource-id",
           |      "type": "$recordType",
           |      "name": "example.dwolla.com",
           |      "content": "$content",
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
           |}""".stripMargin

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
          |    "total_pages": 1,
          |    "count": 2,
          |    "total_count": 2
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
}