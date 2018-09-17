package dwolla.cloudflare

import cats.effect._
import com.dwolla.cloudflare._
import com.dwolla.cloudflare.domain.model.Exceptions.UnexpectedCloudflareErrorException
import com.dwolla.cloudflare.domain.model._
import com.dwolla.cloudflare.domain.model.accounts._
import org.http4s._
import org.http4s.client.Client
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import shapeless.tag.@@
import io.circe.literal._

import scala.language.higherKinds

class AccountMembersClientSpec(implicit ee: ExecutionEnv) extends Specification {
  def tagString[T](s: String): String @@ T = shapeless.tag[T][String](s)

  trait Setup extends Scope {
    val authorization = CloudflareAuthorization("email", "key")
    val fakeService = new FakeCloudflareService(authorization)

    val fakeAccountId1 = tagString[AccountIdTag]("fake-account-id1")
    val fakeAccountId2 = tagString[AccountIdTag]("fake-account-id2")
    val fakeAccountId3 = tagString[AccountIdTag]("fake-account-id3")
  }

  "getMember" should {
    "get account member by id and account id" in new Setup {
      val accountId = fakeAccountId1
      val accountMemberId = tagString[AccountMemberIdTag]("fake-account-member-id")

      val http4sClient = fakeService.client(fakeService.getAccountMember(SampleResponses.Successes.accountMember, accountId, accountMemberId))
      val client = buildAccountMembersClient(http4sClient, authorization)

      private val output = client.getByUri(s"https://api.cloudflare.com/client/v4/accounts/$accountId/members/$accountMemberId")

      output.compile.last.unsafeToFuture() must beSome(
        AccountMember(
          id = accountMemberId,
          user = User(
            id = tagString[UserIdTag]("fake-user-id"),
            firstName = None,
            lastName = None,
            emailAddress = "myemail@test.com",
            twoFactorEnabled = false
          ),
          status = "pending",
          roles = List(
            AccountRole(
              id = "1111",
              name = "Fake Role 1",
              description = "this is the first fake role",
              permissions = Map[String, AccountRolePermissions]("analytics" → AccountRolePermissions(read = true, edit = false))
            ),
            AccountRole(
              id = "2222",
              name = "Fake Role 2",
              description = "second fake role",
              permissions = Map[String, AccountRolePermissions](
                "zone" → AccountRolePermissions(read = true, edit = false),
                "logs" → AccountRolePermissions(read = true, edit = false)
              )
            )
          )
        )
      ).await
    }

    "return None if not found" in new Setup {
      val accountId = fakeAccountId1
      val accountMemberId = tagString[AccountMemberIdTag]("missing-account-member-id")

      val failure = SampleResponses.Failures.accountMemberDoesNotExist
      val http4sClient = fakeService.client(fakeService.getAccountMember(failure.json, accountId, accountMemberId, failure.status))
      val client = buildAccountMembersClient(http4sClient, authorization)

      private val output = client.getById(accountId, accountMemberId)

      output.compile.last.unsafeToFuture() must beNone.await
    }
  }

  "addMember" should {
    "add new member" in new Setup {
      val accountId = fakeAccountId1
      val accountMemberId = tagString[AccountMemberIdTag]("fake-account-member-id")
      val email = "myemail@test.com"
      val roleIds = List("1111", "2222")

      val http4sClient = fakeService.client(fakeService.addAccountMember(SampleResponses.Successes.accountMember, accountId))
      val client = buildAccountMembersClient(http4sClient, authorization)

      private val output = client.addMember(accountId, email, roleIds)

      output.compile.toList.unsafeToFuture() must contain(
        AccountMember(
          id = accountMemberId,
          user = User(
            id = tagString[UserIdTag]("fake-user-id"),
            firstName = None,
            lastName = None,
            emailAddress = email,
            twoFactorEnabled = false
          ),
          status = "pending",
          roles = List(
            AccountRole(
              id = "1111",
              name = "Fake Role 1",
              description = "this is the first fake role",
              permissions = Map[String, AccountRolePermissions]("analytics" → AccountRolePermissions(read = true, edit = false))
            ),
            AccountRole(
              id = "2222",
              name = "Fake Role 2",
              description = "second fake role",
              permissions = Map[String, AccountRolePermissions](
                "zone" → AccountRolePermissions(read = true, edit = false),
                "logs" → AccountRolePermissions(read = true, edit = false)
              )
            )
          )
        )
      ).await
    }

    "throw unexpected exception if error adding new member" in new Setup {
      val accountId = fakeAccountId1
      val email = "me@abc123test.com"
      val roleIds = List("1111", "2222")

      val failure = SampleResponses.Failures.accountMemberCreationError
      val http4sClient = fakeService.client(fakeService.addAccountMember(failure.json, accountId, failure.status))
      val client = buildAccountMembersClient(http4sClient, authorization)

      private val output = client.addMember(accountId, email, roleIds)

      output.compile.last.attempt.unsafeToFuture() must beLeft[Throwable].like {
        case ex: UnexpectedCloudflareErrorException ⇒ ex.getMessage must_==
          """An unexpected Cloudflare error occurred. Errors:
            |
            | - Error(Some(1001),Invalid request: Value required for parameter 'email'.)
            |     """.stripMargin
      }.await
    }
  }

  "updateMember" should {
    "update existing member" in new Setup {
      val email = "myemail@test.com"
      val accountId = fakeAccountId1
      val accountMemberId = tagString[AccountMemberIdTag]("fake-account-member-id")

      val updatedAccountMember = AccountMember(
        id = accountMemberId,
        user = User(
          id = tagString[UserIdTag]("fake-user-id"),
          firstName = Some("Joe"),
          lastName = Some("Smith"),
          emailAddress = email,
          twoFactorEnabled = false
        ),
        status = "pending",
        roles = List(
          AccountRole(
            id = "1111",
            name = "Fake Role 1",
            description = "this is the first fake role",
            permissions = Map[String, AccountRolePermissions]("analytics" → AccountRolePermissions(read = true, edit = false))
          ),
          AccountRole(
            id = "2222",
            name = "Fake Role 2",
            description = "second fake role",
            permissions = Map[String, AccountRolePermissions](
              "zone" → AccountRolePermissions(read = true, edit = false),
              "logs" → AccountRolePermissions(read = true, edit = false)
            )
          ),
          AccountRole(
            id = "3333",
            name = "Fake Role 3",
            description = "third fake role",
            permissions = Map[String, AccountRolePermissions](
              "crypto" → AccountRolePermissions(read = true, edit = false)
            )
          )
        )
      )

      val http4sClient = fakeService.client(fakeService.updateAccountMember(SampleResponses.Successes.updatedAccountMember, accountId, updatedAccountMember.id))
      val client = buildAccountMembersClient(http4sClient, authorization)

      private val output = client.updateMember(accountId, updatedAccountMember)

      output.compile.toList.unsafeToFuture() must contain(updatedAccountMember).await
    }

    "throw unexpected exception if error updating existing member" in new Setup {
      val accountId = fakeAccountId1
      val accountMemberId = tagString[AccountMemberIdTag]("fake-account-member-id")

      val updatedAccountMember = AccountMember(
        id = accountMemberId,
        user = User(
          id = tagString[UserIdTag]("fake-user-id"),
          firstName = Some("Joe"),
          lastName = Some("Smith"),
          emailAddress = "myemail@test.com",
          twoFactorEnabled = false
        ),
        status = "pending",
        roles = List(
          AccountRole(
            id = "1111",
            name = "Fake Role 1",
            description = "this is the first fake role",
            permissions = Map[String, AccountRolePermissions]("analytics" → AccountRolePermissions(read = true, edit = false))
          ),
          AccountRole(
            id = "2222",
            name = "Fake Role 2",
            description = "second fake role",
            permissions = Map[String, AccountRolePermissions](
              "zone" → AccountRolePermissions(read = true, edit = false),
              "logs" → AccountRolePermissions(read = true, edit = false)
            )
          ),
          AccountRole(
            id = "3333",
            name = "Fake Role 3",
            description = "third fake role",
            permissions = Map[String, AccountRolePermissions](
              "crypto" → AccountRolePermissions(read = true, edit = false)
            )
          )
        )
      )

      val failure = SampleResponses.Failures.accountMemberUpdateError
      val http4sClient = fakeService.client(fakeService.updateAccountMember(failure.json, accountId, updatedAccountMember.id, failure.status))
      val client = buildAccountMembersClient(http4sClient, authorization)

      private val output = client.updateMember(accountId, updatedAccountMember)

      output.compile.last.attempt.unsafeToFuture() must beLeft[Throwable].like {
        case ex: UnexpectedCloudflareErrorException ⇒ ex.getMessage must_==
          """An unexpected Cloudflare error occurred. Errors:
            |
            | - Error(Some(1001),Invalid request: Invalid roles)
            |     """.stripMargin
      }.await
    }
  }

  "removeMember" should {
    "remove member from account" in new Setup {
      val accountId = fakeAccountId1
      val accountMemberId = tagString[AccountMemberIdTag]("fake-account-member-id")

      val http4sClient = fakeService.client(fakeService.removeAccountMember(SampleResponses.Successes.removedAccountMember, accountId, accountMemberId))
      val client = buildAccountMembersClient(http4sClient, authorization)

      private val output = client.removeMember(accountId, accountMemberId)

      output.compile.last.unsafeToFuture() must beSome(accountMemberId).await
    }

    "throw unexpected exception if error removing member" in new Setup {
      val accountId = fakeAccountId1
      val accountMemberId = tagString[AccountMemberIdTag]("fake-account-member-id")

      val failure = SampleResponses.Failures.accountMemberRemovalError
      val http4sClient = fakeService.client(fakeService.removeAccountMember(failure.json, accountId, accountMemberId, failure.status))
      val client = buildAccountMembersClient(http4sClient, authorization)

      private val output = client.removeMember(accountId, accountMemberId)

      output.compile.last.attempt.unsafeToFuture() must beLeft[Throwable].like {
        case ex: UnexpectedCloudflareErrorException ⇒ ex.getMessage must_==
          """An unexpected Cloudflare error occurred. Errors:
            |
            | - Error(Some(7003),Could not route to /accounts/fake-account-id1/members/fake-account-member-id, perhaps your object identifier is invalid?)
            | - Error(Some(7000),No route for that URI)
            |     """.stripMargin
      }.await
    }

    "throw not found exception if member not in account" in new Setup {
      val accountId = fakeAccountId1
      val accountMemberId = tagString[AccountMemberIdTag]("fake-account-member-id")

      val failure = SampleResponses.Failures.accountDoesNotExist
      val http4sClient = fakeService.client(fakeService.removeAccountMember(failure.json, accountId, accountMemberId, failure.status))
      val client = buildAccountMembersClient(http4sClient, authorization)

      private val output = client.removeMember(accountId, accountMemberId)

      output.compile.last.attempt.unsafeToFuture() must beLeft[Throwable].like {
        case ex: AccountMemberDoesNotExistException ⇒ ex.getMessage must_==
          "The account member fake-account-member-id not found for account fake-account-id1."
      }.await
    }
  }

  private def buildAccountMembersClient[F[_]: Sync](http4sClient: Client[F], authorization: CloudflareAuthorization): AccountMembersClient[F] =
    AccountMembersClient(new StreamingCloudflareApiExecutor(http4sClient, authorization))

  private object SampleResponses {
    object Successes {
      val accountMember =
    json"""{
             "success": true,
             "errors": [],
             "messages": [],
             "result": {
               "id": "fake-account-member-id",
               "user":
               {
                 "id": "fake-user-id",
                 "first_name": null,
                 "last_name": null,
                 "email": "myemail@test.com",
                 "two_factor_authentication_enabled": false
               },
               "status": "pending",
               "roles": [
                 {
                   "id": "1111",
                   "name": "Fake Role 1",
                   "description": "this is the first fake role",
                   "permissions":
                   {
                     "analytics":
                     {
                       "read": true,
                       "edit": false
                     }
                   }
                 },
                 {
                   "id": "2222",
                   "name": "Fake Role 2",
                   "description": "second fake role",
                   "permissions":
                   {
                     "zone":
                     {
                       "read": true,
                       "edit": false
                     },
                     "logs":
                     {
                       "read": true,
                       "edit": false
                     }
                   }
                 }
               ]
             }
           }
        """.noSpaces

      val updatedAccountMember =
    json"""{
             "success": true,
             "errors": [],
             "messages": [],
             "result": {
               "id": "fake-account-member-id",
               "user":
               {
                 "id": "fake-user-id",
                 "first_name": "Joe",
                 "last_name": "Smith",
                 "email": "myemail@test.com",
                 "two_factor_authentication_enabled": false
               },
               "status": "pending",
               "roles": [
                 {
                   "id": "1111",
                   "name": "Fake Role 1",
                   "description": "this is the first fake role",
                   "permissions":
                   {
                     "analytics":
                     {
                       "read": true,
                       "edit": false
                     }
                   }
                 },
                 {
                   "id": "2222",
                   "name": "Fake Role 2",
                   "description": "second fake role",
                   "permissions":
                   {
                     "zone":
                     {
                       "read": true,
                       "edit": false
                     },
                     "logs":
                     {
                       "read": true,
                       "edit": false
                     }
                   }
                 },
                 {
                   "id": "3333",
                   "name": "Fake Role 3",
                   "description": "third fake role",
                   "permissions":
                   {
                     "crypto":
                     {
                       "read": true,
                       "edit": false
                     }
                   }
                 }
               ]
             }
           }
        """.noSpaces

      val removedAccountMember =
    json"""
           {
             "result": {
               "id": "fake-account-member-id"
             },
             "success": true,
             "errors": [],
             "messages": []
           }
        """.noSpaces
    }

    object Failures {
      case class Failure(status: Status, json: String)

      val accountMemberRemovalError = Failure(Status.BadRequest,
    json"""{
             "success": false,
             "errors": [
               {
                 "code": 7003,
                 "message": "Could not route to /accounts/fake-account-id1/members/fake-account-member-id, perhaps your object identifier is invalid?"
               },
               {
                 "code": 7000,
                 "message": "No route for that URI"
               }
             ],
             "messages": [],
             "result": null
           }
        """.noSpaces)

      val accountMemberUpdateError = Failure(Status.BadRequest,
    json"""{
             "success": false,
             "errors": [
               {
                 "code": 1001,
                 "message": "Invalid request: Invalid roles"
               }
             ],
             "messages": [],
             "result": null
           }
        """.noSpaces)

      val accountMemberCreationError = Failure(Status.BadRequest,
    json"""{
             "success": false,
             "errors": [
               {
                 "code": 1001,
                 "message": "Invalid request: Value required for parameter 'email'."
               }
             ],
             "messages": [],
             "result": null
           }
        """.noSpaces)

      val accountMemberDoesNotExist = Failure(Status.NotFound,
    json"""
           {
             "success": false,
             "errors": [
               {
                 "code": 1003,
                 "message": "Member not found for account"
                }
             ],
             "messages": [],
             "result": null
           }
        """.noSpaces)

      val accountDoesNotExist = Failure(Status.NotFound,
    json"""{
             "success": false,
             "errors": [
               {
                 "code": 1003,
                 "message": "Account not found"
               }
             ],
             "messages": [],
             "result": null
           }
        """.noSpaces)
    }
  }

}
