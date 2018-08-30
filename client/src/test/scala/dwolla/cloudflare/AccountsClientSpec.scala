package dwolla.cloudflare

import cats.effect._
import com.dwolla.cloudflare._
import com.dwolla.cloudflare.domain.model.Exceptions.UnexpectedCloudflareErrorException
import com.dwolla.cloudflare.domain.model.accounts._
import org.http4s.Status
import org.http4s.client.Client
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

import scala.language.higherKinds

class AccountsClientSpec extends Specification {
  trait Setup extends Scope {
    val authorization = CloudflareAuthorization("email", "key")
    val fakeService = new FakeCloudflareService(authorization)
  }

  "list" should {
    "return all accounts across pages" in new Setup {
      val http4sClient = fakeService.client(fakeService.listAccounts(Map(1 → SampleResponses.Successes.listAccountsPage1, 2 → SampleResponses.Successes.listAccountsPage2, 3 → SampleResponses.Successes.listAccountsPage3)))
      val client = buildAccountsClient(http4sClient, authorization)

      val output: List[Account] = client.list().compile.toList.unsafeRunSync()
      output must be_==(
        List(
          Account(
            id = "fake-account-id1",
            name = "Fake Account Org",
            settings = AccountSettings(enforceTwoFactor = false)
          ),
          Account(
            id = "fake-account-id2",
            name = "Fake Account Org 2",
            settings = AccountSettings(enforceTwoFactor = false)
          ),
          Account(
            id = "fake-account-id3",
            name = "Fake Account Org 3",
            settings = AccountSettings(enforceTwoFactor = true)
          )
        )
      )
    }

    "return all accounts across pages doesn't fetch eagerly" in new Setup {
      val http4sClient = fakeService.client(fakeService.listAccounts(Map(1 → SampleResponses.Successes.listAccountsPage1)))
      val client = buildAccountsClient(http4sClient, authorization)

      val output: List[Account] = client.list().take(1).compile.toList.unsafeRunSync()
      output must be_==(
        List(
          Account(
            id = "fake-account-id1",
            name = "Fake Account Org",
            settings = AccountSettings(enforceTwoFactor = false)
          )
        )
      )
    }
  }

  "getById" should {
    "get account by id" in new Setup {
      val accountId = "fake-account-id1"

      val http4sClient = fakeService.client(fakeService.accountById(SampleResponses.Successes.getAccount, accountId))
      val client = buildAccountsClient(http4sClient, authorization)

      val output: Option[Account] = client.getById(accountId)
        .compile.toList.map(_.headOption).unsafeRunSync()

      output must beSome(
        Account(
          id = accountId,
          name = "Fake Account Org",
          settings = AccountSettings(enforceTwoFactor = false)
        )
      )
    }

    "return None if not found" in new Setup {
      val accountId = "missing-id"

      val failure = SampleResponses.Failures.accountDoesNotExist
      val http4sClient = fakeService.client(fakeService.accountById(failure.json, accountId, failure.status))
      val client = buildAccountsClient(http4sClient, authorization)

      val output: Option[Account] = client.getById(accountId)
        .compile.toList.map(_.headOption).unsafeRunSync()

      output must beNone
    }
  }

  "getByName" should {
    "get account by name" in new Setup {
      val accountName = "Another Fake Account Biz"

      val http4sClient = fakeService.client(fakeService.listAccounts(Map(1 → SampleResponses.Successes.listAccounts)))
      val client = buildAccountsClient(http4sClient, authorization)

      val output: Option[Account] = client.getByName(accountName)
        .compile.toList.map(_.headOption).unsafeRunSync()

      output must beSome(
        Account(
          id = "fake-account-id2",
          name = accountName,
          settings = AccountSettings(enforceTwoFactor = true)
        )
      )
    }

    "find account by name across multiple pages of accounts" in new Setup {
      val accountName = "Fake Account Org 3"

      val http4sClient = fakeService.client(fakeService.listAccounts(Map(1 → SampleResponses.Successes.listAccountsPage1, 2 → SampleResponses.Successes.listAccountsPage2, 3 → SampleResponses.Successes.listAccountsPage3)))
      val client = buildAccountsClient(http4sClient, authorization)

      val output: Option[Account] = client.getByName(accountName)
        .compile.toList.map(_.headOption).unsafeRunSync()

      output must beSome(
        Account(
          id = "fake-account-id3",
          name = accountName,
          settings = AccountSettings(enforceTwoFactor = true)
        )
      )
    }

    "return None if not found" in new Setup {
      val http4sClient = fakeService.client(fakeService.listAccounts(Map(1 → SampleResponses.Successes.listAccounts)))
      val client = buildAccountsClient(http4sClient, authorization)

      val output: Option[Account] = client.getByName("Test Stuff")
        .compile.toList.map(_.headOption).unsafeRunSync()

      output must beNone
    }
  }

  "listRoles" should {
    "return all account roles across pages" in new Setup {
      val accountId = "fake-account-id"

      val http4sClient = fakeService.client(fakeService.listAccountRoles(Map(1 → SampleResponses.Successes.getRolesPage1, 2 → SampleResponses.Successes.getRolesPage2, 3 → SampleResponses.Successes.getRolesPage3), accountId))
      val client = buildAccountsClient(http4sClient, authorization)

      val output: List[AccountRole] = client.listRoles(accountId).compile.toList.unsafeRunSync()
      output must be_==(
        List(
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
            name = "Fake Full Role 3",
            description = "full permissions",
            permissions = Map[String, AccountRolePermissions](
              "legal" → AccountRolePermissions(read = true, edit = true),
              "billing" → AccountRolePermissions(read = true, edit = true)
            )
          )
        )
      )
    }

    "return all account roles across pages doesn't fetch eagerly" in new Setup {
      val accountId = "fake-account-id"

      val http4sClient = fakeService.client(fakeService.listAccountRoles(Map(1 → SampleResponses.Successes.getRolesPage1), accountId))
      val client = buildAccountsClient(http4sClient, authorization)

      val output: List[AccountRole] = client.listRoles(accountId).take(1).compile.toList.unsafeRunSync()
      output must be_==(
        List(
          AccountRole(
            id = "1111",
            name = "Fake Role 1",
            description = "this is the first fake role",
            permissions = Map[String, AccountRolePermissions]("analytics" → AccountRolePermissions(read = true, edit = false))
          )
        )
      )
    }
  }

  "getMember" should {
    "get account member by id and account id" in new Setup {
      val accountId = "fake-account-id1"
      val accountMemberId = "fake-account-member-id"

      val http4sClient = fakeService.client(fakeService.getAccountMember(SampleResponses.Successes.accountMember, accountId, accountMemberId))
      val client = buildAccountsClient(http4sClient, authorization)

      val output: Option[AccountMember] = client.getMember(accountId, accountMemberId)
        .compile.toList.map(_.headOption).unsafeRunSync()

      output must beSome(
        AccountMember(
          id = accountMemberId,
          user = User(
            id = "fake-user-id",
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
      )
    }

    "return None if not found" in new Setup {
      val accountId = "fake-account-id1"
      val accountMemberId = "missing-account-member-id"

      val failure = SampleResponses.Failures.accountMemberDoesNotExist
      val http4sClient = fakeService.client(fakeService.getAccountMember(failure.json, accountId, accountMemberId, failure.status))
      val client = buildAccountsClient(http4sClient, authorization)

      val output: Option[AccountMember] = client.getMember(accountId, accountMemberId)
        .compile.toList.map(_.headOption).unsafeRunSync()

      output must beNone
    }
  }

  "addMember" should {
    "add new member" in new Setup {
      val accountId = "fake-account-id1"
      val accountMemberId = "fake-account-member-id"
      val email = "myemail@test.com"
      val roleIds = List("1111", "2222")

      val http4sClient = fakeService.client(fakeService.addAccountMember(SampleResponses.Successes.accountMember, accountId))
      val client = buildAccountsClient(http4sClient, authorization)

      val output = client.addMember(accountId, email, roleIds)
        .compile.toList.unsafeRunSync()

      output must be_==(
        List(AccountMember(
            id = accountMemberId,
            user = User(
              id = "fake-user-id",
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
        )
      )
    }

    "throw unexpected exception if error adding new member" in new Setup {
      val accountId = "fake-account-id1"
      val email = "me@abc123test.com"
      val roleIds = List("1111", "2222")

      val failure = SampleResponses.Failures.accountMemberCreationError
      val http4sClient = fakeService.client(fakeService.addAccountMember(failure.json, accountId, failure.status))
      val client = buildAccountsClient(http4sClient, authorization)

      val output = client.addMember(accountId, email, roleIds)
        .compile
        .toList
        .attempt
        .unsafeRunSync()

      output must beLeft[Throwable].like {
        case ex: UnexpectedCloudflareErrorException ⇒ ex.getMessage must_==
          """An unexpected Cloudflare error occurred. Errors:
            |
            | - Error(Some(1001),Invalid request: Value required for parameter 'email'.)
            |     """.stripMargin
      }
    }
  }

  "updateMember" should {
    "update existing member" in new Setup {
      val email = "myemail@test.com"
      val accountId = "fake-account-id1"
      val accountMemberId = "fake-account-member-id"

      val updatedAccountMember = AccountMember(
        id = accountMemberId,
        user = User(
          id = "fake-user-id",
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
      val client = buildAccountsClient(http4sClient, authorization)

      val output = client.updateMember(accountId, updatedAccountMember)
        .compile.toList.unsafeRunSync()

      output must be_==(List(updatedAccountMember))
    }

    "throw unexpected exception if error updating existing member" in new Setup {
      val accountId = "fake-account-id1"
      val accountMemberId = "fake-account-member-id"

      val updatedAccountMember = AccountMember(
        id = accountMemberId,
        user = User(
          id = "fake-user-id",
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
      val client = buildAccountsClient(http4sClient, authorization)

      val output = client.updateMember(accountId, updatedAccountMember)
        .compile
        .toList
        .attempt
        .unsafeRunSync()

      output must beLeft[Throwable].like {
        case ex: UnexpectedCloudflareErrorException ⇒ ex.getMessage must_==
          """An unexpected Cloudflare error occurred. Errors:
            |
            | - Error(Some(1001),Invalid request: Invalid roles)
            |     """.stripMargin
      }
    }
  }

  "removeMember" should {
    "remove member from account" in new Setup {
      val accountId = "fake-account-id1"
      val accountMemberId = "fake-account-member-id"

      val http4sClient = fakeService.client(fakeService.removeAccountMember(SampleResponses.Successes.removedAccountMember, accountId, accountMemberId))
      val client = buildAccountsClient(http4sClient, authorization)

      val output = client.removeMember(accountId, accountMemberId)
        .compile.toList.unsafeRunSync()

      output must be_==(List(accountMemberId))
    }

    "throw unexpected exception if error removing member" in new Setup {
      val accountId = "fake-account-id1"
      val accountMemberId = "fake-account-member-id"

      val failure = SampleResponses.Failures.accountMemberRemovalError
      val http4sClient = fakeService.client(fakeService.removeAccountMember(failure.json, accountId, accountMemberId, failure.status))
      val client = buildAccountsClient(http4sClient, authorization)

      val output = client.removeMember(accountId, accountMemberId)
        .compile
        .toList
        .attempt
        .unsafeRunSync()

      output must beLeft[Throwable].like {
        case ex: UnexpectedCloudflareErrorException ⇒ ex.getMessage must_==
          """An unexpected Cloudflare error occurred. Errors:
            |
            | - Error(Some(7003),Could not route to /accounts/fake-account-id1/members/fake-account-member-id, perhaps your object identifier is invalid?)
            | - Error(Some(7000),No route for that URI)
            |     """.stripMargin
      }
    }

    "throw not found exception if member not in account" in new Setup {
      val accountId = "fake-account-id1"
      val accountMemberId = "fake-account-member-id"

      val failure = SampleResponses.Failures.accountDoesNotExist
      val http4sClient = fakeService.client(fakeService.removeAccountMember(failure.json, accountId, accountMemberId, failure.status))
      val client = buildAccountsClient(http4sClient, authorization)

      val output = client.removeMember(accountId, accountMemberId)
        .compile
        .toList
        .attempt
        .unsafeRunSync()

      output must beLeft[Throwable].like {
        case ex: AccountMemberDoesNotExistException ⇒ ex.getMessage must_==
          "The account member fake-account-member-id not found for account fake-account-id1."
      }
    }
  }

  def buildAccountsClient[F[_]: Sync](http4sClient: Client[F], authorization: CloudflareAuthorization): AccountsClient[F] = {
    val fakeHttp4sExecutor = new StreamingCloudflareApiExecutor(http4sClient, authorization)
    AccountsClient(fakeHttp4sExecutor)
  }

  private object SampleResponses {
    object Successes {
      val listAccounts =
        """{
          |  "result": [
          |    {
          |      "id": "fake-account-id1",
          |      "name": "Fake Account Org",
          |      "settings":
          |      {
          |        "enforce_twofactor": false
          |      }
          |    },
          |    {
          |      "id": "fake-account-id2",
          |      "name": "Another Fake Account Biz",
          |      "settings":
          |      {
          |        "enforce_twofactor": true
          |      }
          |    }
          |  ],
          |  "result_info": {
          |    "page": 1,
          |    "per_page": 20,
          |    "total_pages": 1,
          |    "count": 2,
          |    "total_count": 2
          |  },
          |  "success": true,
          |  "errors": [],
          |  "messages": []
          |}
        """.stripMargin

      val listAccountsPage1 =
        """{
          |  "result": [
          |    {
          |      "id": "fake-account-id1",
          |      "name": "Fake Account Org",
          |      "settings":
          |      {
          |        "enforce_twofactor": false
          |      }
          |    }
          |  ],
          |  "result_info": {
          |    "page": 1,
          |    "per_page": 1,
          |    "total_pages": 3,
          |    "count": 1,
          |    "total_count": 3
          |  },
          |  "success": true,
          |  "errors": [],
          |  "messages": []
          |}
        """.stripMargin

      val listAccountsPage2 =
        """{
          |  "result": [
          |    {
          |      "id": "fake-account-id2",
          |      "name": "Fake Account Org 2",
          |      "settings":
          |      {
          |        "enforce_twofactor": false
          |      }
          |    }
          |  ],
          |  "result_info": {
          |    "page": 2,
          |    "per_page": 1,
          |    "total_pages": 3,
          |    "count": 1,
          |    "total_count": 3
          |  },
          |  "success": true,
          |  "errors": [],
          |  "messages": []
          |}
        """.stripMargin

      val listAccountsPage3 =
        """{
          |  "result": [
          |    {
          |      "id": "fake-account-id3",
          |      "name": "Fake Account Org 3",
          |      "settings":
          |      {
          |        "enforce_twofactor": true
          |      }
          |    }
          |  ],
          |  "result_info": {
          |    "page": 3,
          |    "per_page": 1,
          |    "total_pages": 3,
          |    "count": 1,
          |    "total_count": 3
          |  },
          |  "success": true,
          |  "errors": [],
          |  "messages": []
          |}
        """.stripMargin

      val getAccount =
        """{
          |  "result": {
          |    "id": "fake-account-id1",
          |    "name": "Fake Account Org",
          |    "settings": {
          |      "enforce_twofactor": false
          |    }
          |  },
          |  "success": true,
          |  "errors": [],
          |  "messages": []
          |}
        """.stripMargin

      val getRoles =
        """
          |{
          |  "result": [
          |    {
          |      "id": "1111",
          |      "name": "Fake Role 1",
          |      "description": "this is the first fake role",
          |      "permissions":
          |      {
          |        "analytics":
          |        {
          |          "read": true,
          |          "edit": false
          |        }
          |      }
          |    },
          |    {
          |      "id": "2222",
          |      "name": "Fake Role 2",
          |      "description": "second fake role",
          |      "permissions":
          |      {
          |        "zone":
          |        {
          |          "read": true,
          |          "edit": false
          |        },
          |        "logs":
          |        {
          |          "read": true,
          |          "edit": false
          |        }
          |      }
          |    }
          |  ],
          |  "result_info": {
          |    "page": 1,
          |    "per_page": 20,
          |    "total_pages": 1,
          |    "count": 2,
          |    "total_count": 2
          |  },
          |  "success": true,
          |  "errors": [],
          |  "messages": []
          |}
        """.stripMargin

      val getRolesPage1 =
        """
          |{
          |  "result": [
          |    {
          |      "id": "1111",
          |      "name": "Fake Role 1",
          |      "description": "this is the first fake role",
          |      "permissions":
          |      {
          |        "analytics":
          |        {
          |          "read": true,
          |          "edit": false
          |        }
          |      }
          |    }
          |  ],
          |  "result_info": {
          |    "page": 1,
          |    "per_page": 1,
          |    "total_pages": 3,
          |    "count": 1,
          |    "total_count": 3
          |  },
          |  "success": true,
          |  "errors": [],
          |  "messages": []
          |}
        """.stripMargin

      val getRolesPage2 =
        """
          |{
          |  "result": [
          |    {
          |      "id": "2222",
          |      "name": "Fake Role 2",
          |      "description": "second fake role",
          |      "permissions":
          |      {
          |        "zone":
          |        {
          |          "read": true,
          |          "edit": false
          |        },
          |        "logs":
          |        {
          |          "read": true,
          |          "edit": false
          |        }
          |      }
          |    }
          |  ],
          |  "result_info": {
          |    "page": 2,
          |    "per_page": 1,
          |    "total_pages": 3,
          |    "count": 1,
          |    "total_count": 3
          |  },
          |  "success": true,
          |  "errors": [],
          |  "messages": []
          |}
        """.stripMargin

      val getRolesPage3 =
        """
          |{
          |  "result": [
          |    {
          |      "id": "3333",
          |      "name": "Fake Full Role 3",
          |      "description": "full permissions",
          |      "permissions":
          |      {
          |        "legal":
          |        {
          |          "read": true,
          |          "edit": true
          |        },
          |        "billing":
          |        {
          |          "read": true,
          |          "edit": true
          |        }
          |      }
          |    }
          |  ],
          |  "result_info": {
          |    "page": 3,
          |    "per_page": 1,
          |    "total_pages": 3,
          |    "count": 1,
          |    "total_count": 3
          |  },
          |  "success": true,
          |  "errors": [],
          |  "messages": []
          |}
        """.stripMargin

      val accountMember =
        """{
          |  "success": true,
          |  "errors": [],
          |  "messages": [],
          |  "result": {
          |    "id": "fake-account-member-id",
          |    "user":
          |    {
          |      "id": "fake-user-id",
          |      "first_name": null,
          |      "last_name": null,
          |      "email": "myemail@test.com",
          |      "two_factor_authentication_enabled": false
          |    },
          |    "status": "pending",
          |    "roles": [
          |      {
          |        "id": "1111",
          |        "name": "Fake Role 1",
          |        "description": "this is the first fake role",
          |        "permissions":
          |        {
          |          "analytics":
          |          {
          |            "read": true,
          |            "edit": false
          |          }
          |        }
          |      },
          |      {
          |        "id": "2222",
          |        "name": "Fake Role 2",
          |        "description": "second fake role",
          |        "permissions":
          |        {
          |          "zone":
          |          {
          |            "read": true,
          |            "edit": false
          |          },
          |          "logs":
          |          {
          |            "read": true,
          |            "edit": false
          |          }
          |        }
          |      }
          |    ]
          |  }
          |}
        """.stripMargin

      val updatedAccountMember =
        """{
          |  "success": true,
          |  "errors": [],
          |  "messages": [],
          |  "result": {
          |    "id": "fake-account-member-id",
          |    "user":
          |    {
          |      "id": "fake-user-id",
          |      "first_name": "Joe",
          |      "last_name": "Smith",
          |      "email": "myemail@test.com",
          |      "two_factor_authentication_enabled": false
          |    },
          |    "status": "pending",
          |    "roles": [
          |      {
          |        "id": "1111",
          |        "name": "Fake Role 1",
          |        "description": "this is the first fake role",
          |        "permissions":
          |        {
          |          "analytics":
          |          {
          |            "read": true,
          |            "edit": false
          |          }
          |        }
          |      },
          |      {
          |        "id": "2222",
          |        "name": "Fake Role 2",
          |        "description": "second fake role",
          |        "permissions":
          |        {
          |          "zone":
          |          {
          |            "read": true,
          |            "edit": false
          |          },
          |          "logs":
          |          {
          |            "read": true,
          |            "edit": false
          |          }
          |        }
          |      },
          |      {
          |        "id": "3333",
          |        "name": "Fake Role 3",
          |        "description": "third fake role",
          |        "permissions":
          |        {
          |          "crypto":
          |          {
          |            "read": true,
          |            "edit": false
          |          }
          |        }
          |      }
          |    ]
          |  }
          |}
        """.stripMargin

      val removedAccountMember =
        """
          |{
          |  "result": {
          |    "id": "fake-account-member-id"
          |  },
          |  "success": true,
          |  "errors": [],
          |  "messages": []
          |}
        """.stripMargin
    }

    object Failures {
      case class Failure(status: Status, json: String)

      val accountMemberRemovalError = Failure(Status.BadRequest,
        """{
          |  "success": false,
          |  "errors": [
          |    {
          |      "code": 7003,
          |      "message": "Could not route to /accounts/fake-account-id1/members/fake-account-member-id, perhaps your object identifier is invalid?"
          |    },
          |    {
          |      "code": 7000,
          |      "message": "No route for that URI"
          |    }
          |  ],
          |  "messages": [],
          |  "result": null
          |}
        """.stripMargin)

      val accountMemberUpdateError = Failure(Status.BadRequest,
        """{
          |  "success": false,
          |  "errors": [
          |    {
          |      "code": 1001,
          |      "message": "Invalid request: Invalid roles"
          |    }
          |  ],
          |  "messages": [],
          |  "result": null
          |}
        """.stripMargin)

      val accountMemberCreationError = Failure(Status.BadRequest,
        """{
          |  "success": false,
          |  "errors": [
          |    {
          |      "code": 1001,
          |      "message": "Invalid request: Value required for parameter 'email'."
          |    }
          |  ],
          |  "messages": [],
          |  "result": null
          |}
        """.stripMargin)

      val accountMemberDoesNotExist = Failure(Status.NotFound,
        """
          |{
          |  "success": false,
          |  "errors": [
          |    {
          |      "code": 1003,
          |      "message": "Member not found for account"
          |     }
          |  ],
          |  "messages": [],
          |  "result": null
          |}
        """.stripMargin)

      val accountDoesNotExist = Failure(Status.NotFound,
        """{
          |  "success": false,
          |  "errors": [
          |    {
          |      "code": 1003,
          |      "message": "Account not found"
          |    }
          |  ],
          |  "messages": [],
          |  "result": null
          |}
        """.stripMargin)
    }
  }
}
