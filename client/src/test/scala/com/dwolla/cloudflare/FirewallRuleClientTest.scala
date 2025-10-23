package com.dwolla.cloudflare

import java.time.Instant

import cats.effect.*
import com.dwolla.cloudflare.domain.model.Exceptions.UnexpectedCloudflareErrorException
import com.dwolla.cloudflare.domain.model.*
import com.dwolla.cloudflare.domain.model.firewallrules.*
import com.dwolla.cloudflare.domain.model.filters.*
import dwolla.cloudflare.FakeCloudflareService
import org.http4s.HttpRoutes
import org.scalacheck.{Arbitrary, Gen}
import munit.CatsEffectSuite
import munit.ScalaCheckSuite
import natchez.Trace.Implicits.noop

class FirewallRuleClientTest extends CatsEffectSuite with ScalaCheckSuite {

  // Common setup values and helper
  val zoneId: ZoneId = tagZoneId("zone-id")
  val firewallRuleId: FirewallRuleId = tagFirewallRuleId("ccbfa4a0b26b4ffa8a006e8b11557397")
  val filterId: FilterId = tagFilterId("d5266c8daa9443e081e5207f64763836")

  val authorization = CloudflareAuthorization("email", "key")

  private def buildFirewallRuleClient(service: HttpRoutes[IO]): FirewallRuleClient[fs2.Stream[IO, *]] = {
    val fakeService = new FakeCloudflareService(authorization)
    FirewallRuleClient[IO](new StreamingCloudflareApiExecutor(fakeService.client(service), authorization))
  }

  test("list should list the firewall rules for the given zone") {
    val fakeService = new FakeCloudflareService(authorization)
    val client = buildFirewallRuleClient(fakeService.listFirewallRules(zoneId))
    val output = client.list(zoneId)

    val expected = List(
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
    )

    assertIO(output.compile.toList, expected)
  }

  test("get by id should return the firewall rule with the given id") {
    val fakeService = new FakeCloudflareService(authorization)
    val client = buildFirewallRuleClient(fakeService.getFirewallRuleById(zoneId, firewallRuleId))
    val output = client.getById(zoneId, firewallRuleId.value)

    val expected = List(FirewallRule(
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
    ))

    assertIO(output.compile.toList, expected)
  }

  private val createInput = FirewallRule(
    filter = FirewallRuleFilter(
      expression = Option(tagFilterExpression("(cf.bot_management.verified_bot)")),
      paused = Option(false)
    ),
    action = Action.Log,
    priority = tagFirewallRulePriority(1),
    paused = false
  )

  test("create should send the json object and return its value") {
    val fakeService = new FakeCloudflareService(authorization)
    val client = buildFirewallRuleClient(fakeService.createFirewallRule(zoneId, firewallRuleId))
    val output = client.create(zoneId, createInput)

    val expected = List(createInput.copy(
      id = Option(firewallRuleId),
      created_on = Option("2019-12-14T01:38:21Z").map(Instant.parse),
      modified_on = Option("2019-12-14T01:38:21Z").map(Instant.parse),
    ))

    assertIO(output.compile.toList, expected)
  }

  test("raise a reasonable error if Cloudflare's business rules are violated") {
    val fakeService = new FakeCloudflareService(authorization)
    val client = buildFirewallRuleClient(fakeService.createFirewallRuleFails)
    val output = client.create(zoneId, createInput)

    val expected = List(
      Left(
        UnexpectedCloudflareErrorException(
          List(Error(None, "products is only valid for the 'bypass' action"))
        )
      )
    )

    assertIO(output.attempt.compile.toList, expected)
  }

  test("update should update the given firewall rule") {
    val input = FirewallRule(
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

    val fakeService = new FakeCloudflareService(authorization)
    val client = buildFirewallRuleClient(fakeService.updateFirewallRule(zoneId, firewallRuleId))
    val output = client.update(zoneId, input)

    val expected = List(input.copy(modified_on = Option("2019-12-14T01:39:58Z").map(Instant.parse)))

    assertIO(output.compile.toList, expected)
  }

  test("update should raise an exception when trying to update an unidentified firewall rule") {
    val input = FirewallRule(
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

    val fakeService = new FakeCloudflareService(authorization)
    val client = buildFirewallRuleClient(fakeService.updateFirewallRule(zoneId, firewallRuleId))
    val output = client.update(zoneId, input)

    val expected = List(Left(CannotUpdateUnidentifiedFirewallRule(input)))

    assertIO(output.attempt.compile.toList, expected)
  }

  test("delete should delete the given firewall rule") {
    val fakeService = new FakeCloudflareService(authorization)
    val client = buildFirewallRuleClient(fakeService.deleteFirewallRule(zoneId, firewallRuleId))
    val output = client.delete(zoneId, firewallRuleId.value)

    val expected = List(firewallRuleId)
    assertIO(output.compile.toList, expected)
  }

  test("delete should return success if the firewall rule id doesn't exist") {
    val fakeService = new FakeCloudflareService(authorization)
    val client = buildFirewallRuleClient(fakeService.deleteFirewallRuleThatDoesNotExist(zoneId, firewallRuleId, true))
    val output = client.delete(zoneId, firewallRuleId.value)

    val expected = List(firewallRuleId)
    assertIO(output.compile.toList, expected)
  }

  test("delete should return success if the firewall rule id is invalid") {
    val fakeService = new FakeCloudflareService(authorization)
    val client = buildFirewallRuleClient(fakeService.deleteFirewallRuleThatDoesNotExist(zoneId, firewallRuleId, false))
    val output = client.delete(zoneId, firewallRuleId.value)

    val expected = List(firewallRuleId)
    assertIO(output.compile.toList, expected)
  }

  // property-based: buildUri and parseUri are inverses
  private val nonEmptyAlphaNumericString = Gen.identifier.suchThat(_.nonEmpty)
  implicit private val arbitraryZoneId: Arbitrary[ZoneId] = Arbitrary(nonEmptyAlphaNumericString.map(ZoneId(_)))
  implicit private val arbitraryFirewallRuleId: Arbitrary[FirewallRuleId] = Arbitrary(nonEmptyAlphaNumericString.map(FirewallRuleId(_)))

  property("buildUri and parseUri should be inverses") {
    import org.scalacheck.Prop.forAll

    forAll { (zoneId: ZoneId, firewallRuleId: FirewallRuleId) =>
      val client = new FirewallRuleClient[IO] {
        override def list(zoneId: ZoneId): IO[FirewallRule] = ???
        override def getById(zoneId: ZoneId, firewallRuleId: String): IO[FirewallRule] = ???
        override def create(zoneId: ZoneId, firewallRule: FirewallRule): IO[FirewallRule] = ???
        override def update(zoneId: ZoneId, firewallRule: FirewallRule): IO[FirewallRule] = ???
        override def delete(zoneId: ZoneId, firewallRuleId: String): IO[FirewallRuleId] = ???
        override def getByUri(uri: String): IO[FirewallRule] = ???
      }

      assertEquals(client.parseUri(client.buildUri(zoneId, firewallRuleId).renderString), Some((zoneId, firewallRuleId)))
    }
  }
}
