package dwolla.cloudflare

import cats.effect._
import com.dwolla.cloudflare._
import com.dwolla.cloudflare.domain.model.Exceptions.UnexpectedCloudflareErrorException
import com.dwolla.cloudflare.domain.model._
import com.dwolla.cloudflare.domain.model.accesscontrolrules._
import io.circe.Json
import org.http4s.Status
import org.http4s.client.Client
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import io.circe.literal._
import org.specs2.concurrent.ExecutionEnv

class AccessControlRuleClientSpec(implicit ee: ExecutionEnv) extends Specification {

  trait Setup extends Scope {
    val accountId: AccountId = shapeless.tag[AccountIdTag][String]("account-id")
    val accessRuleId1: AccessControlRuleId = shapeless.tag[AccessControlRuleIdTag][String]("fake-access-rule-1")
    val accessRuleId2: AccessControlRuleId = shapeless.tag[AccessControlRuleIdTag][String]("fake-access-rule-2")
    val accessRuleId3: AccessControlRuleId = shapeless.tag[AccessControlRuleIdTag][String]("fake-access-rule-3")

    val authorization = CloudflareAuthorization("email", "key")
    val fakeService = new FakeCloudflareService(authorization)
  }

  "list" should {
    "list challenge rules" in new Setup {
      val http4sClient = fakeService.client(fakeService.listChallengeAccessRules(Map(1 → SampleResponses.Successes.listChallengeAccessRulesPage1, 2 → SampleResponses.Successes.listChallengeAccessRulesPage2, 3 → SampleResponses.Successes.listChallengeAccessRulesPage3), accountId))
      val client = buildAccessControlRuleClient(http4sClient, authorization)

      val output: List[Rule] = client
        .list(accountId, Option("challenge"))
        .compile
        .toList
        .unsafeRunSync()
      output must contain(
        Rule(
          id = accessRuleId1,
          notes = Some("Some notes"),
          allowedModes = List("whitelist", "block", "challenge", "js_challenge"),
          mode = Some("challenge"),
          configuration = RuleConfiguration("ip", "1.2.3.4"),
          createdOn = Some("2014-01-01T05:20:00.12345Z"),
          modifiedOn = Some("2014-01-01T05:20:00.12345Z"),
          scope = RuleScope("fake-rule-scope", Option("Some Account"), "account")
        )
      )
      output must contain(
        Rule(
          id = accessRuleId2,
          notes = Some("Some notes"),
          allowedModes = List("whitelist", "block", "challenge", "js_challenge"),
          mode = Some("challenge"),
          configuration = RuleConfiguration("ip", "2.3.4.5"),
          createdOn = Some("2014-01-01T05:20:00.12345Z"),
          modifiedOn = Some("2014-01-01T05:20:00.12345Z"),
          scope = RuleScope("fake-rule-scope", Option("Some Account"), "account")
        )
      )
      output must contain(
        Rule(
          id = accessRuleId3,
          notes = Some("Some notes"),
          allowedModes = List("whitelist", "block", "challenge", "js_challenge"),
          mode = Some("challenge"),
          configuration = RuleConfiguration("ip", "3.4.5.6"),
          createdOn = Some("2014-01-01T05:20:00.12345Z"),
          modifiedOn = Some("2014-01-01T05:20:00.12345Z"),
          scope = RuleScope("fake-rule-scope", None, "account")
        )
      )
    }
    "list whitelist rules" in new Setup {
      val http4sClient = fakeService.client(fakeService.listWhitelistAccessRules(Map(1 → SampleResponses.Successes.listWhitelistAccessRules), accountId))
      val client = buildAccessControlRuleClient(http4sClient, authorization)

      val output: List[Rule] = client
        .list(accountId, Option("whitelist"))
        .compile
        .toList
        .unsafeRunSync()
      output must contain(
        Rule(
          id = accessRuleId1,
          notes = Some("Some notes"),
          allowedModes = List("whitelist", "block", "challenge", "js_challenge"),
          mode = Some("whitelist"),
          configuration = RuleConfiguration("ip", "1.2.3.4"),
          createdOn = Some("2014-01-01T05:20:00.12345Z"),
          modifiedOn = Some("2014-01-01T05:20:00.12345Z"),
          scope = RuleScope("fake-rule-scope", Option("Some Account"), "account")
        )
      )
    }

    "list access rules across pages doesn't fetch eagerly" in new Setup {
      val http4sClient = fakeService.client(fakeService.listChallengeAccessRules(Map(1 → SampleResponses.Successes.listChallengeAccessRulesPage1), accountId))
      val client = buildAccessControlRuleClient(http4sClient, authorization)

      val output: List[Rule] = client.list(accountId, Option("challenge")).take(1).compile.toList.unsafeRunSync()
      output must contain(
        Rule(
          id = accessRuleId1,
          notes = Some("Some notes"),
          allowedModes = List("whitelist", "block", "challenge", "js_challenge"),
          mode = Some("challenge"),
          configuration = RuleConfiguration("ip", "1.2.3.4"),
          createdOn = Some("2014-01-01T05:20:00.12345Z"),
          modifiedOn = Some("2014-01-01T05:20:00.12345Z"),
          scope = RuleScope("fake-rule-scope", Option("Some Account"), "account")
        )
      )
    }
  }

  "create" should {
    "create new access rule" in new Setup {
      val rateLimitId = accessRuleId1

      val createRateLimit = CreateRule(Option("challenge"), RuleConfiguration("ip", "1.2.3.4"), Option("Some notes"))

      val http4sClient = fakeService.client(fakeService.createAccessRule(SampleResponses.Successes.accessRule, accountId))
      val client = buildAccessControlRuleClient(http4sClient, authorization)

      val output = client.create(accountId, createRateLimit)
        .compile.toList.unsafeRunSync()

      output must contain(
        Rule(
          id = accessRuleId1,
          notes = Some("Some notes"),
          allowedModes = List("whitelist", "block", "challenge", "js_challenge"),
          mode = Some("challenge"),
          configuration = RuleConfiguration("ip", "198.51.100.4"),
          createdOn = Some("2014-01-01T05:20:00.12345Z"),
          modifiedOn = Some("2014-01-01T05:20:00.12345Z"),
          scope = RuleScope("fake-rule-scope", Option("Some Account"), "account")
        )
      )
    }

    "throw unexpected exception if error creating access rule" in new Setup {
      val createAccessRule = CreateRule(Option(""), RuleConfiguration("ip", "198.51.100.4"), Option("Some notes"))

      val failure = SampleResponses.Failures.accessRuleCreationErrorMode
      val http4sClient = fakeService.client(fakeService.createAccessRule(failure.json, accountId, failure.status))
      val client = buildAccessControlRuleClient(http4sClient, authorization)

      val output = client.create(accountId, createAccessRule)
        .compile
        .toList
        .attempt
        .unsafeRunSync()

      output must beLeft[Throwable].like {
        case ex: UnexpectedCloudflareErrorException ⇒ ex.getMessage must_==
          """An unexpected Cloudflare error occurred. Errors:
            |
            | - Error(None,firewallaccessrules.api.validation_error:unsupported target received: ,invalid mode received: )
            |     """.stripMargin
      }
    }
  }

  "deleteAccessRule" should {
    "delete access rule from account" in new Setup {
      val http4sClient = fakeService.client(fakeService.deleteAccessRule(SampleResponses.Successes.removedAccessRule, accountId, accessRuleId1))
      val client = buildAccessControlRuleClient(http4sClient, authorization)

      private val output = client.delete(accountId, accessRuleId1)

      output.compile.last.unsafeToFuture() must beSome(accessRuleId1).await
    }
  }

  "parse uri" should {
    val nullClient = new AccessControlRuleClient[IO] {
      override def list(accountId: AccountId, mode: Option[String]) = ???
      override def create(accountId: AccountId, rule: CreateRule) = ???
      override def delete(accountId: AccountId, ruleId: String) = ???
    }

    "parse a valid uri into its constituent parts" in new Setup {
      nullClient.parseUri("https://api.cloudflare.com/client/v4/accounts/account-id/firewall/access_rules/fake-access-rule-1") must beSome((accountId, accessRuleId1))
    }

    "return None for invalid URIs" in new Setup {
      nullClient.parseUri("https://hydragents.xyz") must beNone
    }
  }

  def buildAccessControlRuleClient[F[_] : Sync](http4sClient: Client[F], authorization: CloudflareAuthorization): AccessControlRuleClient[F] = {
    val fakeHttp4sExecutor = new StreamingCloudflareApiExecutor(http4sClient, authorization)
    AccessControlRuleClient(fakeHttp4sExecutor)
  }

  private object SampleResponses {

    object Successes {

      val listChallengeAccessRulesPage1 =
        json"""{
             "result": [
               {
                 "id": "fake-access-rule-1",
                 "notes": "Some notes",
                 "allowed_modes": [
                   "whitelist",
                   "block",
                   "challenge",
                   "js_challenge"
                 ],
                 "mode": "challenge",
                 "configuration": {
                   "target": "ip",
                   "value": "1.2.3.4"
                 },
                 "created_on": "2014-01-01T05:20:00.12345Z",
                 "modified_on": "2014-01-01T05:20:00.12345Z",
                 "scope": {
                   "id": "fake-rule-scope",
                   "name": "Some Account",
                   "type": "account"
                 }
               }
             ],
             "result_info": {
               "page": 1,
               "per_page": 1,
               "total_pages": 3,
               "count": 1,
               "total_count": 3
             },
             "success": true,
             "errors": [],
             "messages": []
           }"""

      val listWhitelistAccessRules =
        json"""{
             "result": [
               {
                 "id": "fake-access-rule-1",
                 "notes": "Some notes",
                 "allowed_modes": [
                   "whitelist",
                   "block",
                   "challenge",
                   "js_challenge"
                 ],
                 "mode": "whitelist",
                 "configuration": {
                   "target": "ip",
                   "value": "1.2.3.4"
                 },
                 "created_on": "2014-01-01T05:20:00.12345Z",
                 "modified_on": "2014-01-01T05:20:00.12345Z",
                 "scope": {
                   "id": "fake-rule-scope",
                   "name": "Some Account",
                   "type": "account"
                 }
               }
             ],
             "result_info": {
               "page": 1,
               "per_page": 1,
               "total_pages": 1,
               "count": 1,
               "total_count": 1
             },
             "success": true,
             "errors": [],
             "messages": []
           }"""

      val listChallengeAccessRulesPage2 =
        json"""{
             "result": [
               {
                 "id": "fake-access-rule-2",
                 "notes": "Some notes",
                 "allowed_modes": [
                   "whitelist",
                   "block",
                   "challenge",
                   "js_challenge"
                 ],
                 "mode": "challenge",
                 "configuration": {
                   "target": "ip",
                   "value": "2.3.4.5"
                 },
                 "created_on": "2014-01-01T05:20:00.12345Z",
                 "modified_on": "2014-01-01T05:20:00.12345Z",
                 "scope": {
                   "id": "fake-rule-scope",
                   "name": "Some Account",
                   "type": "account"
                 }
               }
             ],
             "result_info": {
               "page": 2,
               "per_page": 1,
               "total_pages": 3,
               "count": 1,
               "total_count": 3
             },
             "success": true,
             "errors": [],
             "messages": []
           }"""

      val listChallengeAccessRulesPage3 =
        json"""{
             "result": [
               {
                 "id": "fake-access-rule-3",
                 "notes": "Some notes",
                 "allowed_modes": [
                   "whitelist",
                   "block",
                   "challenge",
                   "js_challenge"
                 ],
                 "mode": "challenge",
                 "configuration": {
                   "target": "ip",
                   "value": "3.4.5.6"
                 },
                 "created_on": "2014-01-01T05:20:00.12345Z",
                 "modified_on": "2014-01-01T05:20:00.12345Z",
                 "scope": {
                   "id": "fake-rule-scope",
                   "type": "account"
                 }
               }
             ],
             "result_info": {
               "page": 3,
               "per_page": 1,
               "total_pages": 3,
               "count": 1,
               "total_count": 3
             },
             "success": true,
             "errors": [],
             "messages": []
           }"""

      val accessRule =
        json"""{
            "success": true,
            "errors": [],
            "messages": [],
            "result": {
              "id": "fake-access-rule-1",
              "notes": "Some notes",
              "allowed_modes": [
                "whitelist",
                "block",
                "challenge",
                "js_challenge"
              ],
              "mode": "challenge",
              "configuration": {
                "target": "ip",
                "value": "198.51.100.4"
              },
              "created_on": "2014-01-01T05:20:00.12345Z",
              "modified_on": "2014-01-01T05:20:00.12345Z",
              "scope": {
                "id": "fake-rule-scope",
                "name": "Some Account",
                "type": "account"
              }
            }
          }"""

      val removedAccessRule =
        json"""{
            "success": true,
            "errors": null,
            "messages": null,
            "result": null
          }"""
    }

    object Failures {

      case class Failure(status: Status, json: Json)

      val accessRuleCreationErrorMode = Failure(Status.BadRequest,
        json"""{
            "success": false,
            "errors": [
              {
                "message": "firewallaccessrules.api.validation_error:unsupported target received: ,invalid mode received: "
              }
            ],
            "messages": [],
            "result": null
          }
        """)
    }

  }

}
