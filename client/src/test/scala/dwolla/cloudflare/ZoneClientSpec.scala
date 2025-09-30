package dwolla.cloudflare

import cats.data.*
import cats.effect.*
import com.dwolla.cloudflare.*
import com.dwolla.cloudflare.domain.model.*
import io.circe.literal.*
import munit.CatsEffectSuite
import org.http4s.*
import org.http4s.client.Client
import org.http4s.syntax.all.*

class ZoneClientSpec extends CatsEffectSuite {

  private val authorization = CloudflareAuthorization("email", "key")
  private val getZoneId = new FakeCloudflareService(authorization).listZones("dwolla.com", SampleResponses.Successes.getZones)

  private def client: Reader[HttpRoutes[IO], ZoneClient[IO]] = for {
    fakeExecutor <- Reader((fakeService: HttpRoutes[IO]) => new StreamingCloudflareApiExecutor[IO](Client.fromHttpApp(fakeService.orNotFound), authorization))
  } yield new ZoneClientImpl(fakeExecutor)

  test("ZoneClient should accept a domain name and find a Zone ID") {
    val domain = "dwolla.com"

    val output = client(getZoneId)
      .getZoneId(domain)
      .compile
      .last

    assertIO(output, Some(ZoneId("fake-zone-id")))
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
           }"""

    }

  }
}
