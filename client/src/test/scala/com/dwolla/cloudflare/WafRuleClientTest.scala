package com.dwolla.cloudflare

import cats.effect._
import com.dwolla.cloudflare.domain.model._
import com.dwolla.cloudflare.domain.model.wafrules._
import dwolla.cloudflare.FakeCloudflareService
import org.http4s.HttpRoutes
import org.http4s.server.Router
import org.scalacheck.{Arbitrary, Gen}
import org.specs2.ScalaCheck
import org.specs2.matcher.{IOMatchers, Matchers}
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

class WafRuleClientTest extends Specification with ScalaCheck with IOMatchers with Matchers {

  trait Setup extends Scope {
    val zoneId: ZoneId = tagZoneId("zone-id")
    val wafRulePackageId: WafRulePackageId = tagWafRulePackageId("c504870194831cd12c3fc0284f294abb")
    val wafRuleId = tagWafRuleId("958019")

    val authorization = CloudflareAuthorization("email", "key")
    val fakeService = new FakeCloudflareService(authorization)

    protected def buildWafRuleClient(service: HttpRoutes[IO]): WafRuleClient[IO] =
      WafRuleClient(new StreamingCloudflareApiExecutor(fakeService.client(service), authorization))

  }

  "list" should {

    "list the waf rules for the given zone" in new Setup {
      private val client = buildWafRuleClient(fakeService.listWafRules(zoneId, wafRulePackageId))
      private val output = client.list(zoneId, wafRulePackageId)

      output.compile.toList must returnValue(containTheSameElementsAs(List(
        WafRule(
          id = tagWafRuleId("958019"),
          description = "Cross-site Scripting (XSS) Attack",
          priority = tagWafRulePriority("14"),
          package_id = wafRulePackageId,
          group = WafRuleGroup(
            id = tagWafRuleGroupId("d508327aee37c147e03873f79288bb1d"),
            name = WafRuleGroupName("OWASP XSS Attacks")
          ),
          mode = Mode.On,
          allowed_modes = List(
            Mode.On,
            Mode.Off
          )
        ),
        WafRule(
          id = tagWafRuleId("958020"),
          description = "Cross-site Scripting (XSS) Attack",
          priority = tagWafRulePriority("56"),
          package_id = wafRulePackageId,
          group = WafRuleGroup(
            id = tagWafRuleGroupId("d508327aee37c147e03873f79288bb1d"),
            name = WafRuleGroupName("OWASP XSS Attacks")
          ),
          mode = Mode.On,
          allowed_modes = List(
            Mode.On,
            Mode.Off
          )
        )
      )))
    }

  }

  "get by id" should {

    "return the waf rule with the given id" in new Setup {
      private val client = buildWafRuleClient(fakeService.getWafRuleById(zoneId, wafRulePackageId, wafRuleId, Mode.On))
      private val output = client.getById(zoneId, wafRulePackageId, wafRuleId)

      output.compile.toList must returnValue(List(WafRule(
        id = tagWafRuleId("958019"),
        description = "Cross-site Scripting (XSS) Attack",
        priority = tagWafRulePriority("14"),
        package_id = wafRulePackageId,
        group = WafRuleGroup(
          id = tagWafRuleGroupId("d508327aee37c147e03873f79288bb1d"),
          name = WafRuleGroupName("OWASP XSS Attacks")
        ),
        mode = Mode.On,
        allowed_modes = List(
          Mode.On,
          Mode.Off
        )
      )))
    }

  }

  "setMode" should {
    "set the mode for the given waf rule" in new Setup {
      private val input = WafRule(
        id = tagWafRuleId("958019"),
        description = "Cross-site Scripting (XSS) Attack",
        priority = tagWafRulePriority("14"),
        package_id = wafRulePackageId,
        group = WafRuleGroup(
          id = tagWafRuleGroupId("d508327aee37c147e03873f79288bb1d"),
          name = WafRuleGroupName("OWASP XSS Attacks")
        ),
        mode = Mode.Off,
        allowed_modes = List(
          Mode.On,
          Mode.Off
        )
      )

      private val client = buildWafRuleClient(fakeService.setModeForWafRuleToOn(zoneId, input))
      private val output = client.setMode(zoneId, input.package_id, input.id, Mode.On)

      output.compile.toList must returnValue(List(input.copy(mode = Mode.On)))
    }

    "return success if waf rule is already on" in new Setup {
      private val existingWafRule = WafRule(
        id = tagWafRuleId("958019"),
        description = "Cross-site Scripting (XSS) Attack",
        priority = tagWafRulePriority("14"),
        package_id = wafRulePackageId,
        group = WafRuleGroup(
          id = tagWafRuleGroupId("d508327aee37c147e03873f79288bb1d"),
          name = WafRuleGroupName("OWASP XSS Attacks")
        ),
        mode = Mode.On,
        allowed_modes = List(
          Mode.On,
          Mode.Off
        )
      )

      private val service = Router(
        "" -> fakeService.setModeForWafRuleThatAlreadyHasTheSpecifiedModeValue(zoneId, existingWafRule),
        "" -> fakeService.getWafRuleById(zoneId, existingWafRule.package_id, existingWafRule.id, existingWafRule.mode)
      )
      private val client = buildWafRuleClient(service)
      private val output = client.setMode(zoneId, existingWafRule.package_id, existingWafRule.id, existingWafRule.mode)

      output.compile.toList must returnValue(List(existingWafRule))
    }

    "return success if waf rule is already off" in new Setup {
      private val input = WafRule(
        id = tagWafRuleId("958019"),
        description = "Cross-site Scripting (XSS) Attack",
        priority = tagWafRulePriority("14"),
        package_id = wafRulePackageId,
        group = WafRuleGroup(
          id = tagWafRuleGroupId("d508327aee37c147e03873f79288bb1d"),
          name = WafRuleGroupName("OWASP XSS Attacks")
        ),
        mode = Mode.Off,
        allowed_modes = List(
          Mode.On,
          Mode.Off
        )
      )

      private val service = Router(
        "" -> fakeService.setModeForWafRuleThatAlreadyHasTheSpecifiedModeValue(zoneId, input),
        "" -> fakeService.getWafRuleById(zoneId, wafRulePackageId, input.id, input.mode)
      )
      private val client = buildWafRuleClient(service)
      private val output = client.setMode(zoneId, input.package_id, input.id, Mode.Off)

      output.compile.toList must returnValue(List(input.copy(mode = Mode.Off)))
    }
  }

  "buildUri and parseUri" should {
    val nonEmptyAlphaNumericString = Gen.asciiPrintableStr.suchThat(_.nonEmpty)
    implicit val arbitraryZoneId = Arbitrary(nonEmptyAlphaNumericString.map(shapeless.tag[ZoneIdTag][String]))
    implicit val arbitraryWafRulePackageId = Arbitrary(nonEmptyAlphaNumericString.map(shapeless.tag[WafRulePackageIdTag][String]))
    implicit val arbitraryWafRuleId = Arbitrary(nonEmptyAlphaNumericString.map(shapeless.tag[WafRuleIdTag][String]))

    "be the inverse of each other" >> { prop { (zoneId: ZoneId, wafRulePackageId: WafRulePackageId, wafRuleId: WafRuleId) =>
      val client = new WafRuleClient[IO] {
        override def list(zoneId: ZoneId, wafRulePackageId: WafRulePackageId): fs2.Stream[IO, WafRule] = ???
        override def getById(zoneId: ZoneId, wafRulePackageId: WafRulePackageId, wafRuleId: WafRuleId): fs2.Stream[IO, WafRule] = ???
        override def setMode(zoneId: ZoneId, wafRulePackageId: WafRulePackageId, wafRuleId: WafRuleId, mode: Mode): fs2.Stream[IO, WafRule] = ???
      }

      client.parseUri(client.buildUri(zoneId, wafRulePackageId, wafRuleId)) must beSome((zoneId, wafRulePackageId, wafRuleId))
    }}
  }
}
