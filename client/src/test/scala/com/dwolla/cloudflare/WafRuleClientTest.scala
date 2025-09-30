package com.dwolla.cloudflare

import cats.effect.*
import com.dwolla.cloudflare.domain.model.*
import com.dwolla.cloudflare.domain.model.wafrules.*
import dwolla.cloudflare.FakeCloudflareService
import org.http4s.HttpRoutes
import org.http4s.server.Router
import org.scalacheck.{Arbitrary, Gen}
import munit.CatsEffectSuite
import munit.ScalaCheckSuite

class WafRuleClientTest extends CatsEffectSuite with ScalaCheckSuite {

  // Common setup values and helper
  val zoneId: ZoneId = tagZoneId("zone-id")
  val wafRulePackageId: WafRulePackageId = tagWafRulePackageId("c504870194831cd12c3fc0284f294abb")
  val wafRuleId: WafRuleId = tagWafRuleId("958019")

  val authorization = CloudflareAuthorization("email", "key")

  private def buildWafRuleClient(service: HttpRoutes[IO]): WafRuleClient[IO] = {
    val fakeService = new FakeCloudflareService(authorization)
    WafRuleClient(new StreamingCloudflareApiExecutor(fakeService.client(service), authorization))
  }

  test("list should list the waf rules for the given zone") {
    val fakeService = new FakeCloudflareService(authorization)
    val client = buildWafRuleClient(fakeService.listWafRules(zoneId, wafRulePackageId))
    val output = client.list(zoneId, wafRulePackageId)

    val expected = List(
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
        allowed_modes = List(Mode.On, Mode.Off)
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
        allowed_modes = List(Mode.On, Mode.Off)
      )
    )

    assertIO(output.compile.toList.map(_.toSet), expected.toSet)
  }

  test("get by id should return the waf rule with the given id") {
    val fakeService = new FakeCloudflareService(authorization)
    val client = buildWafRuleClient(fakeService.getWafRuleById(zoneId, wafRulePackageId, wafRuleId, Mode.On))
    val output = client.getById(zoneId, wafRulePackageId, wafRuleId)

    val expected = List(WafRule(
      id = tagWafRuleId("958019"),
      description = "Cross-site Scripting (XSS) Attack",
      priority = tagWafRulePriority("14"),
      package_id = wafRulePackageId,
      group = WafRuleGroup(
        id = tagWafRuleGroupId("d508327aee37c147e03873f79288bb1d"),
        name = WafRuleGroupName("OWASP XSS Attacks")
      ),
      mode = Mode.On,
      allowed_modes = List(Mode.On, Mode.Off)
    ))

    assertIO(output.compile.toList, expected)
  }

  test("setMode should set the mode for the given waf rule") {
    val input = WafRule(
      id = tagWafRuleId("958019"),
      description = "Cross-site Scripting (XSS) Attack",
      priority = tagWafRulePriority("14"),
      package_id = wafRulePackageId,
      group = WafRuleGroup(
        id = tagWafRuleGroupId("d508327aee37c147e03873f79288bb1d"),
        name = WafRuleGroupName("OWASP XSS Attacks")
      ),
      mode = Mode.Off,
      allowed_modes = List(Mode.On, Mode.Off)
    )

    val fakeService = new FakeCloudflareService(authorization)
    val client = buildWafRuleClient(fakeService.setModeForWafRuleToOn(zoneId, input))
    val output = client.setMode(zoneId, input.package_id, input.id, Mode.On)

    val expected = List(input.copy(mode = Mode.On))

    assertIO(output.compile.toList, expected)
  }

  test("setMode should return success if waf rule is already on") {
    val existingWafRule = WafRule(
      id = tagWafRuleId("958019"),
      description = "Cross-site Scripting (XSS) Attack",
      priority = tagWafRulePriority("14"),
      package_id = wafRulePackageId,
      group = WafRuleGroup(
        id = tagWafRuleGroupId("d508327aee37c147e03873f79288bb1d"),
        name = WafRuleGroupName("OWASP XSS Attacks")
      ),
      mode = Mode.On,
      allowed_modes = List(Mode.On, Mode.Off)
    )

    val fakeService = new FakeCloudflareService(authorization)
    val service = Router(
      "" -> fakeService.setModeForWafRuleThatAlreadyHasTheSpecifiedModeValue(zoneId, existingWafRule),
      "" -> fakeService.getWafRuleById(zoneId, existingWafRule.package_id, existingWafRule.id, existingWafRule.mode)
    )
    val client = buildWafRuleClient(service)
    val output = client.setMode(zoneId, existingWafRule.package_id, existingWafRule.id, existingWafRule.mode)

    val expected = List(existingWafRule)

    assertIO(output.compile.toList, expected)
  }

  test("setMode should return success if waf rule is already off") {
    val input = WafRule(
      id = tagWafRuleId("958019"),
      description = "Cross-site Scripting (XSS) Attack",
      priority = tagWafRulePriority("14"),
      package_id = wafRulePackageId,
      group = WafRuleGroup(
        id = tagWafRuleGroupId("d508327aee37c147e03873f79288bb1d"),
        name = WafRuleGroupName("OWASP XSS Attacks")
      ),
      mode = Mode.Off,
      allowed_modes = List(Mode.On, Mode.Off)
    )

    val fakeService = new FakeCloudflareService(authorization)
    val service = Router(
      "" -> fakeService.setModeForWafRuleThatAlreadyHasTheSpecifiedModeValue(zoneId, input),
      "" -> fakeService.getWafRuleById(zoneId, wafRulePackageId, input.id, input.mode)
    )
    val client = buildWafRuleClient(service)
    val output = client.setMode(zoneId, input.package_id, input.id, Mode.Off)

    val expected = List(input.copy(mode = Mode.Off))

    assertIO(output.compile.toList, expected)
  }

  // property-based: buildUri and parseUri are inverses
  private val nonEmptyAlphaNumericString = Gen.identifier
  implicit private val arbitraryZoneId: Arbitrary[ZoneId] = Arbitrary(nonEmptyAlphaNumericString.map(ZoneId(_)))
  implicit private val arbitraryWafRulePackageId: Arbitrary[WafRulePackageId] = Arbitrary(nonEmptyAlphaNumericString.map(WafRulePackageId(_)))
  implicit private val arbitraryWafRuleId: Arbitrary[WafRuleId] = Arbitrary(nonEmptyAlphaNumericString.map(WafRuleId(_)))

  property("buildUri and parseUri should be inverses") {
    import org.scalacheck.Prop.forAll

    forAll { (zoneId: ZoneId, wafRulePackageId: WafRulePackageId, wafRuleId: WafRuleId) =>
      val client = new WafRuleClient[IO] {
        override def list(zoneId: ZoneId, wafRulePackageId: WafRulePackageId): fs2.Stream[IO, WafRule] = ???
        override def getById(zoneId: ZoneId, wafRulePackageId: WafRulePackageId, wafRuleId: WafRuleId): fs2.Stream[IO, WafRule] = ???
        override def setMode(zoneId: ZoneId, wafRulePackageId: WafRulePackageId, wafRuleId: WafRuleId, mode: Mode): fs2.Stream[IO, WafRule] = ???
      }

      assertEquals(client.parseUri(client.buildUri(zoneId, wafRulePackageId, wafRuleId).renderString), Some((zoneId, wafRulePackageId, wafRuleId)))
    }
  }
}
