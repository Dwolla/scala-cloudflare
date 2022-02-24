package dwolla.cloudflare

import cats.data._
import cats.effect._
import cats.effect.testing.specs2.CatsEffect
import com.dwolla.cloudflare._
import com.dwolla.cloudflare.domain.model._
import io.circe.literal._
import org.http4s._
import org.http4s.client.Client
import org.http4s.syntax.all._
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import shapeless.tag.@@

class ZoneClientSpec
  extends Specification
    with CatsEffect {

  private def tagString[T](s: String): String @@ T = shapeless.tag[T][String](s)

  trait Setup extends Scope {
    val client = for {
      fakeExecutor <- Reader((fakeService: HttpRoutes[IO]) => new StreamingCloudflareApiExecutor[IO](Client.fromHttpApp(fakeService.orNotFound), authorization))
    } yield new ZoneClientImpl(fakeExecutor)
  }

  val authorization = CloudflareAuthorization("email", "key")
  val getZoneId = new FakeCloudflareService(authorization).listZones("dwolla.com", SampleResponses.Successes.getZones)

  "ZoneClient" should {
    "accept a domain name and find a Zone ID" in new Setup {
      private val domain = "dwolla.com"

      client(getZoneId)
        .getZoneId(domain)
        .compile
        .last
        .map(_ must beSome(tagString[ZoneIdTag]("fake-zone-id")))
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
           }"""

    }

  }

}
