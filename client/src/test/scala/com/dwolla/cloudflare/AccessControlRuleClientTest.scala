package com.dwolla.cloudflare

import java.time.Instant

import cats.effect.*
import com.dwolla.cloudflare.domain.model.*
import com.dwolla.cloudflare.domain.model.accesscontrolrules.*
import dwolla.cloudflare.FakeCloudflareService
import org.http4s.HttpRoutes
import org.scalacheck.{Arbitrary, Gen}
import munit.CatsEffectSuite
import munit.ScalaCheckSuite

class AccessControlRuleClientTest extends CatsEffectSuite with ScalaCheckSuite {
  // Common setup values and helper
  val accountId: AccountId = tagAccountId("account-id")
  val zoneId: ZoneId = tagZoneId("zone-id")
  val ruleId: AccessControlRuleId = tagAccessControlRuleId("630758d3b7274d7a96503972441b677c")

  val authorization = CloudflareAuthorization("email", "key")

  private def buildAccessControlRuleClient(service: HttpRoutes[IO]): AccessControlRuleClient[IO] = {
    val fakeService = new FakeCloudflareService(authorization)
    AccessControlRuleClient(new StreamingCloudflareApiExecutor(fakeService.client(service), authorization))
  }

  test("list should list the access control rules for the given account") {
    val fakeService = new FakeCloudflareService(authorization)
    val client = buildAccessControlRuleClient(fakeService.listAccessControlRulesByAccount(accountId))
    val output = client.list(Level.Account(accountId))

    val expected = List(
      AccessControlRule(
        id = Option("fake-access-rule-1").map(tagAccessControlRuleId),
        notes = Option("Some notes"),
        mode = tagAccessControlRuleMode("challenge"),
        configuration = AccessControlRuleConfiguration(
          tagAccessControlRuleConfigurationTarget("ip"),
          tagAccessControlRuleConfigurationValue("1.2.3.4")
        ),
        allowed_modes = List("whitelist", "block", "challenge", "js_challenge"),
        created_on = Option("2014-01-01T05:20:00.12345Z").map(Instant.parse),
        modified_on = Option("2014-01-01T05:20:00.12345Z").map(Instant.parse),
        scope = Option(AccessControlRuleScope.Account(
          tagAccessControlRuleScopeId("fake-rule-scope"),
          Option("Some Account").map(tagAccessControlRuleScopeName)
        ))
      ),
      AccessControlRule(
        id = Option("fake-access-rule-2").map(tagAccessControlRuleId),
        notes = Option("Some notes"),
        mode = tagAccessControlRuleMode("challenge"),
        configuration = AccessControlRuleConfiguration(
          tagAccessControlRuleConfigurationTarget("ip"),
          tagAccessControlRuleConfigurationValue("2.3.4.5")
        ),
        allowed_modes = List("whitelist", "block", "challenge", "js_challenge"),
        created_on = Option("2014-01-01T05:20:00.12345Z").map(Instant.parse),
        modified_on = Option("2014-01-01T05:20:00.12345Z").map(Instant.parse),
        scope = Option(AccessControlRuleScope.Account(
          tagAccessControlRuleScopeId("fake-rule-scope"),
          Option("Some Account").map(tagAccessControlRuleScopeName)
        ))
      )
    )

    assertIO(output.compile.toList, expected)
  }

  test("list should list the access control rules for the given zone") {
    val fakeService = new FakeCloudflareService(authorization)
    val client = buildAccessControlRuleClient(fakeService.listAccessControlRulesByZone(zoneId))
    val output = client.list(Level.Zone(zoneId))

    val expected = List(
      AccessControlRule(
        id = Option("fake-access-rule-1").map(tagAccessControlRuleId),
        notes = Option("Some notes"),
        mode = tagAccessControlRuleMode("challenge"),
        configuration = AccessControlRuleConfiguration(
          tagAccessControlRuleConfigurationTarget("ip"),
          tagAccessControlRuleConfigurationValue("1.2.3.4")
        ),
        allowed_modes = List("whitelist", "block", "challenge", "js_challenge"),
        created_on = Option("2014-01-01T05:20:00.12345Z").map(Instant.parse),
        modified_on = Option("2014-01-01T05:20:00.12345Z").map(Instant.parse),
        scope = Option(AccessControlRuleScope.Zone(
          tagAccessControlRuleScopeId("fake-rule-scope"),
          Option("Some Zone").map(tagAccessControlRuleScopeName)
        ))
      ),
      AccessControlRule(
        id = Option("fake-access-rule-2").map(tagAccessControlRuleId),
        notes = Option("Some notes"),
        mode = tagAccessControlRuleMode("challenge"),
        configuration = AccessControlRuleConfiguration(
          tagAccessControlRuleConfigurationTarget("ip"),
          tagAccessControlRuleConfigurationValue("2.3.4.5")
        ),
        allowed_modes = List("whitelist", "block", "challenge", "js_challenge"),
        created_on = Option("2014-01-01T05:20:00.12345Z").map(Instant.parse),
        modified_on = Option("2014-01-01T05:20:00.12345Z").map(Instant.parse),
        scope = Option(AccessControlRuleScope.Zone(
          tagAccessControlRuleScopeId("fake-rule-scope"),
          Option("Some Zone").map(tagAccessControlRuleScopeName)
        ))
      )
    )

    assertIO(output.compile.toList, expected)
  }

  test("list should list the access control rules filtered by mode for an account") {
    val fakeService = new FakeCloudflareService(authorization)
    val client = buildAccessControlRuleClient(fakeService.listAccessControlRulesByAccountFilteredByWhitelistMode(accountId))
    val output = client.list(Level.Account(accountId), mode = Option("whitelist"))

    val expected = List(
      AccessControlRule(
        id = Option("fake-access-rule-1").map(tagAccessControlRuleId),
        notes = Option("Some notes"),
        mode = tagAccessControlRuleMode("whitelist"),
        configuration = AccessControlRuleConfiguration(
          tagAccessControlRuleConfigurationTarget("ip"),
          tagAccessControlRuleConfigurationValue("1.2.3.4")
        ),
        allowed_modes = List("whitelist", "block", "challenge", "js_challenge"),
        created_on = Option("2014-01-01T05:20:00.12345Z").map(Instant.parse),
        modified_on = Option("2014-01-01T05:20:00.12345Z").map(Instant.parse),
        scope = Option(AccessControlRuleScope.Account(
          tagAccessControlRuleScopeId("fake-rule-scope"),
          Option("Some Account").map(tagAccessControlRuleScopeName)
        ))
      )
    )

    assertIO(output.compile.toList, expected)
  }

  test("list should list the access control rules filtered by mode for a zone") {
    val fakeService = new FakeCloudflareService(authorization)
    val client = buildAccessControlRuleClient(fakeService.listAccessControlRulesByZoneFilteredByWhitelistMode(zoneId))
    val output = client.list(Level.Zone(zoneId), mode = Option("whitelist"))

    val expected = List(
      AccessControlRule(
        id = Option("fake-access-rule-1").map(tagAccessControlRuleId),
        notes = Option("Some notes"),
        mode = tagAccessControlRuleMode("whitelist"),
        configuration = AccessControlRuleConfiguration(
          tagAccessControlRuleConfigurationTarget("ip"),
          tagAccessControlRuleConfigurationValue("1.2.3.4")
        ),
        allowed_modes = List("whitelist", "block", "challenge", "js_challenge"),
        created_on = Option("2014-01-01T05:20:00.12345Z").map(Instant.parse),
        modified_on = Option("2014-01-01T05:20:00.12345Z").map(Instant.parse),
        scope = Option(AccessControlRuleScope.Zone(
          tagAccessControlRuleScopeId("fake-rule-scope"),
          Option("Some Zone").map(tagAccessControlRuleScopeName)
        ))
      )
    )

    assertIO(output.compile.toList, expected)
  }

  test("get by id should return the access control rule with the given id for an account") {
    val fakeService = new FakeCloudflareService(authorization)
    val client = buildAccessControlRuleClient(fakeService.getAccessControlRuleByIdForAccount(accountId, ruleId))
    val output = client.getById(Level.Account(accountId), ruleId.value)

    val expected = List(
      AccessControlRule(
        id = Option("fake-access-rule-1").map(tagAccessControlRuleId),
        notes = Option("Some notes"),
        mode = tagAccessControlRuleMode("challenge"),
        configuration = AccessControlRuleConfiguration(
          tagAccessControlRuleConfigurationTarget("ip"),
          tagAccessControlRuleConfigurationValue("198.51.100.4")
        ),
        allowed_modes = List("whitelist", "block", "challenge", "js_challenge"),
        created_on = Option("2014-01-01T05:20:00.12345Z").map(Instant.parse),
        modified_on = Option("2014-01-01T05:20:00.12345Z").map(Instant.parse),
        scope = Option(AccessControlRuleScope.Account(
          tagAccessControlRuleScopeId("fake-rule-scope"),
          Option("Some Account").map(tagAccessControlRuleScopeName)
        ))
      )
    )

    assertIO(output.compile.toList, expected)
  }

  test("get by id should return the access control rule with the given id for a zone") {
    val fakeService = new FakeCloudflareService(authorization)
    val client = buildAccessControlRuleClient(fakeService.getAccessControlRuleByIdForZone(zoneId, ruleId))
    val output = client.getById(Level.Zone(zoneId), ruleId.value)

    val expected = List(
      AccessControlRule(
        id = Option("fake-access-rule-1").map(tagAccessControlRuleId),
        notes = Option("Some notes"),
        mode = tagAccessControlRuleMode("challenge"),
        configuration = AccessControlRuleConfiguration(
          tagAccessControlRuleConfigurationTarget("ip"),
          tagAccessControlRuleConfigurationValue("198.51.100.4")
        ),
        allowed_modes = List("whitelist", "block", "challenge", "js_challenge"),
        created_on = Option("2014-01-01T05:20:00.12345Z").map(Instant.parse),
        modified_on = Option("2014-01-01T05:20:00.12345Z").map(Instant.parse),
        scope = Option(AccessControlRuleScope.Account(
          tagAccessControlRuleScopeId("fake-rule-scope"),
          Option("Some Zone").map(tagAccessControlRuleScopeName)
        ))
      )
    )

    assertIO(output.compile.toList, expected)
  }

  private val createInput = AccessControlRule(
    mode = tagAccessControlRuleMode("challenge"),
    notes = Option("Some notes"),
    configuration = AccessControlRuleConfiguration(
      tagAccessControlRuleConfigurationTarget("ip"),
      tagAccessControlRuleConfigurationValue("1.2.3.4")
    )
  )

  test("create should send the json object and return its value for an account") {
    val fakeService = new FakeCloudflareService(authorization)
    val client = buildAccessControlRuleClient(fakeService.createAccessControlRuleForAccount(accountId, ruleId))
    val output = client.create(Level.Account(accountId), createInput)

    val expected = List(createInput.copy(
      id = Option(ruleId),
      created_on = Option("1983-09-10T21:33:59.000000Z").map(Instant.parse),
      modified_on = Option("2019-01-24T11:09:11.000000Z").map(Instant.parse),
      scope = Option(AccessControlRuleScope.Account(
        id = tagAccessControlRuleScopeId("fake-rule-scope"),
        name = Option("Some Account").map(tagAccessControlRuleScopeName)
      ))
    ))

    assertIO(output.compile.toList, expected)
  }

  test("create should send the json object and return its value for a zone") {
    val fakeService = new FakeCloudflareService(authorization)
    val client = buildAccessControlRuleClient(fakeService.createAccessControlRuleForZone(zoneId, ruleId))
    val output = client.create(Level.Zone(zoneId), createInput)

    val expected = List(createInput.copy(
      id = Option(ruleId),
      created_on = Option("1983-09-10T21:33:59.000000Z").map(Instant.parse),
      modified_on = Option("2019-01-24T11:09:11.000000Z").map(Instant.parse),
      scope = Option(AccessControlRuleScope.Zone(
        id = tagAccessControlRuleScopeId("fake-rule-scope"),
        name = Option("Some Zone").map(tagAccessControlRuleScopeName)
      ))
    ))

    assertIO(output.compile.toList, expected)
  }

  private val unidentifiedInput = AccessControlRule(
    mode = tagAccessControlRuleMode("challenge"),
    notes = Option("Some notes"),
    configuration = AccessControlRuleConfiguration(
      tagAccessControlRuleConfigurationTarget("ip"),
      tagAccessControlRuleConfigurationValue("1.2.3.4")
    )
  )

  test("update should update the given access control rule for an account") {
    val input = unidentifiedInput.copy(id = Option(ruleId))

    val fakeService = new FakeCloudflareService(authorization)
    val client = buildAccessControlRuleClient(fakeService.updateAccessControlRule(Level.Account(accountId), ruleId))
    val output = client.update(Level.Account(accountId), input)

    val expected = List(input.copy(
      modified_on = Option("2019-01-24T11:09:11.000000Z").map(Instant.parse),
      scope = Option(AccessControlRuleScope.Account(
        id = tagAccessControlRuleScopeId("fake-rule-scope"),
        name = Option("Some Account").map(tagAccessControlRuleScopeName)
      ))
    ))

    assertIO(output.compile.toList, expected)
  }

  test("update should update the given access control rule for a zone") {
    val input = unidentifiedInput.copy(id = Option(ruleId))

    val fakeService = new FakeCloudflareService(authorization)
    val client = buildAccessControlRuleClient(fakeService.updateAccessControlRule(Level.Zone(zoneId), ruleId))
    val output = client.update(Level.Zone(zoneId), input)

    val expected = List(input.copy(
      modified_on = Option("2019-01-24T11:09:11.000000Z").map(Instant.parse),
      scope = Option(AccessControlRuleScope.Zone(
        id = tagAccessControlRuleScopeId("fake-rule-scope"),
        name = Option("Some Zone").map(tagAccessControlRuleScopeName)
      ))
    ))

    assertIO(output.compile.toList, expected)
  }

  test("update should raise an exception when trying to update an unidentified rule for an account") {
    val fakeService = new FakeCloudflareService(authorization)
    val client = buildAccessControlRuleClient(fakeService.updateAccessControlRule(Level.Account(accountId), ruleId))
    val output = client.update(Level.Account(accountId), unidentifiedInput)

    val expected = List(Left(CannotUpdateUnidentifiedAccessControlRule(unidentifiedInput)))

    assertIO(output.attempt.compile.toList, expected)
  }

  test("update should raise an exception when trying to update an unidentified rule for a zone") {
    val fakeService = new FakeCloudflareService(authorization)
    val client = buildAccessControlRuleClient(fakeService.updateAccessControlRule(Level.Zone(zoneId), ruleId))
    val output = client.update(Level.Zone(zoneId), unidentifiedInput)

    val expected = List(Left(CannotUpdateUnidentifiedAccessControlRule(unidentifiedInput)))

    assertIO(output.attempt.compile.toList, expected)
  }

  test("delete should delete the given access control rule for an account") {
    val fakeService = new FakeCloudflareService(authorization)
    val client = buildAccessControlRuleClient(fakeService.deleteAccessControlRule(Level.Account(accountId), ruleId))
    val output = client.delete(Level.Account(accountId), ruleId.value)

    val expected = List(ruleId)
    assertIO(output.compile.toList, expected)
  }

  test("delete should delete the given access control rule for a zone") {
    val fakeService = new FakeCloudflareService(authorization)
    val client = buildAccessControlRuleClient(fakeService.deleteAccessControlRule(Level.Zone(zoneId), ruleId))
    val output = client.delete(Level.Zone(zoneId), ruleId.value)

    val expected = List(ruleId)
    assertIO(output.compile.toList, expected)
  }

  test("delete should return success if the access control rule id doesn't exist for an account") {
    val fakeService = new FakeCloudflareService(authorization)
    val client = buildAccessControlRuleClient(fakeService.deleteAccessControlThatDoesNotExist(Level.Account(accountId)))
    val output = client.delete(Level.Account(accountId), ruleId.value)

    val expected = List(ruleId)
    assertIO(output.compile.toList, expected)
  }

  test("delete should return success if the access control rule id doesn't exist for a zone") {
    val fakeService = new FakeCloudflareService(authorization)
    val client = buildAccessControlRuleClient(fakeService.deleteAccessControlThatDoesNotExist(Level.Zone(zoneId)))
    val output = client.delete(Level.Zone(zoneId), ruleId.value)

    val expected = List(ruleId)
    assertIO(output.compile.toList, expected)
  }

  // property-based: buildUri and parseUri are inverses
  private val nonEmptyAlphaNumericString = Gen.identifier.suchThat(_.nonEmpty)
  private val genAccountLevel = nonEmptyAlphaNumericString.map(AccountId(_)).map(Level.Account(_))
  private val genZoneLevel = nonEmptyAlphaNumericString.map(ZoneId(_)).map(Level.Zone(_))
  implicit private val arbitraryLevel: Arbitrary[Level] = Arbitrary(Gen.oneOf(genAccountLevel, genZoneLevel))
  implicit private val arbitraryRuleId: Arbitrary[AccessControlRuleId] = Arbitrary(nonEmptyAlphaNumericString.map(AccessControlRuleId(_)))

  property("buildUri and parseUri should be inverses") {
    import org.scalacheck.Prop.forAll

    forAll { (level: Level, ruleId: AccessControlRuleId) =>
      val client = new AccessControlRuleClient[IO] {
        override def list(level: Level, mode: Option[String] = None): fs2.Stream[IO, AccessControlRule] = ???
        override def getById(level: Level, ruleId: String): fs2.Stream[IO, AccessControlRule] = ???
        override def create(level: Level, ruleId: AccessControlRule): fs2.Stream[IO, AccessControlRule] = ???
        override def update(level: Level, ruleId: AccessControlRule): fs2.Stream[IO, AccessControlRule] = ???
        override def delete(level: Level, ruleId: String): fs2.Stream[IO, AccessControlRuleId] = ???
      }

      assertEquals(client.parseUri(client.buildUri(level, ruleId).renderString), Some((level, ruleId)))
    }
  }
}
