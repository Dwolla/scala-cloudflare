package dwolla.cloudflare

import cats.data._
import cats.effect._
import cats.implicits._
import com.dwolla.cloudflare.CloudflareSettingFunctions._
import com.dwolla.cloudflare._
import com.dwolla.cloudflare.domain.model.ZoneSettings.{CloudflareSecurityLevel, CloudflareTlsLevel, CloudflareWaf}
import com.dwolla.cloudflare.domain.model._
import org.http4s._
import org.http4s.syntax.all._
import org.http4s.client._
import org.http4s.dsl.Http4sDsl
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import org.specs2.matcher.IOMatchers

class ZoneSettingsClientSpec(implicit ee: ExecutionEnv) extends Specification with IOMatchers {

  private val authorization = CloudflareAuthorization("email", "key")
  private val fakeCloudflareService = new FakeCloudflareService(authorization)
  private val getZoneId = fakeCloudflareService.listZones("hydragents.xyz", SampleResponses.Successes.getZones)

  trait Setup extends Scope with Http4sDsl[IO] {
    def client(csfs: CloudflareSettingFunction*) =
      for {
        fakeExecutor <- Reader((fakeService: HttpRoutes[IO]) => new StreamingCloudflareApiExecutor[IO](Client.fromHttpApp(fakeService.orNotFound), authorization))
      } yield new ZoneSettingsClientImpl(fakeExecutor, 1) {
        override val settings = csfs.toSet
      }
  }

  "Zone Settings client" should {

    "apply the TLS setting to the given domain" in new Setup {

      val zone = Zone("hydragents.xyz", CloudflareTlsLevel.FullTlsStrict, None, None)

      private val zoneSettingsClient = client(setTlsLevel)(getZoneId <+> fakeCloudflareService.setTlsLevelService("fake-zone-id", "strict"))
      private val output = zoneSettingsClient.updateSettings(zone)

      output.compile.last.unsafeToFuture() must beSome[ValidatedNel[Throwable, Unit]].like {
        case Validated.Valid(u) => u must_==(())
        case Validated.Invalid(e) => throw e.head
      }.await
    }

    "apply the security level to the given domain" in new Setup {
      val zone = Zone("hydragents.xyz", CloudflareTlsLevel.FullTlsStrict, Option(CloudflareSecurityLevel.High), None)

      private val zoneSettingsClient = client(setSecurityLevel)(getZoneId <+> fakeCloudflareService.setSecurityLevelService("fake-zone-id", "high"))
      private val output = zoneSettingsClient.updateSettings(zone)

      output.compile.last.unsafeToFuture() must beSome[ValidatedNel[Throwable, Unit]].like {
        case Validated.Valid(u) => u must_==(())
      }.await
    }

    "apply waf to the given domain" in new Setup {
      val zone = Zone("hydragents.xyz", CloudflareTlsLevel.FullTlsStrict, None, Option(CloudflareWaf.On))

      private val zoneSettingsClient = client(setSecurityLevel)(getZoneId <+> fakeCloudflareService.setWafService("fake-zone-id", "on"))
      private val output = zoneSettingsClient.updateSettings(zone)

      output.compile.last.unsafeToFuture() must beSome[ValidatedNel[Throwable, Unit]].like {
        case Validated.Valid(u) => u must_==(())
      }.await
    }

    "ignore optional settings if they're not set (indicating a custom setting)" in new Setup {
      val zone = Zone("hydragents.xyz", CloudflareTlsLevel.FullTlsStrict, None, None)

      def testOptionalSetting(cloudflareSettingFunction: CloudflareSettingFunction) = {
        val zoneSettingsClient = client(cloudflareSettingFunction)(getZoneId)
        val output = zoneSettingsClient.updateSettings(zone)

        output.compile.last.unsafeToFuture() must beSome[ValidatedNel[Throwable, Unit]].like {
          case Validated.Valid(u) => u must_==(())
        }.await
      }

      private val settingsUnderTest = List(setSecurityLevel, setWaf)
      settingsUnderTest.foreach(testOptionalSetting)
    }

    "contain the default rules for all zone settings" in new Setup {
      private val zoneSettingsClient = new ZoneSettingsClientImpl(new StreamingCloudflareApiExecutor[IO](Client.fromHttpApp(HttpRoutes.empty[IO].orNotFound), authorization), 1)

      zoneSettingsClient.settings must_==(Set(setSecurityLevel, setTlsLevel, setWaf))
    }

    "accumulate multiple errors, should they occur when updating settings" in new Setup {
      private val zoneSettingsClient = ZoneSettingsClient(new StreamingCloudflareApiExecutor[IO](Client.fromHttpApp(getZoneId.orNotFound), authorization))
      private val zone = Zone("hydragents.xyz", CloudflareTlsLevel.FullTlsStrict, Option(CloudflareSecurityLevel.High), None)

      private val output = zoneSettingsClient.updateSettings(zone)

      output.compile.last.unsafeToFuture() should beSome[ValidatedNel[Throwable, Unit]].like {
        case Validated.Invalid(nel) => nel.toList should have size greaterThanOrEqualTo(2)
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
          |      "name": "hydragents.xyz",
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


    }

  }

}
