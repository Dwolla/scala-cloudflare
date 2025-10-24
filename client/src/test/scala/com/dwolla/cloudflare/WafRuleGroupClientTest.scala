package com.dwolla.cloudflare

import cats.effect.*
import com.dwolla.cloudflare.domain.model.*
import com.dwolla.cloudflare.domain.model.wafrulegroups.*
import dwolla.cloudflare.FakeCloudflareService
import munit.{CatsEffectSuite, ScalaCheckSuite}
import org.http4s.HttpRoutes
import org.http4s.server.Router
import org.scalacheck.{Arbitrary, Gen}

class WafRuleGroupClientTest extends CatsEffectSuite with ScalaCheckSuite {

  // Common setup values and helper
  val zoneId: ZoneId = tagZoneId("zone-id")
  val wafRulePackageId: WafRulePackageId = tagWafRulePackageId("c504870194831cd12c3fc0284f294abb")
  val wafRuleGroupId: WafRuleGroupId = tagWafRuleGroupId("cfda825dfda411ea218cb70e6c88e82e")

  val authorization = CloudflareAuthorization("email", "key")

  private def buildWafRuleGroupClient(service: HttpRoutes[IO]): WafRuleGroupClient[IO] = {
    val fakeService = new FakeCloudflareService(authorization)
    WafRuleGroupClient(new StreamingCloudflareApiExecutor(fakeService.client(service), authorization))
  }

  test("list should list the waf rule groups for the given zone") {
    val fakeService = new FakeCloudflareService(authorization)
    val client = buildWafRuleGroupClient(fakeService.listWafRuleGroups(zoneId, wafRulePackageId))
    val output = client.list(zoneId, wafRulePackageId)

    val expected = List(
      WafRuleGroup(
        id = tagWafRuleGroupId("cfda825dfda411ea218cb70e6c88e82e"),
        name = WafRuleGroupName("OWASP Uri XSS Attacks"),
        description = "Cross site scripting (XSS) attacks that may result in unwanted HTML being inserted into web pages via URIs",
        mode = Mode.Off,
        package_id = tagWafRulePackageId("c504870194831cd12c3fc0284f294abb"),
        rules_count = 112,
        modified_rules_count = 0
      ),
      WafRuleGroup(
        id = tagWafRuleGroupId("d508327aee37c147e03873f79288bb1d"),
        name = WafRuleGroupName("OWASP XSS Attacks"),
        description = "Cross site scripting (XSS) attacks that may result in unwanted HTML being inserted into web pages.",
        mode = Mode.On,
        package_id = tagWafRulePackageId("c504870194831cd12c3fc0284f294abb"),
        rules_count = 112,
        modified_rules_count = 0
      )
    )

    assertIO(output.compile.toList.map(_.toSet), expected.toSet)
  }

  test("get by id should return the waf rule group with the given id") {
    val fakeService = new FakeCloudflareService(authorization)
    val client = buildWafRuleGroupClient(fakeService.getWafRuleGroupById(zoneId, wafRulePackageId, wafRuleGroupId, Mode.Off))
    val output = client.getById(zoneId, wafRulePackageId, wafRuleGroupId)

    val expected = List(WafRuleGroup(
      id = tagWafRuleGroupId("cfda825dfda411ea218cb70e6c88e82e"),
      name = WafRuleGroupName("OWASP Uri XSS Attacks"),
      description = "Cross site scripting (XSS) attacks that may result in unwanted HTML being inserted into web pages via URIs",
      mode = Mode.Off,
      package_id = tagWafRulePackageId("c504870194831cd12c3fc0284f294abb"),
      rules_count = 112,
      modified_rules_count = 0
    ))

    assertIO(output.compile.toList, expected)
  }

  test("setMode should set the mode for the given waf rule group") {
    val input = WafRuleGroup(
      id = tagWafRuleGroupId("cfda825dfda411ea218cb70e6c88e82e"),
      name = WafRuleGroupName("OWASP Uri XSS Attacks"),
      description = "Cross site scripting (XSS) attacks that may result in unwanted HTML being inserted into web pages via URIs",
      mode = Mode.Off,
      package_id = tagWafRulePackageId("c504870194831cd12c3fc0284f294abb"),
      rules_count = 112,
      modified_rules_count = 0
    )

    val fakeService = new FakeCloudflareService(authorization)
    val client = buildWafRuleGroupClient(fakeService.setModeForWafRuleGroupToOn(zoneId, input))
    val output = client.setMode(zoneId, input.package_id, input.id, Mode.On)

    val expected = List(input.copy(mode = Mode.On))

    assertIO(output.compile.toList, expected)
  }

  test("setMode should return success if waf rule group is already on") {
    val existingWafRuleGroup = WafRuleGroup(
      id = tagWafRuleGroupId("cfda825dfda411ea218cb70e6c88e82e"),
      name = WafRuleGroupName("OWASP Uri XSS Attacks"),
      description = "Cross site scripting (XSS) attacks that may result in unwanted HTML being inserted into web pages via URIs",
      mode = Mode.On,
      package_id = tagWafRulePackageId("c504870194831cd12c3fc0284f294abb"),
      rules_count = 112,
      modified_rules_count = 0
    )

    val fakeService = new FakeCloudflareService(authorization)
    val service = Router(
      "" -> fakeService.setModeForWafRuleGroupThatAlreadyHasTheSpecifiedModeValue(zoneId, existingWafRuleGroup),
      "" -> fakeService.getWafRuleGroupById(zoneId, existingWafRuleGroup.package_id, existingWafRuleGroup.id, existingWafRuleGroup.mode)
    )
    val client = buildWafRuleGroupClient(service)
    val output = client.setMode(zoneId, existingWafRuleGroup.package_id, existingWafRuleGroup.id, existingWafRuleGroup.mode)

    val expected = List(existingWafRuleGroup)

    assertIO(output.compile.toList, expected)
  }

  test("setMode should return success if waf rule group is already off") {
    val existingWafRuleGroup = WafRuleGroup(
      id = tagWafRuleGroupId("cfda825dfda411ea218cb70e6c88e82e"),
      name = WafRuleGroupName("OWASP Uri XSS Attacks"),
      description = "Cross site scripting (XSS) attacks that may result in unwanted HTML being inserted into web pages via URIs",
      mode = Mode.Off,
      package_id = tagWafRulePackageId("c504870194831cd12c3fc0284f294abb"),
      rules_count = 112,
      modified_rules_count = 0
    )

    val fakeService = new FakeCloudflareService(authorization)
    val service = Router(
      "" -> fakeService.setModeForWafRuleGroupThatAlreadyHasTheSpecifiedModeValue(zoneId, existingWafRuleGroup),
      "" -> fakeService.getWafRuleGroupById(zoneId, existingWafRuleGroup.package_id, existingWafRuleGroup.id, existingWafRuleGroup.mode)
    )
    val client = buildWafRuleGroupClient(service)
    val output = client.setMode(zoneId, existingWafRuleGroup.package_id, existingWafRuleGroup.id, existingWafRuleGroup.mode)

    val expected = List(existingWafRuleGroup)

    assertIO(output.compile.toList, expected)
  }

  test("getRuleGroupId should get the id of the waf rule group with the given name") {
    val input = WafRuleGroup(
      id = tagWafRuleGroupId("cfda825dfda411ea218cb70e6c88e82e"),
      name = WafRuleGroupName("OWASP Uri XSS Attacks"),
      description = "Cross site scripting (XSS) attacks that may result in unwanted HTML being inserted into web pages via URIs",
      mode = Mode.Off,
      package_id = tagWafRulePackageId("c504870194831cd12c3fc0284f294abb"),
      rules_count = 112,
      modified_rules_count = 0
    )

    val fakeService = new FakeCloudflareService(authorization)
    val client = buildWafRuleGroupClient(fakeService.listWafRuleGroupsByName(zoneId, wafRulePackageId, input))
    val output = client.getRuleGroupId(zoneId, wafRulePackageId, input.name)

    val expected = List(input.id)

    assertIO(output.compile.toList, expected)
  }

  // property-based: buildUri and parseUri are inverses
  private val nonEmptyAlphaNumericString = Gen.identifier
  implicit private val arbitraryZoneId: Arbitrary[ZoneId] = Arbitrary(nonEmptyAlphaNumericString.map(ZoneId(_)))
  implicit private val arbitraryWafRulePackageId: Arbitrary[WafRulePackageId] = Arbitrary(nonEmptyAlphaNumericString.map(WafRulePackageId(_)))
  implicit private val arbitraryWafRuleGroupId: Arbitrary[WafRuleGroupId] = Arbitrary(nonEmptyAlphaNumericString.map(WafRuleGroupId(_)))

  property("buildUri and parseUri should be inverses") {
    import org.scalacheck.Prop.forAll

    forAll { (zoneId: ZoneId, wafRulePackageId: WafRulePackageId, wafRuleGroupId: WafRuleGroupId) =>
      val client = new WafRuleGroupClient[IO] {
        override def list(zoneId: ZoneId, wafRulePackageId: WafRulePackageId): fs2.Stream[IO, WafRuleGroup] = ???
        override def getById(zoneId: ZoneId, wafRulePackageId: WafRulePackageId, wafRuleId: WafRuleGroupId): fs2.Stream[IO, WafRuleGroup] = ???
        override def setMode(zoneId: ZoneId, wafRulePackageId: WafRulePackageId, wafRuleId: WafRuleGroupId, mode: Mode): fs2.Stream[IO, WafRuleGroup] = ???
        override def getRuleGroupId(zoneId: ZoneId, wafRulePackageId: WafRulePackageId, name: WafRuleGroupName): fs2.Stream[IO, WafRuleGroupId] = ???
      }

      assertEquals(client.parseUri(client.buildUri(zoneId, wafRulePackageId, wafRuleGroupId).renderString), Some((zoneId, wafRulePackageId, wafRuleGroupId)))
    }
  }
}
