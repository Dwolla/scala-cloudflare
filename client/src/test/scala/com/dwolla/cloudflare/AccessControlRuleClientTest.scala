package com.dwolla.cloudflare

import java.time.Instant

import cats.effect._
import com.dwolla.cloudflare.domain.model._
import com.dwolla.cloudflare.domain.model.accesscontrolrules.{AccessControlRuleId, _}
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
    val ruleId: AccessControlRuleId = tagAccessControlRuleId("630758d3b7274d7a96503972441b677c")

    val authorization = CloudflareAuthorization("email", "key")
    val fakeService = new FakeCloudflareService(authorization)

    protected def buildAccessControlRuleClient(service: HttpRoutes[IO]): AccessControlRuleClient[IO] =
      AccessControlRuleClient(new StreamingCloudflareApiExecutor(fakeService.client(service), authorization))
  }

  "list" should {

    "list the access control rules for the given account" in new Setup {
      private val client = buildAccessControlRuleClient(fakeService.listAccessControlRules(accountId))
      private val output = client.list(accountId)

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
                    scope = Option(AccessControlRuleScope(
                      tagAccessControlRuleScopeId("fake-rule-scope"),
                      Option("Some Account").map(tagAccessControlRuleScopeName),
                      tagAccessControlRuleScopeType("account")
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
                    scope = Option(AccessControlRuleScope(
                      tagAccessControlRuleScopeId("fake-rule-scope"),
                      Option("Some Account").map(tagAccessControlRuleScopeName),
                      tagAccessControlRuleScopeType("account"))
                    )
                  )
      ))
    }

    "list the access control rules filtered by mode for the account" in new Setup {
      private val client = buildAccessControlRuleClient(fakeService.listAccessControlRulesFilteredByWhitelistMode(accountId))
      private val output = client.list(accountId, mode=Option("whitelist"))

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
          scope = Option(AccessControlRuleScope(
            tagAccessControlRuleScopeId("fake-rule-scope"),
            Option("Some Account").map(tagAccessControlRuleScopeName),
            tagAccessControlRuleScopeType("account")
          ))
        )
      ))
    }

    "get by id" should {
      "return the access control rule with the given id" in new Setup {
        private val client = buildAccessControlRuleClient(fakeService.getAccessControlRuleById(accountId, ruleId))
        private val output = client.getById(accountId, ruleId: String)

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
            scope = Option(AccessControlRuleScope(
              tagAccessControlRuleScopeId("fake-rule-scope"),
              Option("Some Account").map(tagAccessControlRuleScopeName),
              tagAccessControlRuleScopeType("account")
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

    "send the json object and return its value" in new Setup {
      private val client = buildAccessControlRuleClient(fakeService.createAccessControlRule(accountId, ruleId))
      private val output = client.create(accountId, input)

      output.compile.toList must returnValue(List(input.copy(
        id = Option(ruleId),
        created_on = Option("1983-09-10T21:33:59.000000Z").map(Instant.parse),
        modified_on = Option("2019-01-24T11:09:11.000000Z").map(Instant.parse),
      )))
    }
  }

  "update" should {
    "update the given rate limit" in new Setup {
      val input = AccessControlRule(
        id = Option(ruleId),
        mode = tagAccessControlRuleMode("challenge"),
        notes = Option("Some notes"),
        configuration = AccessControlRuleConfiguration(
          tagAccessControlRuleConfigurationTarget("ip"),
          tagAccessControlRuleConfigurationValue("1.2.3.4")
        )
      )

      private val client = buildAccessControlRuleClient(fakeService.updateAccessControlRule(accountId, ruleId))
      private val output = client.update(accountId, input)

      output.compile.toList must returnValue(List(input.copy(modified_on = Option("2019-01-24T11:09:11.000000Z").map(Instant.parse))))
    }

    "raise an exception when trying to update an unidentified rate limit" in new Setup {
      val input = AccessControlRule(
        id = None,
        mode = tagAccessControlRuleMode("challenge"),
        notes = Option("Some notes"),
        configuration = AccessControlRuleConfiguration(
          tagAccessControlRuleConfigurationTarget("ip"),
          tagAccessControlRuleConfigurationValue("1.2.3.4")
        )
      )

      private val client = buildAccessControlRuleClient(fakeService.updateAccessControlRule(accountId, ruleId))
      private val output = client.update(accountId, input)

      output.attempt.compile.toList must returnValue(List(
        Left(CannotUpdateUnidentifiedAccessControlRule(input))
      ))
    }
  }

  "delete" should {
    "delete the given access control rule" in new Setup {
      private val client = buildAccessControlRuleClient(fakeService.deleteAccessControlRule(accountId, ruleId))
      private val output = client.delete(accountId, ruleId)

      output.compile.toList must returnValue(List(ruleId))
    }

    "return success if the access control rule id doesn't exist" in new Setup {
      private val client = buildAccessControlRuleClient(fakeService.deleteAccessControlThatDoesNotExist(accountId))
      private val output = client.delete(accountId, ruleId)

      output.compile.toList must returnValue(List(ruleId))
    }

    "buildUri and parseUri" should {
      val nonEmptyAlphaNumericString = Gen.asciiPrintableStr.suchThat(_.nonEmpty)
      implicit val arbitraryAccountId = Arbitrary(nonEmptyAlphaNumericString.map(shapeless.tag[AccountIdTag][String]))
      implicit val arbitraryRuleId = Arbitrary(nonEmptyAlphaNumericString.map(shapeless.tag[AccessControlRuleIdTag][String]))

      "be the inverse of each other" >> { prop { (accountId: AccountId, ruleId: AccessControlRuleId) =>
        val client = new AccessControlRuleClient[IO] {
          override def list(accountId: AccountId, mode: Option[String] = None): fs2.Stream[IO, AccessControlRule] = ???
          override def getById(accountId: AccountId, ruleId: String): fs2.Stream[IO, AccessControlRule] = ???
          override def create(accountId: AccountId, ruleId: AccessControlRule): fs2.Stream[IO, AccessControlRule] = ???
          override def update(accountId: AccountId, ruleId: AccessControlRule): fs2.Stream[IO, AccessControlRule] = ???
          override def delete(accountId: AccountId, ruleId: String): fs2.Stream[IO, AccessControlRuleId] = ???
        }

        client.parseUri(client.buildUri(accountId, ruleId)) must beSome((accountId, ruleId))
      }}
    }
  }
}
