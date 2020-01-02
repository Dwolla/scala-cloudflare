package com.dwolla.cloudflare

import cats.effect._
import com.dwolla.cloudflare.domain.model._
import com.dwolla.cloudflare.domain.model.wafrulepackages._
import dwolla.cloudflare.FakeCloudflareService
import org.http4s.HttpRoutes
import org.scalacheck.{Arbitrary, Gen}
import org.specs2.ScalaCheck
import org.specs2.matcher.{IOMatchers, Matchers}
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

class WafRulePackageClientTest extends Specification with ScalaCheck with IOMatchers with Matchers {

  trait Setup extends Scope {
    val zoneId: ZoneId = tagZoneId("zone-id")
    val wafRulePackageId: WafRulePackageId = tagWafRulePackageId("c504870194831cd12c3fc0284f294abb")

    val authorization = CloudflareAuthorization("email", "key")
    val fakeService = new FakeCloudflareService(authorization)

    protected def buildWafRulePackageClient(service: HttpRoutes[IO]): WafRulePackageClient[IO] =
      WafRulePackageClient(new StreamingCloudflareApiExecutor(fakeService.client(service), authorization))

  }

  "list" should {

    "list the waf rule packages for the given zone" in new Setup {
      private val client = buildWafRulePackageClient(fakeService.listWafRulePackages(zoneId))
      private val output = client.list(zoneId)

      output.compile.toList must returnValue(containTheSameElementsAs(List(
        WafRulePackage(
          id = tagWafRulePackageId("059f5a550fffae09cbb4072edf101bd2"),
          name = tagWafRulePackageName("USER"),
          description = None,
          zone_id = tagZoneId("90940840480ba654a3a5ddcdc5d741f9"),
          detection_mode = DetectionMode.Traditional,
          sensitivity = None,
          action_mode = None
        ),
        WafRulePackage(
          id = tagWafRulePackageId("c504870194831cd12c3fc0284f294abb"),
          name = tagWafRulePackageName("OWASP ModSecurity Core Rule Set"),
          description = Option("OWASP Core Ruleset (2013) provides protection against common attack categories, including SQL Injection and Cross-Site Scripting."),
          zone_id = tagZoneId("90940840480ba654a3a5ddcdc5d741f9"),
          detection_mode = DetectionMode.Anomaly,
          sensitivity = Option(Sensitivity.Off),
          action_mode = Option(ActionMode.Challenge)
        )
      )))
    }
  }

  "get by id" should {

    "return the waf rule package with the given id" in new Setup {
      private val client = buildWafRulePackageClient(fakeService.getWafRulePackageById(zoneId, wafRulePackageId))
      private val output = client.getById(zoneId, wafRulePackageId)

      output.compile.toList must returnValue(List(WafRulePackage(
        id = tagWafRulePackageId("059f5a550fffae09cbb4072edf101bd2"),
        name = tagWafRulePackageName("USER"),
        description = None,
        zone_id = tagZoneId("90940840480ba654a3a5ddcdc5d741f9"),
        detection_mode = DetectionMode.Traditional,
        sensitivity = None,
        action_mode = None
      )))
    }
  }

  "edit" should {
    "set the sensitivity and action_mode for the given waf rule package" in new Setup {
      private val input = WafRulePackage(
        id = tagWafRulePackageId("059f5a550fffae09cbb4072edf101bd2"),
        name = tagWafRulePackageName("USER"),
        description = None,
        zone_id = tagZoneId("90940840480ba654a3a5ddcdc5d741f9"),
        detection_mode = DetectionMode.Traditional,
        sensitivity = None,
        action_mode = None
      )

      private val updatedSensitivity = Sensitivity.Medium
      private val updatedActionMode = ActionMode.Challenge

      private val client = buildWafRulePackageClient(fakeService.editWafRulePackage(zoneId, input, updatedSensitivity, updatedActionMode))
      private val output = client.edit(zoneId, input.id, updatedSensitivity, updatedActionMode)

      output.compile.toList must returnValue(List(input.copy(sensitivity = Option(updatedSensitivity), action_mode = Option(updatedActionMode))))
    }
  }

  "getRulePackageId" should {

    "get the id of the waf rule package with the given name" in new Setup {
      private val input = WafRulePackage(
        id = tagWafRulePackageId("059f5a550fffae09cbb4072edf101bd2"),
        name = tagWafRulePackageName("USER"),
        description = None,
        zone_id = tagZoneId("90940840480ba654a3a5ddcdc5d741f9"),
        detection_mode = DetectionMode.Traditional,
        sensitivity = None,
        action_mode = None
      )

      private val client = buildWafRulePackageClient(fakeService.listWafRulePackagesByName(zoneId, input))
      private val output = client.getRulePackageId(zoneId, input.name)

      output.compile.toList must returnValue(List(input.id))
    }
  }

  "buildUri and parseUri" should {
    val nonEmptyAlphaNumericString = Gen.asciiPrintableStr.suchThat(_.nonEmpty)
    implicit val arbitraryZoneId = Arbitrary(nonEmptyAlphaNumericString.map(shapeless.tag[ZoneIdTag][String]))
    implicit val arbitraryWafRulePackageId = Arbitrary(nonEmptyAlphaNumericString.map(shapeless.tag[WafRulePackageIdTag][String]))

    "be the inverse of each other" >> { prop { (zoneId: ZoneId, wafRulePackageId: WafRulePackageId) =>
      val client = new WafRulePackageClient[IO] {
        override def list(zoneId: ZoneId): fs2.Stream[IO, WafRulePackage] = ???
        override def getById(zoneId: ZoneId, wafRulePackageId: WafRulePackageId): fs2.Stream[IO, WafRulePackage] = ???
        override def edit(zoneId: ZoneId, wafRulePackageId: WafRulePackageId, sensitivity: Sensitivity, actionMode: ActionMode): fs2.Stream[IO, WafRulePackage] = ???
        override def getRulePackageId(zoneId: ZoneId, name: WafRulePackageName): fs2.Stream[IO, WafRulePackageId] = ???
      }

      client.parseUri(client.buildUri(zoneId, wafRulePackageId)) must beSome((zoneId, wafRulePackageId))
    }}
  }
}
