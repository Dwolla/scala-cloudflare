package dwolla.cloudflare

import cats.effect._
import cats.effect.testing.specs2.CatsEffect
import com.dwolla.cloudflare._
import com.dwolla.cloudflare.domain.model._
import com.dwolla.cloudflare.domain.model.accounts._
import io.circe.literal._
import org.http4s.client.Client
import org.http4s.{HttpRoutes, Status}
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import shapeless.tag.@@

class AccountsClientSpec
  extends Specification
    with CatsEffect {

  def tagString[T](s: String): String @@ T = shapeless.tag[T][String](s)

  trait Setup extends Scope {
    val authorization = CloudflareAuthorization("email", "key")
    val fakeService = new FakeCloudflareService(authorization)

    val fakeAccountId1 = tagString[AccountIdTag]("fake-account-id1")
    val fakeAccountId2 = tagString[AccountIdTag]("fake-account-id2")
    val fakeAccountId3 = tagString[AccountIdTag]("fake-account-id3")
  }

  "list" should {
    "return all accounts across pages" in new Setup {
      val http4sClient = fakeService.client(fakeService.listAccounts(Map(1 -> SampleResponses.Successes.listAccountsPage1, 2 -> SampleResponses.Successes.listAccountsPage2, 3 -> SampleResponses.Successes.listAccountsPage3)))
      val client = buildAccountsClient(http4sClient, authorization)

      client.list().compile.toList.map(_ must be_==(
        List(
          Account(
            id = fakeAccountId1,
            name = "Fake Account Org",
            settings = AccountSettings(enforceTwoFactor = false)
          ),
          Account(
            id = fakeAccountId2,
            name = "Fake Account Org 2",
            settings = AccountSettings(enforceTwoFactor = false)
          ),
          Account(
            id = fakeAccountId3,
            name = "Fake Account Org 3",
            settings = AccountSettings(enforceTwoFactor = true)
          )
        )
      ))
    }

    "return all accounts across pages doesn't fetch eagerly" in new Setup {
      val http4sClient = fakeService.client(fakeService.listAccounts(Map(1 -> SampleResponses.Successes.listAccountsPage1)))
      val client = buildAccountsClient(http4sClient, authorization)

      client.list().take(1).compile.toList.map(_ must be_==(
        List(
          Account(
            id = fakeAccountId1,
            name = "Fake Account Org",
            settings = AccountSettings(enforceTwoFactor = false)
          )
        )
      ))
    }
  }

  "getByUri" should {
    "get account by uri" in new Setup {
      val http4sClient = fakeService.client(fakeService.accountById(SampleResponses.Successes.getAccount, fakeAccountId1))
      val client = buildAccountsClient(http4sClient, authorization)

      private val output = client.getByUri(s"https://api.cloudflare.com/client/v4/accounts/$fakeAccountId1")

      output.compile.last.map(_ must beSome(
        Account(
          id = fakeAccountId1,
          name = "Fake Account Org",
          settings = AccountSettings(enforceTwoFactor = false)
        )
      ))
    }

    "return None for invalid URIs" in new Setup {
      val client = buildAccountsClient(fakeService.client(HttpRoutes.empty[IO]), authorization)

      private val output = client.getByUri("https://hydragents.xyz")

      client.parseUri("https://hydragents.xyz") must beNone
      output.compile.toList.map(_ must beEmpty[List[Account]])
    }

  }

  "getById" should {
    "get account by id" in new Setup {
      val accountId: String = fakeAccountId1

      val http4sClient = fakeService.client(fakeService.accountById(SampleResponses.Successes.getAccount, accountId))
      val client = buildAccountsClient(http4sClient, authorization)

      client
        .getById(accountId)
        .compile
        .toList
        .map(_.headOption)
        .map {
          _ must beSome(
            Account(
              id = fakeAccountId1,
              name = "Fake Account Org",
              settings = AccountSettings(enforceTwoFactor = false)
            )
          )
        }
    }

    "return None if not found" in new Setup {
      val accountId = tagString[AccountIdTag]("missing-id")

      val failure = SampleResponses.Failures.accountDoesNotExist
      val http4sClient = fakeService.client(fakeService.accountById(failure.json, accountId, failure.status))
      val client = buildAccountsClient(http4sClient, authorization)

      client
        .getById(accountId)
        .compile
        .toList
        .map(_.headOption)
        .map(_ must beNone)
    }
  }

  "getByName" should {
    "get account by name" in new Setup {
      val accountName = "Another Fake Account Biz"

      val http4sClient = fakeService.client(fakeService.listAccounts(Map(1 -> SampleResponses.Successes.listAccounts)))
      val client = buildAccountsClient(http4sClient, authorization)

      client
        .getByName(accountName)
        .compile
        .toList
        .map(_.headOption)
        .map {
          _ must beSome(
            Account(
              id = fakeAccountId2,
              name = accountName,
              settings = AccountSettings(enforceTwoFactor = true)
            )
          )
        }
    }

    "find account by name across multiple pages of accounts" in new Setup {
      val accountName = "Fake Account Org 3"

      val http4sClient = fakeService.client(fakeService.listAccounts(Map(1 -> SampleResponses.Successes.listAccountsPage1, 2 -> SampleResponses.Successes.listAccountsPage2, 3 -> SampleResponses.Successes.listAccountsPage3)))
      val client = buildAccountsClient(http4sClient, authorization)

      client
        .getByName(accountName)
        .compile
        .toList
        .map(_.headOption)
        .map {
          _ must beSome(
            Account(
              id = fakeAccountId3,
              name = accountName,
              settings = AccountSettings(enforceTwoFactor = true)
            )
          )
        }

    }

    "return None if not found" in new Setup {
      val http4sClient = fakeService.client(fakeService.listAccounts(Map(1 -> SampleResponses.Successes.listAccounts)))
      val client = buildAccountsClient(http4sClient, authorization)

      client
        .getByName("Test Stuff")
        .compile
        .toList
        .map(_.headOption)
        .map(_ must beNone)
    }
  }

  "listRoles" should {
    "return all account roles across pages" in new Setup {
      val accountId = tagString[AccountIdTag]("fake-account-id")

      val http4sClient = fakeService.client(fakeService.listAccountRoles(Map(1 -> SampleResponses.Successes.getRolesPage1, 2 -> SampleResponses.Successes.getRolesPage2, 3 -> SampleResponses.Successes.getRolesPage3), accountId))
      val client = buildAccountsClient(http4sClient, authorization)

      client
        .listRoles(accountId)
        .compile
        .toList
        .map {
          _ must be_==(
            List(
              AccountRole(
                id = "1111",
                name = "Fake Role 1",
                description = "this is the first fake role",
                permissions = Map[String, AccountRolePermissions]("analytics" -> AccountRolePermissions(read = true, edit = false))
              ),
              AccountRole(
                id = "2222",
                name = "Fake Role 2",
                description = "second fake role",
                permissions = Map[String, AccountRolePermissions](
                  "zone" -> AccountRolePermissions(read = true, edit = false),
                  "logs" -> AccountRolePermissions(read = true, edit = false)
                )
              ),
              AccountRole(
                id = "3333",
                name = "Fake Full Role 3",
                description = "full permissions",
                permissions = Map[String, AccountRolePermissions](
                  "legal" -> AccountRolePermissions(read = true, edit = true),
                  "billing" -> AccountRolePermissions(read = true, edit = true)
                )
              )
            )
          )
        }

    }

    "return all account roles across pages doesn't fetch eagerly" in new Setup {
      val accountId = tagString[AccountIdTag]("fake-account-id")

      val http4sClient = fakeService.client(fakeService.listAccountRoles(Map(1 -> SampleResponses.Successes.getRolesPage1), accountId))
      val client = buildAccountsClient(http4sClient, authorization)

      client
        .listRoles(accountId)
        .take(1)
        .compile
        .toList
        .map {
          _ must be_==(
            List(
              AccountRole(
                id = "1111",
                name = "Fake Role 1",
                description = "this is the first fake role",
                permissions = Map[String, AccountRolePermissions]("analytics" -> AccountRolePermissions(read = true, edit = false))
              )
            )
          )
        }
    }
  }

  def buildAccountsClient[F[_]: Concurrent](http4sClient: Client[F], authorization: CloudflareAuthorization): AccountsClient[F] = {
    val fakeHttp4sExecutor = new StreamingCloudflareApiExecutor(http4sClient, authorization)
    AccountsClient(fakeHttp4sExecutor)
  }

  private object SampleResponses {
    object Successes {
      val listAccounts =
    json"""{
             "result": [
               {
                 "id": "fake-account-id1",
                 "name": "Fake Account Org",
                 "settings":
                 {
                   "enforce_twofactor": false
                 }
               },
               {
                 "id": "fake-account-id2",
                 "name": "Another Fake Account Biz",
                 "settings":
                 {
                   "enforce_twofactor": true
                 }
               }
             ],
             "result_info": {
               "page": 1,
               "per_page": 20,
               "total_pages": 1,
               "count": 2,
               "total_count": 2
             },
             "success": true,
             "errors": [],
             "messages": []
           }
        """.noSpaces

      val listAccountsPage1 =
    json"""{
             "result": [
               {
                 "id": "fake-account-id1",
                 "name": "Fake Account Org",
                 "settings":
                 {
                   "enforce_twofactor": false
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
           }
        """.noSpaces

      val listAccountsPage2 =
    json"""{
             "result": [
               {
                 "id": "fake-account-id2",
                 "name": "Fake Account Org 2",
                 "settings":
                 {
                   "enforce_twofactor": false
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
           }
        """.noSpaces

      val listAccountsPage3 =
    json"""{
             "result": [
               {
                 "id": "fake-account-id3",
                 "name": "Fake Account Org 3",
                 "settings":
                 {
                   "enforce_twofactor": true
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
           }
        """.noSpaces

      val getAccount =
    json"""{
             "result": {
               "id": "fake-account-id1",
               "name": "Fake Account Org",
               "settings": {
                 "enforce_twofactor": false
               }
             },
             "success": true,
             "errors": [],
             "messages": []
           }
        """.noSpaces

      val getRoles =
    json"""
           {
             "result": [
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
             ],
             "result_info": {
               "page": 1,
               "per_page": 20,
               "total_pages": 1,
               "count": 2,
               "total_count": 2
             },
             "success": true,
             "errors": [],
             "messages": []
           }
        """.noSpaces

      val getRolesPage1 =
    json"""
           {
             "result": [
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
           }
        """.noSpaces

      val getRolesPage2 =
    json"""
           {
             "result": [
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
           }
        """.noSpaces

      val getRolesPage3 =
    json"""
           {
             "result": [
               {
                 "id": "3333",
                 "name": "Fake Full Role 3",
                 "description": "full permissions",
                 "permissions":
                 {
                   "legal":
                   {
                     "read": true,
                     "edit": true
                   },
                   "billing":
                   {
                     "read": true,
                     "edit": true
                   }
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
           }
        """.noSpaces
    }

    object Failures {
      case class Failure(status: Status, json: String)

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
