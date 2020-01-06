package com.dwolla.cloudflare

import cats.effect._
import com.dwolla.cloudflare.domain.model._
import com.dwolla.cloudflare.domain.model.wafrulegroups._
import dwolla.cloudflare.FakeCloudflareService
import org.http4s.HttpRoutes
import org.scalacheck.{Arbitrary, Gen}
import org.specs2.ScalaCheck
import org.specs2.matcher.{IOMatchers, Matchers}
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

class WafRuleGroupClientTest extends Specification with ScalaCheck with IOMatchers with Matchers {

  trait Setup extends Scope {
    val zoneId: ZoneId = tagZoneId("zone-id")
    val wafRulePackageId: WafRulePackageId = tagWafRulePackageId("c504870194831cd12c3fc0284f294abb")
    val wafRuleGroupId = tagWafRuleGroupId("cfda825dfda411ea218cb70e6c88e82e")

    val authorization = CloudflareAuthorization("email", "key")
    val fakeService = new FakeCloudflareService(authorization)

    protected def buildWafRuleGroupClient(service: HttpRoutes[IO]): WafRuleGroupClient[IO] =
      WafRuleGroupClient(new StreamingCloudflareApiExecutor(fakeService.client(service), authorization))

  }

  "list" should {

    "list the waf rule groups for the given zone" in new Setup {
      private val client = buildWafRuleGroupClient(fakeService.listWafRuleGroups(zoneId, wafRulePackageId))
      private val output = client.list(zoneId, wafRulePackageId)

      output.compile.toList must returnValue(containTheSameElementsAs(List(
        WafRuleGroup(
          id = tagWafRuleGroupId("cfda825dfda411ea218cb70e6c88e82e"),
          name = tagWafRuleGroupName("OWASP Uri XSS Attacks"),
          description = "Cross site scripting (XSS) attacks that may result in unwanted HTML being inserted into web pages via URIs",
          mode = Mode.Off,
          package_id = tagWafRulePackageId("c504870194831cd12c3fc0284f294abb"),
          rules_count = 112,
          modified_rules_count = 0
        ),
        WafRuleGroup(
          id = tagWafRuleGroupId("d508327aee37c147e03873f79288bb1d"),
          name = tagWafRuleGroupName("OWASP XSS Attacks"),
          description = "Cross site scripting (XSS) attacks that may result in unwanted HTML being inserted into web pages.",
          mode = Mode.On,
          package_id = tagWafRulePackageId("c504870194831cd12c3fc0284f294abb"),
          rules_count = 112,
          modified_rules_count = 0
        )
      )))
    }
  }

  "get by id" should {

    "return the waf rule group with the given id" in new Setup {
      private val client = buildWafRuleGroupClient(fakeService.getWafRuleGroupById(zoneId, wafRulePackageId, wafRuleGroupId))
      private val output = client.getById(zoneId, wafRulePackageId, wafRuleGroupId)

      output.compile.toList must returnValue(List(WafRuleGroup(
        id = tagWafRuleGroupId("cfda825dfda411ea218cb70e6c88e82e"),
        name = tagWafRuleGroupName("OWASP Uri XSS Attacks"),
        description = "Cross site scripting (XSS) attacks that may result in unwanted HTML being inserted into web pages via URIs",
        mode = Mode.Off,
        package_id = tagWafRulePackageId("c504870194831cd12c3fc0284f294abb"),
        rules_count = 112,
        modified_rules_count = 0
      )))
    }
  }

  "setMode" should {
    "set the mode for the given waf rule group" in new Setup {
      private val input = WafRuleGroup(
        id = tagWafRuleGroupId("cfda825dfda411ea218cb70e6c88e82e"),
        name = tagWafRuleGroupName("OWASP Uri XSS Attacks"),
        description = "Cross site scripting (XSS) attacks that may result in unwanted HTML being inserted into web pages via URIs",
        mode = Mode.Off,
        package_id = tagWafRulePackageId("c504870194831cd12c3fc0284f294abb"),
        rules_count = 112,
        modified_rules_count = 0
      )

      private val client = buildWafRuleGroupClient(fakeService.setModeForWafRuleGroupToOn(zoneId, input))
      private val output = client.setMode(zoneId, input.package_id, input.id, Mode.On)

      output.compile.toList must returnValue(List(input.copy(mode = Mode.On)))
    }
  }

  "getRuleGroupId" should {

    "get the id of the waf rule group with the given name" in new Setup {
      private val input = WafRuleGroup(
        id = tagWafRuleGroupId("cfda825dfda411ea218cb70e6c88e82e"),
        name = tagWafRuleGroupName("OWASP Uri XSS Attacks"),
        description = "Cross site scripting (XSS) attacks that may result in unwanted HTML being inserted into web pages via URIs",
        mode = Mode.Off,
        package_id = tagWafRulePackageId("c504870194831cd12c3fc0284f294abb"),
        rules_count = 112,
        modified_rules_count = 0
      )

      private val client = buildWafRuleGroupClient(fakeService.listWafRuleGroupsByName(zoneId, wafRulePackageId, input))
      private val output = client.getRuleGroupId(zoneId, wafRulePackageId, input.name)

      output.compile.toList must returnValue(List(input.id))
    }
  }

  "buildUri and parseUri" should {
    val nonEmptyAlphaNumericString = Gen.asciiPrintableStr.suchThat(_.nonEmpty)
    implicit val arbitraryZoneId = Arbitrary(nonEmptyAlphaNumericString.map(shapeless.tag[ZoneIdTag][String]))
    implicit val arbitraryWafRulePackageId = Arbitrary(nonEmptyAlphaNumericString.map(shapeless.tag[WafRulePackageIdTag][String]))
    implicit val arbitraryWafRuleGroupId = Arbitrary(nonEmptyAlphaNumericString.map(shapeless.tag[WafRuleGroupIdTag][String]))

    "be the inverse of each other" >> { prop { (zoneId: ZoneId, wafRulePackageId: WafRulePackageId, wafRuleGroupId: WafRuleGroupId) =>
      val client = new WafRuleGroupClient[IO] {
        override def list(zoneId: ZoneId, wafRulePackageId: WafRulePackageId): fs2.Stream[IO, WafRuleGroup] = ???
        override def getById(zoneId: ZoneId, wafRulePackageId: WafRulePackageId, wafRuleId: WafRuleGroupId): fs2.Stream[IO, WafRuleGroup] = ???
        override def setMode(zoneId: ZoneId, wafRulePackageId: WafRulePackageId, wafRuleId: WafRuleGroupId, mode: Mode): fs2.Stream[IO, WafRuleGroup] = ???
        override def getRuleGroupId(zoneId: ZoneId, wafRulePackageId: WafRulePackageId, name: WafRuleGroupName): fs2.Stream[IO, WafRuleGroupId] = ???
      }

      client.parseUri(client.buildUri(zoneId, wafRulePackageId, wafRuleGroupId)) must beSome((zoneId, wafRulePackageId, wafRuleGroupId))
    }}
  }
}
