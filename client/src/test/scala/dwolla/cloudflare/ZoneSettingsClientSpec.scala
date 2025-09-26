package dwolla.cloudflare

import cats.data.*
import cats.effect.*
import cats.syntax.all.*
import com.dwolla.cloudflare.*
import com.dwolla.cloudflare.CloudflareSettingFunctions.*
import com.dwolla.cloudflare.domain.model.*
import com.dwolla.cloudflare.domain.model.ZoneSettings.{CloudflareSecurityLevel, CloudflareTlsLevel, CloudflareWaf}
import io.circe.literal.*
import munit.CatsEffectSuite
import org.http4s.*
import org.http4s.client.*
import org.http4s.dsl.Http4sDsl
import org.http4s.syntax.all.*

class ZoneSettingsClientSpec extends CatsEffectSuite with Http4sDsl[IO] {

  private val authorization = CloudflareAuthorization("email", "key")
  private val fakeCloudflareService = new FakeCloudflareService(authorization)
  private val getZoneId = fakeCloudflareService.listZones("hydragents.xyz", SampleResponses.Successes.getZones)

  private def client(csfs: CloudflareSettingFunction*) =
    for {
      fakeExecutor <- Reader((fakeService: HttpRoutes[IO]) => new StreamingCloudflareApiExecutor[IO](Client.fromHttpApp(fakeService.orNotFound), authorization))
    } yield new ZoneSettingsClientImpl(fakeExecutor, 1) {
      override val settings = csfs.toSet
    }

  test("Zone Settings client should apply the TLS setting to the given domain") {
    val zone = Zone("hydragents.xyz", CloudflareTlsLevel.FullTlsStrict, None, None)

    val zoneSettingsClient = client(setTlsLevel)(getZoneId <+> fakeCloudflareService.setTlsLevelService("fake-zone-id", "strict"))
    val output = zoneSettingsClient.updateSettings(zone)

    output.compile.last.map {
      case Some(Validated.Valid(())) => assertEquals((), ())
      case Some(Validated.Invalid(e)) => fail(e.toList.mkString(", "))
      case None => fail("Expected Some result")
    }
  }

  test("Zone Settings client should apply the security level to the given domain") {
    val zone = Zone("hydragents.xyz", CloudflareTlsLevel.FullTlsStrict, Option(CloudflareSecurityLevel.High), None)

    val zoneSettingsClient = client(setSecurityLevel)(getZoneId <+> fakeCloudflareService.setSecurityLevelService("fake-zone-id", "high"))
    val output = zoneSettingsClient.updateSettings(zone)

    output.compile.last.map {
      case Some(Validated.Valid(())) => assertEquals((), ())
      case Some(Validated.Invalid(e)) => fail(e.toList.mkString(", "))
      case None => fail("Expected Some result")
    }
  }

  test("Zone Settings client should apply waf to the given domain") {
    val zone = Zone("hydragents.xyz", CloudflareTlsLevel.FullTlsStrict, None, Option(CloudflareWaf.On))

    val zoneSettingsClient = client(setSecurityLevel)(getZoneId <+> fakeCloudflareService.setWafService("fake-zone-id", "on"))
    val output = zoneSettingsClient.updateSettings(zone)

    output.compile.last.map {
      case Some(Validated.Valid(())) => assertEquals((), ())
      case Some(Validated.Invalid(e)) => fail(e.toList.mkString(", "))
      case None => fail("Expected Some result")
    }
  }

  test("Zone Settings client should ignore optional settings if they're not set (indicating a custom setting)") {
    val zone = Zone("hydragents.xyz", CloudflareTlsLevel.FullTlsStrict, None, None)

    def testOptionalSetting(cloudflareSettingFunction: CloudflareSettingFunction) = {
      val zoneSettingsClient = client(cloudflareSettingFunction)(getZoneId)
      val output = zoneSettingsClient.updateSettings(zone)

      output.compile.last.map {
        case Some(Validated.Valid(())) => assertEquals((), ())
        case Some(Validated.Invalid(e)) => fail(e.toList.mkString(", "))
        case None => fail("Expected Some result")
      }
    }

    val settingsUnderTest = List(setSecurityLevel, setWaf)
    settingsUnderTest.traverse_(fn => testOptionalSetting(fn))
  }

  test("Zone Settings client should contain the default rules for all zone settings") {
    val zoneSettingsClient = new ZoneSettingsClientImpl(new StreamingCloudflareApiExecutor[IO](Client.fromHttpApp(HttpRoutes.empty[IO].orNotFound), authorization), 1)
    assertEquals(zoneSettingsClient.settings, Set(setSecurityLevel, setTlsLevel, setWaf))
  }

  test("Zone Settings client should accumulate multiple errors, should they occur when updating settings") {
    val zoneSettingsClient = ZoneSettingsClient(new StreamingCloudflareApiExecutor[IO](Client.fromHttpApp(getZoneId.orNotFound), authorization))
    val zone = Zone("hydragents.xyz", CloudflareTlsLevel.FullTlsStrict, Option(CloudflareSecurityLevel.High), None)

    val output = zoneSettingsClient.updateSettings(zone)

    output.compile.last.map {
      case Some(Validated.Invalid(nel)) => assert(nel.toList.size >= 2)
      case Some(Validated.Valid(_)) => fail("Expected errors but got valid result")
      case None => fail("Expected Some result")
    }
  }

  private object SampleResponses {

    object Successes {
      val getZones =
        json"""{
            "result": [
              {
                "id": "fake-zone-id",
                "name": "hydragents.xyz",
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


    }

  }
}
