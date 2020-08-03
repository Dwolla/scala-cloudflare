package com.dwolla.cloudflare

import java.time.Instant

import cats.effect._
import com.dwolla.cloudflare.domain.model.Exceptions.UnexpectedCloudflareErrorException
import com.dwolla.cloudflare.domain.model._
import com.dwolla.cloudflare.domain.model.firewallrules._
import com.dwolla.cloudflare.domain.model.filters._
import dwolla.cloudflare.FakeCloudflareService
import org.http4s.HttpRoutes
import org.scalacheck.{Arbitrary, Gen}
import org.specs2.ScalaCheck
import org.specs2.matcher.{IOMatchers, Matchers}
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

class FirewallRuleClientTest extends Specification with ScalaCheck with IOMatchers with Matchers {

  trait Setup extends Scope {
    val zoneId: ZoneId = tagZoneId("zone-id")
    val firewallRuleId = tagFirewallRuleId("ccbfa4a0b26b4ffa8a006e8b11557397")
    val filterId = tagFilterId("d5266c8daa9443e081e5207f64763836")

    val authorization = CloudflareAuthorization("email", "key")
    val fakeService = new FakeCloudflareService(authorization)

    protected def buildFirewallRuleClient(service: HttpRoutes[IO]): FirewallRuleClient[IO] =
      FirewallRuleClient(new StreamingCloudflareApiExecutor(fakeService.client(service), authorization))

  }

  "list" should {

    "list the firewall rules for the given zone" in new Setup {
      private val client = buildFirewallRuleClient(fakeService.listFirewallRules(zoneId))
      private val output = client.list(zoneId)

      output.compile.toList must returnValue(containTheSameElementsAs(List(
        FirewallRule(
          id = Option("ccbfa4a0b26b4ffa8a006e8b11557397").map(tagFirewallRuleId),
          paused = false,
          description = Option("rule1"),
          action = Action.Log,
          priority = tagFirewallRulePriority(1),
          filter = FirewallRuleFilter(
            id = Option("d5266c8daa9443e081e5207f64763836").map(tagFilterId),
            expression = Option(tagFilterExpression("(cf.bot_management.verified_bot)")),
            paused = Option(false)
          ),
          created_on = Option("2019-12-14T01:38:21Z").map(Instant.parse),
          modified_on = Option("2019-12-14T01:38:21Z").map(Instant.parse)
        ),
        FirewallRule(
          id = Option("c41d348b8ff64bc8a7f4f8b58c986c4c").map(tagFirewallRuleId),
          paused = false,
          description = Option("rule2"),
          action = Action.Challenge,
          priority = tagFirewallRulePriority(2),
          filter = FirewallRuleFilter(
            id = Option("308d8c703fa14939b563c84db4320fee").map(tagFilterId),
            expression = Option(tagFilterExpression("(ip.src ne 0.0.0.0)")),
            paused = Option(false)
          ),
          created_on = Option("2019-12-14T01:39:06Z").map(Instant.parse),
          modified_on = Option("2019-12-14T01:39:06Z").map(Instant.parse)
        )
      )))
    }

  }

  "get by id" should {

    "return the firewall rule with the given id" in new Setup {
      private val client = buildFirewallRuleClient(fakeService.getFirewallRuleById(zoneId, firewallRuleId))
      private val output = client.getById(zoneId, firewallRuleId: String)

      output.compile.toList must returnValue(List(FirewallRule(
        id = Option("ccbfa4a0b26b4ffa8a006e8b11557397").map(tagFirewallRuleId),
        paused = false,
        description = Option("rule1"),
        action = Action.Log,
        priority = tagFirewallRulePriority(1),
        filter = FirewallRuleFilter(
          id = Option("d5266c8daa9443e081e5207f64763836").map(tagFilterId),
          expression = Option(tagFilterExpression("(cf.bot_management.verified_bot)")),
          paused = Option(false)
        ),
        created_on = Option("2019-12-14T01:38:21Z").map(Instant.parse),
        modified_on = Option("2019-12-14T01:38:21Z").map(Instant.parse)
      )))
    }

  }

  "create" should {
    val input = FirewallRule(
      filter = FirewallRuleFilter(
        expression = Option(tagFilterExpression("(cf.bot_management.verified_bot)")),
        paused = Option(false)
      ),
      action = Action.Log,
      priority = tagFirewallRulePriority(1),
      paused = false
    )

    "send the json object and return its value" in new Setup {
      private val client = buildFirewallRuleClient(fakeService.createFirewallRule(zoneId, firewallRuleId))
      private val output = client.create(zoneId, input)

      output.compile.toList must returnValue(List(input.copy(
        id = Option(firewallRuleId),
        created_on = Option("2019-12-14T01:38:21Z").map(Instant.parse),
        modified_on = Option("2019-12-14T01:38:21Z").map(Instant.parse),
      )))
    }

    "raise a reasonable error if Cloudflare's business rules are violated" in new Setup {
      private val client = buildFirewallRuleClient(fakeService.createFirewallRuleFails)
      private val output = client.create(zoneId, input)

      output.attempt.compile.toList must returnValue(List(
        Left(
          UnexpectedCloudflareErrorException(
            List(Error(None, "products is only valid for the 'bypass' action"))
          )
        )
      ))
    }
  }

  "update" should {
    "update the given firewall rule" in new Setup {
      private val input = FirewallRule(
        id = Option(firewallRuleId),
        filter = FirewallRuleFilter(
          id = Option(filterId),
          expression = Option(tagFilterExpression("(cf.bot_management.verified_bot)")),
          paused = Option(false)
        ),
        action = Action.Log,
        priority = tagFirewallRulePriority(1),
        paused = false
      )

      private val client = buildFirewallRuleClient(fakeService.updateFirewallRule(zoneId, firewallRuleId))
      private val output = client.update(zoneId, input)

      output.compile.toList must returnValue(List(input.copy(modified_on = Option("2019-12-14T01:39:58Z").map(Instant.parse))))
    }

    "raise an exception when trying to update an unidentified firewall rule" in new Setup {
      private val input = FirewallRule(
        id = None,
        filter = FirewallRuleFilter(
          id = Option(filterId),
          expression = Option(tagFilterExpression("(cf.bot_management.verified_bot)")),
          paused = Option(false)
        ),
        action = Action.Log,
        priority = tagFirewallRulePriority(1),
        paused = false
      )

      private val client = buildFirewallRuleClient(fakeService.updateFirewallRule(zoneId, firewallRuleId))
      private val output = client.update(zoneId, input)

      output.attempt.compile.toList must returnValue(List(
        Left(CannotUpdateUnidentifiedFirewallRule(input))
      ))
    }
  }

  "delete" should {
    "delete the given firewall rule" in new Setup {
      private val client = buildFirewallRuleClient(fakeService.deleteFirewallRule(zoneId, firewallRuleId))
      private val output = client.delete(zoneId, firewallRuleId)

      output.compile.toList must returnValue(List(firewallRuleId))
    }

    "return success if the firewall rule id doesn't exist" in new Setup {
      private val client = buildFirewallRuleClient(fakeService.deleteFirewallRuleThatDoesNotExist(zoneId, firewallRuleId, true))
      private val output = client.delete(zoneId, firewallRuleId)

      output.compile.toList must returnValue(List(firewallRuleId))
    }

    "return success if the firewall rule id is invalid" in new Setup {
      private val client = buildFirewallRuleClient(fakeService.deleteFirewallRuleThatDoesNotExist(zoneId, firewallRuleId, false))
      private val output = client.delete(zoneId, firewallRuleId)

      output.compile.toList must returnValue(List(firewallRuleId))
    }
  }

  "buildUri and parseUri" should {
    val nonEmptyAlphaNumericString = Gen.asciiPrintableStr.suchThat(_.nonEmpty)
    implicit val arbitraryZoneId = Arbitrary(nonEmptyAlphaNumericString.map(shapeless.tag[ZoneIdTag][String]))
    implicit val arbitraryFirewallRuleId = Arbitrary(nonEmptyAlphaNumericString.map(shapeless.tag[FirewallRuleIdTag][String]))

    "be the inverse of each other" >> { prop { (zoneId: ZoneId, firewallRuleId: FirewallRuleId) =>
      val client = new FirewallRuleClient[IO] {
        override def list(zoneId: ZoneId): fs2.Stream[IO, FirewallRule] = ???
        override def getById(zoneId: ZoneId, firewallRuleId: String): fs2.Stream[IO, FirewallRule] = ???
        override def create(zoneId: ZoneId, firewallRule: FirewallRule): fs2.Stream[IO, FirewallRule] = ???
        override def update(zoneId: ZoneId, firewallRule: FirewallRule): fs2.Stream[IO, FirewallRule] = ???
        override def delete(zoneId: ZoneId, firewallRuleId: String): fs2.Stream[IO, FirewallRuleId] = ???
      }

      client.parseUri(client.buildUri(zoneId, firewallRuleId)) must beSome((zoneId, firewallRuleId))
    }}
  }
}
