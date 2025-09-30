package com.dwolla.cloudflare

import cats.effect.*
import com.dwolla.cloudflare.domain.model.*
import com.dwolla.cloudflare.domain.model.wafrulepackages.*
import dwolla.cloudflare.FakeCloudflareService
import munit.{CatsEffectSuite, ScalaCheckSuite}
import org.http4s.HttpRoutes
import org.scalacheck.{Arbitrary, Gen}

class WafRulePackageClientTest extends CatsEffectSuite with ScalaCheckSuite {

  // Common setup values and helper
  val zoneId: ZoneId = tagZoneId("zone-id")
  val wafRulePackageId: WafRulePackageId = tagWafRulePackageId("c504870194831cd12c3fc0284f294abb")

  val authorization = CloudflareAuthorization("email", "key")

  private def buildWafRulePackageClient(service: HttpRoutes[IO]): WafRulePackageClient[IO] = {
    val fakeService = new FakeCloudflareService(authorization)
    WafRulePackageClient(new StreamingCloudflareApiExecutor(fakeService.client(service), authorization))
  }

  test("list should list the waf rule packages for the given zone") {
    val fakeService = new FakeCloudflareService(authorization)
    val client = buildWafRulePackageClient(fakeService.listWafRulePackages(zoneId))
    val output = client.list(zoneId)

    val expected = List(
      WafRulePackage(
        id = tagWafRulePackageId("059f5a550fffae09cbb4072edf101bd2"),
        name = WafRulePackageName("USER"),
        description = None,
        zone_id = tagZoneId("90940840480ba654a3a5ddcdc5d741f9"),
        detection_mode = DetectionMode.Traditional,
        sensitivity = None,
        action_mode = None
      ),
      WafRulePackage(
        id = tagWafRulePackageId("c504870194831cd12c3fc0284f294abb"),
        name = WafRulePackageName("OWASP ModSecurity Core Rule Set"),
        description = Option("OWASP Core Ruleset (2013) provides protection against common attack categories, including SQL Injection and Cross-Site Scripting."),
        zone_id = tagZoneId("90940840480ba654a3a5ddcdc5d741f9"),
        detection_mode = DetectionMode.Anomaly,
        sensitivity = Option(Sensitivity.Off),
        action_mode = Option(ActionMode.Challenge)
      )
    )

    assertIO(output.compile.toList, expected)
  }

  test("get by id should return the waf rule package with the given id") {
    val fakeService = new FakeCloudflareService(authorization)
    val client = buildWafRulePackageClient(fakeService.getWafRulePackageById(zoneId, wafRulePackageId))
    val output = client.getById(zoneId, wafRulePackageId)

    val expected = List(WafRulePackage(
      id = tagWafRulePackageId("059f5a550fffae09cbb4072edf101bd2"),
      name = WafRulePackageName("USER"),
      description = None,
      zone_id = tagZoneId("90940840480ba654a3a5ddcdc5d741f9"),
      detection_mode = DetectionMode.Traditional,
      sensitivity = None,
      action_mode = None
    ))

    assertIO(output.compile.toList, expected)
  }

  test("edit should set the sensitivity and action_mode for the given waf rule package") {
    val input = WafRulePackage(
      id = tagWafRulePackageId("059f5a550fffae09cbb4072edf101bd2"),
      name = WafRulePackageName("USER"),
      description = None,
      zone_id = tagZoneId("90940840480ba654a3a5ddcdc5d741f9"),
      detection_mode = DetectionMode.Traditional,
      sensitivity = None,
      action_mode = None
    )

    val updatedSensitivity = Sensitivity.Medium
    val updatedActionMode = ActionMode.Challenge

    val fakeService = new FakeCloudflareService(authorization)
    val client = buildWafRulePackageClient(fakeService.editWafRulePackage(zoneId, input, updatedSensitivity, updatedActionMode))
    val output = client.edit(zoneId, input.id, updatedSensitivity, updatedActionMode)

    val expected = List(input.copy(sensitivity = Option(updatedSensitivity), action_mode = Option(updatedActionMode)))

    assertIO(output.compile.toList, expected)
  }

  test("getRulePackageId should get the id of the waf rule package with the given name") {
    val input = WafRulePackage(
      id = tagWafRulePackageId("059f5a550fffae09cbb4072edf101bd2"),
      name = WafRulePackageName("USER"),
      description = None,
      zone_id = tagZoneId("90940840480ba654a3a5ddcdc5d741f9"),
      detection_mode = DetectionMode.Traditional,
      sensitivity = None,
      action_mode = None
    )

    val fakeService = new FakeCloudflareService(authorization)
    val client = buildWafRulePackageClient(fakeService.listWafRulePackagesByName(zoneId, input))
    val output = client.getRulePackageId(zoneId, input.name)

    val expected = List(input.id)

    assertIO(output.compile.toList, expected)
  }

  // property-based: buildUri and parseUri are inverses
  private val nonEmptyAlphaNumericString = Gen.identifier
  implicit private val arbitraryZoneId: Arbitrary[ZoneId] = Arbitrary(nonEmptyAlphaNumericString.map(ZoneId(_)))
  implicit private val arbitraryWafRulePackageId: Arbitrary[WafRulePackageId] = Arbitrary(nonEmptyAlphaNumericString.map(WafRulePackageId(_)))

  property("buildUri and parseUri should be inverses") {
    import org.scalacheck.Prop.forAll

    forAll { (zoneId: ZoneId, wafRulePackageId: WafRulePackageId) =>
      val client = new WafRulePackageClient[IO] {
        override def list(zoneId: ZoneId): fs2.Stream[IO, WafRulePackage] = ???
        override def getById(zoneId: ZoneId, wafRulePackageId: WafRulePackageId): fs2.Stream[IO, WafRulePackage] = ???
        override def edit(zoneId: ZoneId, wafRulePackageId: WafRulePackageId, sensitivity: Sensitivity, actionMode: ActionMode): fs2.Stream[IO, WafRulePackage] = ???
        override def getRulePackageId(zoneId: ZoneId, name: WafRulePackageName): fs2.Stream[IO, WafRulePackageId] = ???
      }

      assertEquals(client.parseUri(client.buildUri(zoneId, wafRulePackageId).renderString), Some((zoneId, wafRulePackageId)))
    }
  }
}
