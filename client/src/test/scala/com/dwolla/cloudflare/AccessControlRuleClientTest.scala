package com.dwolla.cloudflare

import java.time.Instant

import cats.effect._
import com.dwolla.cloudflare.domain.model._
import com.dwolla.cloudflare.domain.model.accesscontrolrules._
import dwolla.cloudflare.FakeCloudflareService
import org.http4s.HttpRoutes
import org.scalacheck.{Arbitrary, Gen}
import org.specs2.ScalaCheck
import org.specs2.matcher.{IOMatchers, Matchers}
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

class AccessControlRuleClientTest extends Specification with ScalaCheck with IOMatchers with Matchers {
  trait Setup extends Scope {
    val accountId: AccountId = tagAccountId("account-id")
    val zoneId: ZoneId = tagZoneId("zone-id")
    val ruleId: AccessControlRuleId = tagAccessControlRuleId("630758d3b7274d7a96503972441b677c")

    val authorization = CloudflareAuthorization("email", "key")
    val fakeService = new FakeCloudflareService(authorization)

    protected def buildAccessControlRuleClient(service: HttpRoutes[IO]): AccessControlRuleClient[IO] =
      AccessControlRuleClient(new StreamingCloudflareApiExecutor(fakeService.client(service), authorization))
  }

  "list" should {

    "list the access control rules for the given account" in new Setup {
      private val client = buildAccessControlRuleClient(fakeService.listAccessControlRulesByAccount(accountId))
      private val output = client.list(Level.Account(accountId))

      output.compile.toList must returnValue(List(
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
                      Option("Some Account").map(tagAccessControlRuleScopeName))
                    )
                  )
      ))
    }

    "list the access control rules for the given zone" in new Setup {
      private val client = buildAccessControlRuleClient(fakeService.listAccessControlRulesByZone(zoneId))
      private val output = client.list(Level.Zone(zoneId))

      output.compile.toList must returnValue(List(
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
            Option("Some Zone").map(tagAccessControlRuleScopeName))
          )
        )
      ))
    }

    "list the access control rules filtered by mode for an account" in new Setup {
      private val client = buildAccessControlRuleClient(fakeService.listAccessControlRulesByAccountFilteredByWhitelistMode(accountId))
      private val output = client.list(Level.Account(accountId), mode=Option("whitelist"))

      output.compile.toList must returnValue(List(
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
      ))
    }

    "list the access control rules filtered by mode for a zone" in new Setup {
      private val client = buildAccessControlRuleClient(fakeService.listAccessControlRulesByZoneFilteredByWhitelistMode(zoneId))
      private val output = client.list(Level.Zone(zoneId), mode=Option("whitelist"))

      output.compile.toList must returnValue(List(
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
      ))
    }

    "get by id" should {
      "return the access control rule with the given id for an account" in new Setup {
        private val client = buildAccessControlRuleClient(fakeService.getAccessControlRuleByIdForAccount(accountId, ruleId))
        private val output = client.getById(Level.Account(accountId), ruleId: String)

        output.compile.toList must returnValue(List(
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
        ))
      }

      "return the access control rule with the given id for a zone" in new Setup {
        private val client = buildAccessControlRuleClient(fakeService.getAccessControlRuleByIdForZone(zoneId, ruleId))
        private val output = client.getById(Level.Zone(zoneId), ruleId: String)

        output.compile.toList must returnValue(List(
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
        ))
      }
    }
  }

  "create" should {
    val input = AccessControlRule(
      mode = tagAccessControlRuleMode("challenge"),
      notes = Option("Some notes"),
      configuration = AccessControlRuleConfiguration(
        tagAccessControlRuleConfigurationTarget("ip"),
        tagAccessControlRuleConfigurationValue("1.2.3.4")
      )
    )

    "send the json object and return its value for an account" in new Setup {
      private val client = buildAccessControlRuleClient(fakeService.createAccessControlRuleForAccount(accountId, ruleId))
      private val output = client.create(Level.Account(accountId), input)

      output.compile.toList must returnValue(List(input.copy(
        id = Option(ruleId),
        created_on = Option("1983-09-10T21:33:59.000000Z").map(Instant.parse),
        modified_on = Option("2019-01-24T11:09:11.000000Z").map(Instant.parse),
        scope = Option(AccessControlRuleScope.Account(
          id = tagAccessControlRuleScopeId("fake-rule-scope"),
          name = Option("Some Account").map(tagAccessControlRuleScopeName)
        ))
      )))
    }

    "send the json object and return its value for a zone" in new Setup {
      private val client = buildAccessControlRuleClient(fakeService.createAccessControlRuleForZone(zoneId, ruleId))
      private val output = client.create(Level.Zone(zoneId), input)

      output.compile.toList must returnValue(List(input.copy(
        id = Option(ruleId),
        created_on = Option("1983-09-10T21:33:59.000000Z").map(Instant.parse),
        modified_on = Option("2019-01-24T11:09:11.000000Z").map(Instant.parse),
        scope = Option(AccessControlRuleScope.Zone(
          id = tagAccessControlRuleScopeId("fake-rule-scope"),
          name = Option("Some Zone").map(tagAccessControlRuleScopeName)
        ))
      )))
    }
  }

  "update" should {
    val unidentifiedInput = AccessControlRule(
      mode = tagAccessControlRuleMode("challenge"),
      notes = Option("Some notes"),
      configuration = AccessControlRuleConfiguration(
        tagAccessControlRuleConfigurationTarget("ip"),
        tagAccessControlRuleConfigurationValue("1.2.3.4")
      )
    )

    "update the given access control rule for an account" in new Setup {
      val input = unidentifiedInput.copy(id = Option(ruleId))

      private val client = buildAccessControlRuleClient(fakeService.updateAccessControlRule(Level.Account(accountId), ruleId))
      private val output = client.update(Level.Account(accountId), input)

      output.compile.toList must returnValue(List(input.copy(
        modified_on = Option("2019-01-24T11:09:11.000000Z").map(Instant.parse),
        scope = Option(AccessControlRuleScope.Account(
          id = tagAccessControlRuleScopeId("fake-rule-scope"),
          name = Option("Some Account").map(tagAccessControlRuleScopeName)
        ))
      )))
    }

    "update the given access control rule for a zone" in new Setup {
      val input = unidentifiedInput.copy(id = Option(ruleId))

      private val client = buildAccessControlRuleClient(fakeService.updateAccessControlRule(Level.Zone(zoneId), ruleId))
      private val output = client.update(Level.Zone(zoneId), input)

      output.compile.toList must returnValue(List(input.copy(
        modified_on = Option("2019-01-24T11:09:11.000000Z").map(Instant.parse),
        scope = Option(AccessControlRuleScope.Zone(
          id = tagAccessControlRuleScopeId("fake-rule-scope"),
          name = Option("Some Zone").map(tagAccessControlRuleScopeName)
        ))
      )))
    }

    "raise an exception when trying to update an unidentified rule for an account" in new Setup {
      private val client = buildAccessControlRuleClient(fakeService.updateAccessControlRule(Level.Account(accountId), ruleId))
      private val output = client.update(Level.Account(accountId), unidentifiedInput)

      output.attempt.compile.toList must returnValue(List(
        Left(CannotUpdateUnidentifiedAccessControlRule(unidentifiedInput))
      ))
    }

    "raise an exception when trying to update an unidentified rule for a zone" in new Setup {
      private val client = buildAccessControlRuleClient(fakeService.updateAccessControlRule(Level.Zone(zoneId), ruleId))
      private val output = client.update(Level.Zone(zoneId), unidentifiedInput)

      output.attempt.compile.toList must returnValue(List(
        Left(CannotUpdateUnidentifiedAccessControlRule(unidentifiedInput))
      ))
    }
  }

  "delete" should {
    "delete the given access control rule for an account" in new Setup {
      private val client = buildAccessControlRuleClient(fakeService.deleteAccessControlRule(Level.Account(accountId), ruleId))
      private val output = client.delete(Level.Account(accountId), ruleId)

      output.compile.toList must returnValue(List(ruleId))
    }

    "delete the given access control rule for a zone" in new Setup {
      private val client = buildAccessControlRuleClient(fakeService.deleteAccessControlRule(Level.Zone(zoneId), ruleId))
      private val output = client.delete(Level.Zone(zoneId), ruleId)

      output.compile.toList must returnValue(List(ruleId))
    }

    "return success if the access control rule id doesn't exist for an account" in new Setup {
      private val client = buildAccessControlRuleClient(fakeService.deleteAccessControlThatDoesNotExist(Level.Account(accountId)))
      private val output = client.delete(Level.Account(accountId), ruleId)

      output.compile.toList must returnValue(List(ruleId))
    }

    "return success if the access control rule id doesn't exist for a zone" in new Setup {
      private val client = buildAccessControlRuleClient(fakeService.deleteAccessControlThatDoesNotExist(Level.Zone(zoneId)))
      private val output = client.delete(Level.Zone(zoneId), ruleId)

      output.compile.toList must returnValue(List(ruleId))
    }

    "buildUri and parseUri" should {
      val nonEmptyAlphaNumericString = Gen.identifier.suchThat(_.nonEmpty)
      val genAccountLevel = nonEmptyAlphaNumericString.map(shapeless.tag[AccountIdTag][String]).map(Level.Account(_))
      val genZoneLevel = nonEmptyAlphaNumericString.map(shapeless.tag[ZoneIdTag][String]).map(Level.Zone(_))
      implicit val arbitraryLevel: Arbitrary[Level] = Arbitrary(Gen.oneOf(genAccountLevel, genZoneLevel))
      implicit val arbitraryRuleId = Arbitrary(nonEmptyAlphaNumericString.map(shapeless.tag[AccessControlRuleIdTag][String]))

      "be the inverse of each other" >> { prop { (level: Level, ruleId: AccessControlRuleId) =>
        val client = new AccessControlRuleClient[IO] {
          override def list(level: Level, mode: Option[String] = None): fs2.Stream[IO, AccessControlRule] = ???
          override def getById(level: Level, ruleId: String): fs2.Stream[IO, AccessControlRule] = ???
          override def create(level: Level, ruleId: AccessControlRule): fs2.Stream[IO, AccessControlRule] = ???
          override def update(level: Level, ruleId: AccessControlRule): fs2.Stream[IO, AccessControlRule] = ???
          override def delete(level: Level, ruleId: String): fs2.Stream[IO, AccessControlRuleId] = ???
        }

        client.parseUri(client.buildUri(level, ruleId).renderString) must beSome((level, ruleId))
      }}
    }
  }
}
