package dwolla.cloudflare

import cats.effect.*
import cats.syntax.all.*
import com.dwolla.cloudflare.{*, given}
import com.dwolla.cloudflare.domain.model.Exceptions.RecordAlreadyExists
import com.dwolla.cloudflare.domain.model.*
import fs2.Stream
import io.circe.literal.*
import org.http4s.*
import org.http4s.client.Client
import org.http4s.syntax.all.*
import munit.CatsEffectSuite
import natchez.Trace.Implicits.noop

class DnsRecordClientSpec extends CatsEffectSuite {
  private val authorization = CloudflareAuthorization("email", "key")
  private val getZoneId = new FakeCloudflareService(authorization).listZones("dwolla.com", SampleResponses.Successes.getZones)

  private def client(fakeService: HttpRoutes[IO]): DnsRecordClient[Stream[IO, *]] =
    DnsRecordClient(new StreamingCloudflareApiExecutor[IO](Client.fromHttpApp(fakeService.orNotFound), authorization))

  test("Cloudflare API Client lookup should accept a domain name and return existing record") {
    val getDnsRecordsForZone = new FakeCloudflareService(authorization).listRecordsForZone(ZoneId("fake-zone-id"), "example.dwolla.com", SampleResponses.Successes.listDnsRecordsWithOneResult())
    val output = client(getDnsRecordsForZone <+> getZoneId)
      .getExistingDnsRecords("example.dwolla.com")
      .compile
      .last

    val expected = Some(IdentifiedDnsRecord(
      physicalResourceId = PhysicalResourceId("https://api.cloudflare.com/client/v4/zones/fake-zone-id/dns_records/fake-resource-id"),
      zoneId = ZoneId("fake-zone-id"),
      resourceId = ResourceId("fake-resource-id"),
      name = "example.dwolla.com",
      content = "example.dwollalabs.com",
      recordType = "CNAME",
      ttl = Option(1),
      proxied = Option(true)
    ))

    assertIO(output, expected)
  }

  test("Cloudflare API Client lookup should accept a domain name and content and return existing record") {
    val content = "different-example.dwollalabs.com"
    val getDnsRecordsForZone = new FakeCloudflareService(authorization)
      .listRecordsForZone(
        ZoneId("fake-zone-id"),
        "example.dwolla.com",
        SampleResponses.Successes.listDnsRecordsWithOneResult(content = content),
        contentFilter = Option(content),
      )
    val output = client(getDnsRecordsForZone <+> getZoneId)
      .getExistingDnsRecords("example.dwolla.com", content = Option(content))
      .compile
      .last

    val expected = Some(IdentifiedDnsRecord(
      physicalResourceId = PhysicalResourceId("https://api.cloudflare.com/client/v4/zones/fake-zone-id/dns_records/fake-resource-id"),
      zoneId = ZoneId("fake-zone-id"),
      resourceId = ResourceId("fake-resource-id"),
      name = "example.dwolla.com",
      content = content,
      recordType = "CNAME",
      ttl = Option(1),
      proxied = Option(true)
    ))

    assertIO(output, expected)
  }

  test("Cloudflare API Client lookup should accept a domain name and recordType and return existing record") {
    val recordType = "A"
    val getDnsRecordsForZone = new FakeCloudflareService(authorization)
      .listRecordsForZone(
        ZoneId("fake-zone-id"),
        "example.dwolla.com",
        SampleResponses.Successes.listDnsRecordsWithOneResult(recordType = "A", content = "192.168.0.1"),
        recordTypeFilter = Option(recordType),
      )
    val output = client(getDnsRecordsForZone <+> getZoneId)
      .getExistingDnsRecords("example.dwolla.com", recordType = Option(recordType))
      .compile
      .last

    val expected = Some(IdentifiedDnsRecord(
      physicalResourceId = PhysicalResourceId("https://api.cloudflare.com/client/v4/zones/fake-zone-id/dns_records/fake-resource-id"),
      zoneId = ZoneId("fake-zone-id"),
      resourceId = ResourceId("fake-resource-id"),
      name = "example.dwolla.com",
      content = "192.168.0.1",
      recordType = "A",
      ttl = Option(1),
      proxied = Option(true)
    ))

    assertIO(output, expected)
  }

  test("Cloudflare API Client lookup should accept a domain name and return None when no matching record exists") {
    val getDnsRecordsForZone = new FakeCloudflareService(authorization).listRecordsForZone(ZoneId("fake-zone-id"), "example.dwolla.com", SampleResponses.Successes.listDnsRecordsWithNoResults)
    val output = client(getDnsRecordsForZone <+> getZoneId).getExistingDnsRecords("example.dwolla.com").compile.last

    assertIO(output, None)
  }

  test("Cloudflare API Client lookup should accept the URI of a DNS record and return it as an IdentifiedDnsRecord") {
    val fakeZoneId = ZoneId("fake-zone-id")
    val fakeRecordId = "fake-record-id"
    val getDnsRecord = new FakeCloudflareService(authorization).getDnsRecordByUri(fakeZoneId, fakeRecordId)
    val output = client(getDnsRecord).getByUri(physicalResourceId(fakeZoneId, fakeRecordId)).compile.toList

    val expected = List(IdentifiedDnsRecord(
      physicalResourceId = PhysicalResourceId(s"https://api.cloudflare.com/client/v4/zones/$fakeZoneId/dns_records/$fakeRecordId"),
      zoneId = fakeZoneId,
      resourceId = ResourceId(fakeRecordId),
      name = "example.hydragents.xyz",
      content = "content.hydragents.xyz",
      recordType = "CNAME",
    ))

    assertIO(output, expected)
  }

  test("Cloudflare API Client lookup should return an empty stream if the passed URI results in the dumb Cloudflare-equivalent of a 404") {
    val fakeZoneId = ZoneId("fake-zone-id")
    val fakeRecordId = "fake-record-id"
    val getDnsRecord = new FakeCloudflareService(authorization).getDnsRecordByUri(fakeZoneId, fakeRecordId)
    val output = client(getDnsRecord).getByUri(physicalResourceId(fakeZoneId, "different-fake-resource-id")).compile.toList

    assertIO(output, Nil)
  }

  private def physicalResourceId(zoneId: ZoneId, recordId: String): String =
    (uri"https://api.cloudflare.com" / "client" / "v4" / "zones" / zoneId / "dns_records" / recordId).toString

  test("Cloudflare API client record create should accept a DNS Record and return it with its new ID") {
    val createDnsRecord = new FakeCloudflareService(authorization).createRecordInZone(ZoneId("fake-zone-id"))
    val output = client(createDnsRecord <+> getZoneId)
      .createDnsRecord(UnidentifiedDnsRecord(
        name = "example.dwolla.com",
        content = "example.dwollalabs.com",
        recordType = "CNAME",
        proxied = Option(true),
      ))
      .compile
      .toList

    val expected = List(IdentifiedDnsRecord(
      physicalResourceId = PhysicalResourceId("https://api.cloudflare.com/client/v4/zones/fake-zone-id/dns_records/fake-record-id"),
      zoneId = ZoneId("fake-zone-id"),
      resourceId = ResourceId("fake-record-id"),
      name = "example.dwolla.com",
      content = "example.dwollalabs.com",
      recordType = "CNAME",
      ttl = Option(1),
      proxied = Option(true)
    ))

    assertIO(output, expected)
  }

  test("Cloudflare API client record create should return a failed stream if the record already exists") {
    val createDnsRecordFailure = new FakeCloudflareService(authorization).createRecordThatAlreadyExists(ZoneId("fake-zone-id"))

    val io = client(createDnsRecordFailure <+> getZoneId)
      .createDnsRecord(
        UnidentifiedDnsRecord(
          name = "example.dwolla.com",
          content = "example.dwollalabs.com",
          recordType = "TXT",
        ))
      .compile
      .toList

    interceptIO[RecordAlreadyExists.type](io).void
  }

  test("Cloudflare API client record update should accept a DNS Record and return it with its new ID (update)") {
    val updateDnsRecord = new FakeCloudflareService(authorization).updateRecordInZone(ZoneId("fake-zone-id"), "fake-record-id")
    val output = client(updateDnsRecord)
      .updateDnsRecord(IdentifiedDnsRecord(
        physicalResourceId = PhysicalResourceId("https://api.cloudflare.com/client/v4/zones/fake-zone-id/dns_records/fake-record-id"),
        name = "example.dwolla.com",
        content = "new-content.dwollalabs.com",
        recordType = "CNAME",
        zoneId = ZoneId("fake-zone-id"),
        resourceId = ResourceId("fake-record-id"),
      ))
      .compile
      .toList

    val expected = List(IdentifiedDnsRecord(
      physicalResourceId = PhysicalResourceId("https://api.cloudflare.com/client/v4/zones/fake-zone-id/dns_records/fake-record-id"),
      name = "example.dwolla.com",
      content = "new-content.dwollalabs.com",
      recordType = "CNAME",
      zoneId = ZoneId("fake-zone-id"),
      resourceId = ResourceId("fake-record-id"),
      ttl = Option(1),
      proxied = Option(false)
    ))

    assertIO(output, expected)
  }

  test("Cloudflare API client delegation records delete should accept a physical resource id and return the deleted ID") {
    val deleteDnsRecord = new FakeCloudflareService(authorization).deleteRecordInZone(ZoneId("fake-zone-id"), "fake-record-id")

    val output = client(deleteDnsRecord).deleteDnsRecord("https://api.cloudflare.com/client/v4/zones/fake-zone-id/dns_records/fake-record-id").compile.last

    assertIO(output, Some(PhysicalResourceId("fake-record-id")))
  }

  test("Cloudflare API client delegation records delete should throw an exception if the Record ID does not exist") {
    val deleteDnsRecord = new FakeCloudflareService(authorization).failedDeleteRecordInZone(ZoneId("fake-zone-id"), "fake-record-id", SampleResponses.Failures.deleteDnsRecordButIdDoesNotExist.json)

    val io = client(deleteDnsRecord)
      .deleteDnsRecord("https://api.cloudflare.com/client/v4/zones/fake-zone-id/dns_records/fake-record-id")
      .compile
      .toList

    interceptIO[DnsRecordIdDoesNotExistException](io).map { ex =>
      assert(ex.getMessage.startsWith("The given DNS record ID does not exist"))
      assertEquals(ex.resourceId, "https://api.cloudflare.com/client/v4/zones/fake-zone-id/dns_records/fake-record-id")
    }
  }

  private object SampleResponses {

    object Successes {
      val getZones =
        json"""{
             "result": [
               {
                 "id": "fake-zone-id",
                 "name": "dwolla.com",
                 "status": "active",
                 "paused": false,
                 "type": "full",
                 "development_mode": 0,
                 "name_servers": [
                   "eric.ns.cloudflare.com",
                   "lucy.ns.cloudflare.com"
                 ]
               }
             ],
             "result_info": {
               "page": 1,
               "per_page": 20,
               "total_pages": 1,
               "count": 1,
               "total_count": 1
             },
             "success": true,
             "errors": [],
             "messages": []
           }
        """
      val listDnsRecordsWithNoResults =
        json"""{
             "result": [],
             "result_info": {
               "page": 1,
               "per_page": 20,
               "total_pages": 0,
               "count": 0,
               "total_count": 0
             },
             "success": true,
             "errors": [],
             "messages": []
           }
        """.noSpaces
      val listDnsRecordsWithManyResults =
        json"""{
             "result": [
               {
                 "id": "fake-dns-record-id-1",
                 "type": "CNAME",
                 "name": "example.dwolla.com",
                 "content": "example.dwollalabs.com",
                 "proxiable": true,
                 "proxied": false,
                 "ttl": 1,
                 "locked": false,
                 "zone_id": "fake-zone-id",
                 "zone_name": "dwolla.com",
                 "modified_on": "2016-12-20T18:45:19.525129Z",
                 "created_on": "2016-12-20T18:45:19.525129Z",
                 "meta": {
                   "auto_added": false
                 }
               },
               {
                 "id": "fake-dns-record-id-2",
                 "type": "CNAME",
                 "name": "example.dwolla.com",
                 "content": "example2.dwollalabs.com",
                 "proxiable": true,
                 "proxied": true,
                 "ttl": 1,
                 "locked": false,
                 "zone_id": "fake-zone-id",
                 "zone_name": "dwolla.com",
                 "modified_on": "2016-12-20T18:45:30.268036Z",
                 "created_on": "2016-12-20T18:45:30.268036Z",
                 "meta": {
                   "auto_added": false
                 }
               }
             ],
             "result_info": {
               "page": 1,
               "per_page": 2,
               "total_pages": 1,
               "count": 2,
               "total_count": 2
             },
             "success": true,
             "errors": [],
             "messages": []
           }
        """.noSpaces
      val createDnsRecord =
        json"""{
             "success": true,
             "errors": [],
             "messages": [],
             "result": {
               "id": "fake-record-id",
               "type": "CNAME",
               "name": "example.dwollalabs.com",
               "content": "example.dwollalabs.com",
               "proxiable": true,
               "proxied": true,
               "ttl": 1,
               "locked": false,
               "zone_id": "fake-zone-id",
               "zone_name": "dwolla.com",
               "created_on": "2014-01-01T05:20:00.12345Z",
               "modified_on": "2014-01-01T05:20:00.12345Z",
               "data": {}
             }
           }
        """.noSpaces
      val updateDnsRecord =
        json"""{
             "success": true,
             "errors": [],
             "messages": [],
             "result": {
               "id": "fake-record-id",
               "type": "CNAME",
               "name": "example.dwolla.com",
               "content": "new-content.dwollalabs.com",
               "proxiable": true,
               "proxied": false,
               "ttl": 1,
               "locked": false,
               "zone_id": "fake-zone-id",
               "zone_name": "dwolla.com",
               "created_on": "2014-01-01T05:20:00.12345Z",
               "modified_on": "2014-01-01T05:20:00.12345Z",
               "data": {}
             }
           }
        """.noSpaces
      val deleteDnsRecord =
        json"""{
             "success": true,
             "errors": [],
             "messages": [],
             "result": {
               "id": "fake-record-id"
             }
           }
        """.noSpaces

      def listDnsRecordsWithOneResult(content: String = "example.dwollalabs.com",
                                      recordType: String = "CNAME") =
        json"""{
              "result": [
                {
                  "id": "fake-resource-id",
                  "type": $recordType,
                  "name": "example.dwolla.com",
                  "content": $content,
                  "proxiable": true,
                  "proxied": true,
                  "ttl": 1,
                  "locked": false,
                  "zone_id": "fake-zone-id",
                  "zone_name": "dwolla.com",
                  "modified_on": "2016-12-20T18:45:30.268036Z",
                  "created_on": "2016-12-20T18:45:30.268036Z",
                  "meta": {
                    "auto_added": false
                  }
                }
              ],
              "result_info": {
                "page": 1,
                "per_page": 20,
                "total_pages": 1,
                "count": 1,
                "total_count": 1
              },
              "success": true,
              "errors": [],
              "messages": []
            }""".noSpaces
    }

    object Failures {

      val deleteDnsRecordButIdDoesNotExist = Failure(400,
        json"""{
             "success": false,
             "errors": [
               {
                 "code": 1032,
                 "message": "Invalid DNS record identifier"
               }
             ],
             "messages": [],
             "result": null
           }
        """.noSpaces)
      val validationError = Failure(400,
        json"""{
               "success": false,
               "errors": [
                   {
                       "code": 1004,
                       "message": "DNS Validation Error",
                       "error_chain": [
                           {
                               "code": 9021,
                               "message": "Invalid TTL. Must be between 120 and 2,147,483,647 seconds, or 1 for automatic"
                           }
                       ]
                   }
               ],
               "messages": [],
               "result": null
           }
        """.noSpaces)

      case class Failure(statusCode: Int, json: String)
    }

  }
}
