package dwolla.cloudflare

import java.net.URI

import cats.implicits._
import com.dwolla.cloudflare.domain.model.Exceptions.UnexpectedCloudflareErrorException
import com.dwolla.cloudflare.domain.model.accounts._
import com.dwolla.cloudflare.{AccountMemberDoesNotExistException, AccountsClient, CloudflareAuthorization, _}
import dwolla.testutils.httpclient.SimpleHttpRequestMatcher.http
import org.apache.http.HttpVersion.HTTP_1_1
import org.apache.http.client.HttpClient
import org.apache.http.client.methods._
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.message.BasicStatusLine
import org.json4s.DefaultFormats
import org.specs2.concurrent.ExecutionEnv
import org.specs2.matcher.{JsonMatchers, JsonType, Matcher}
import org.specs2.mock.Mockito
import org.specs2.mock.mockito.ArgumentCapture
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

import scala.concurrent.Future
import scala.io.Source

class AccountsClientSpec(implicit ee: ExecutionEnv) extends Specification with Mockito with JsonMatchers with HttpClientHelper {
  trait Setup extends Scope {
    implicit val formats = DefaultFormats
    implicit val mockHttpClient = mock[CloseableHttpClient]
    val fakeExecutor = new FutureCloudflareApiExecutor(CloudflareAuthorization("email", "key")) {
      override lazy val httpClient: CloseableHttpClient = mockHttpClient
    }

    val client = new AccountsClient(fakeExecutor)
  }

  "listAccounts" should {
    "return accounts ordered asc" in new Setup {
      mockListAccounts(SampleResponses.Successes.listAccounts)

      val output: Future[Set[Account]] = client.listAccounts()
      output must be_==(Set(
        Account(
          id = "fake-account-id1",
          name = "Fake Account Org",
          settings = AccountSettings(enforceTwoFactor = false)
        ),
        Account(
          id = "fake-account-id2",
          name = "Another Fake Account Biz",
          settings = AccountSettings(enforceTwoFactor = true)
        )
      )).await
    }
  }

  "getByName" should {
    "get account by name" in new Setup {
      val accountName = "Another Fake Account Biz"

      mockListAccounts(SampleResponses.Successes.listAccounts)

      val output: Future[Option[Account]] = client.getByName(accountName)
      output must beSome(
        Account(
          id = "fake-account-id2",
          name = accountName,
          settings = AccountSettings(enforceTwoFactor = true)
        )
      ).await
    }

    "return None if not found" in new Setup {
      mockListAccounts(SampleResponses.Successes.listAccounts)

      val output: Future[Option[Account]] = client.getByName("Test Stuff")
      output must beNone.await
    }
  }

  "getById" should {
    "get account by id" in new Setup {
      val accountId = "fake-account-id1"

      mockGetAccountById(accountId, SampleResponses.Successes.getAccount)

      val output: Future[Option[Account]] = client.getById(accountId)
      output must beSome(
        Account(
          id = accountId,
          name = "Fake Account Org",
          settings = AccountSettings(enforceTwoFactor = false)
        )
      ).await
    }

    "return None if not found" in new Setup {
      val accountId = "missing-id"

      val failure = SampleResponses.Failures.accountDoesNotExist
      val captor: ArgumentCapture[HttpGet] = mockExecuteWithCaptor[HttpGet](fakeResponse(new BasicStatusLine(HTTP_1_1, failure.statusCode, "Not Found"), new StringEntity(failure.json)))

      val output: Future[Option[Account]] = client.getById(accountId)
      output must beNone.await

      val httpGet: HttpGet = captor.value
      httpGet.getMethod must_== "GET"
      httpGet.getURI must_== new URI(s"https://api.cloudflare.com/client/v4/accounts/$accountId")
    }
  }

  "getRolesForAccount" should {
    "return all roles for an account" in new Setup {
      val accountId = "fake-account-id"

      val captor = mockExecuteWithCaptor[HttpGet](fakeResponse(new BasicStatusLine(HTTP_1_1, 200, "Ok"), new StringEntity(SampleResponses.Successes.getRoles)))

      val output: Future[Set[AccountRole]] = client.getRolesForAccount(accountId)
      output must be_==(Set(
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
      )).await
    }
  }

  "getAccountMember" should {
    "get account member by id and account id" in new Setup {
      val accountId = "fake-account-id1"
      val accountMemberId = "fake-account-member-id"

      mockGetAccountMember(accountId, accountMemberId, SampleResponses.Successes.accountMember)

      val output: Future[Option[AccountMember]] = client.getAccountMember(accountId, accountMemberId)
      output must beSome(
        AccountMember(
          id = accountMemberId,
          user = User(
            id = "fake-user-id",
            firstName = null,
            lastName = null,
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
      val accountId = "fake-account-id1"
      val accountMemberId = "missing-account-member-id"

      val failure = SampleResponses.Failures.accountMemberDoesNotExist
      val captor: ArgumentCapture[HttpGet] = mockExecuteWithCaptor[HttpGet](fakeResponse(new BasicStatusLine(HTTP_1_1, failure.statusCode, "Not Found"), new StringEntity(failure.json)))

      val output: Future[Option[AccountMember]] = client.getAccountMember(accountId, accountMemberId)
      output must beNone.await

      val httpGet: HttpGet = captor.value
      httpGet.getMethod must_== "GET"
      httpGet.getURI must_== new URI(s"https://api.cloudflare.com/client/v4/accounts/$accountId/members/$accountMemberId")
    }
  }

  "addMemberToAccount" should {
    "add new member" in new Setup {
      val accountId = "fake-account-id1"
      val accountMemberId = "fake-account-member-id"
      val email = "myemail@test.com"
      val roleIds = List("1111", "2222")

      val captor: ArgumentCapture[HttpPost] = mockExecuteWithCaptor[HttpPost](fakeResponse(new BasicStatusLine(HTTP_1_1, 200, "Ok"), new StringEntity(SampleResponses.Successes.accountMember)))

      val output: Future[AccountMember] = client.addMemberToAccount(accountId, email, roleIds)
      output must be_==(
        AccountMember(
          id = accountMemberId,
          user = User(
            id = "fake-user-id",
            firstName = null,
            lastName = null,
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

      val httpPost: HttpPost = captor.value
      httpPost.getMethod must_== "POST"
      httpPost.getURI must_== new URI(s"https://api.cloudflare.com/client/v4/accounts/$accountId/members")

      private val httpEntity = httpPost.getEntity

      httpEntity.getContentType.getValue must_== "application/json"
      val postedJson: String = Source.fromInputStream(httpEntity.getContent).mkString

      postedJson must /("email" → email)
      postedJson must /("status" → "pending")
      postedJson must /("roles").andHave(exactly("1111", "2222"))
    }

    "throw unexpected exception if error adding new member" in new Setup {
      val accountId = "fake-account-id1"
      val email = "me@abc123test.com"
      val roleIds = List("1111", "2222")

      val failure = SampleResponses.Failures.accountMemberCreationError
      val captor: ArgumentCapture[HttpPost] = mockExecuteWithCaptor[HttpPost](fakeResponse(new BasicStatusLine(HTTP_1_1, failure.statusCode, "Bad Request"), new StringEntity(failure.json)))

      client.addMemberToAccount(accountId, email, roleIds) must throwA[UnexpectedCloudflareErrorException].like {
        case ex ⇒ ex.getMessage must_==
          """An unexpected Cloudflare error occurred. Errors:
            |
            | - Error(1001,Invalid request: Value required for parameter 'email'.)
            |     """.stripMargin
      }.await
    }
  }

  "updateAccountMember" should {
    "update existing member" in new Setup {
      val email = "myemail@test.com"
      val accountId = "fake-account-id1"
      val accountMemberId = "fake-account-member-id"

      val updatedAccountMember = AccountMember(
        id = accountMemberId,
        user = User(
          id = "fake-user-id",
          firstName = null,
          lastName = null,
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

      val captor: ArgumentCapture[HttpPut] = mockExecuteWithCaptor[HttpPut](fakeResponse(new BasicStatusLine(HTTP_1_1, 200, "Ok"), new StringEntity(SampleResponses.Successes.updatedAccountMember)))

      val output: Future[AccountMember] = client.updateAccountMember(accountId, updatedAccountMember)
      output must be_==(updatedAccountMember).await

      val httpPut: HttpPut = captor.value
      httpPut.getMethod must_== "PUT"
      httpPut.getURI must_== new URI(s"https://api.cloudflare.com/client/v4/accounts/$accountId/members/$accountMemberId")

      private val httpEntity = httpPut.getEntity

      httpEntity.getContentType.getValue must_== "application/json"
      val putJson: String = Source.fromInputStream(httpEntity.getContent).mkString

      putJson must / ("user") /("email" → email)
      putJson must / ("status" → "pending")

      putJson must haveRoles(
        aRoleWith(id = "1111", name = "Fake Role 1", description = "this is the first fake role"),
        aRoleWith(id = "2222", name = "Fake Role 2", description = "second fake role"),
        aRoleWith(id = "3333", name = "Fake Role 3", description = "third fake role")
      )
    }

    "throw unexpected exception if error updating existing member" in new Setup {
      val accountId = "fake-account-id1"
      val accountMemberId = "fake-account-member-id"

      val updatedAccountMember = AccountMember(
        id = accountMemberId,
        user = User(
          id = "fake-user-id",
          firstName = null,
          lastName = null,
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
      val captor: ArgumentCapture[HttpPut] = mockExecuteWithCaptor[HttpPut](fakeResponse(new BasicStatusLine(HTTP_1_1, failure.statusCode, "Bad Request"), new StringEntity(failure.json)))

      client.updateAccountMember(accountId, updatedAccountMember) must throwA[UnexpectedCloudflareErrorException].like {
        case ex ⇒ ex.getMessage must_==
          """An unexpected Cloudflare error occurred. Errors:
            |
            | - Error(1001,Invalid request: Invalid roles)
            |     """.stripMargin
      }.await
    }
  }

  "removeAccountMember" should {
    "remove member from account" in new Setup {
      val accountId = "fake-account-id1"
      val accountMemberId = "fake-account-member-id"

      val captor: ArgumentCapture[HttpDelete] = mockExecuteWithCaptor[HttpDelete](fakeResponse(new BasicStatusLine(HTTP_1_1, 200, "Ok"), new StringEntity(SampleResponses.Successes.removedAccountMember)))

      val output: Future[String] = client.removeAccountMember(accountId, accountMemberId)
      output must be_==(
        accountMemberId
      ).await

      val httpDelete: HttpDelete = captor.value
      httpDelete.getMethod must_== "DELETE"
      httpDelete.getURI must_== new URI(s"https://api.cloudflare.com/client/v4/accounts/$accountId/members/$accountMemberId")
    }

    "throw unexpected exception if error removing member" in new Setup {
      val accountId = "fake-account-id1"
      val accountMemberId = "fake-account-member-id"

      val failure = SampleResponses.Failures.accountMemberRemovalError
      val captor: ArgumentCapture[HttpDelete] = mockExecuteWithCaptor[HttpDelete](fakeResponse(new BasicStatusLine(HTTP_1_1, failure.statusCode, "Bad Request"), new StringEntity(failure.json)))

      client.removeAccountMember(accountId, accountMemberId) must throwA[UnexpectedCloudflareErrorException].like {
        case ex ⇒ ex.getMessage must_==
          """An unexpected Cloudflare error occurred. Errors:
            |
            | - Error(7003,Could not route to /accounts/fake-account-id1/members/fake-account-member-id, perhaps your object identifier is invalid?)
            | - Error(7000,No route for that URI)
            |     """.stripMargin
      }.await
    }

    "throw not found exception if member not in account" in new Setup {
      val accountId = "fake-account-id1"
      val accountMemberId = "fake-account-member-id"

      val failure = SampleResponses.Failures.accountDoesNotExist
      val captor: ArgumentCapture[HttpDelete] = mockExecuteWithCaptor[HttpDelete](fakeResponse(new BasicStatusLine(HTTP_1_1, failure.statusCode, "Not Found"), new StringEntity(failure.json)))

      client.removeAccountMember(accountId, accountMemberId) must throwA[AccountMemberDoesNotExistException].like {
        case ex ⇒ ex.getMessage must_==
          "The account member fake-account-member-id not found for account fake-account-id1."
      }.await
    }
  }

  def mockListAccounts(responseBody: String)(implicit mockHttpClient: HttpClient): Unit = {
    val response = fakeResponse(new BasicStatusLine(HTTP_1_1, 200, "Ok"), new StringEntity(responseBody))
    mockHttpClient.execute(http(new HttpGet(s"https://api.cloudflare.com/client/v4/accounts?direction=asc"))) returns response
  }

  def mockGetAccountById(accountId: String, responseBody: String)(implicit mockHttpClient: HttpClient): Unit = {
    val response = fakeResponse(new BasicStatusLine(HTTP_1_1, 200, "Ok"), new StringEntity(responseBody))
    mockHttpClient.execute(http(new HttpGet(s"https://api.cloudflare.com/client/v4/accounts/$accountId"))) returns response
  }

  def mockGetAccountMember(accountId: String, accountMemberId: String, responseBody: String)(implicit mockHttpClient: HttpClient): Unit = {
    val response = fakeResponse(new BasicStatusLine(HTTP_1_1, 200, "Ok"), new StringEntity(responseBody))
    mockHttpClient.execute(http(new HttpGet(s"https://api.cloudflare.com/client/v4/accounts/$accountId/members/$accountMemberId"))) returns response
  }

  def aRoleWith(id: Matcher[JsonType], name: Matcher[JsonType],  description: Matcher[JsonType]): Matcher[String] =
    /("id").andHave(id) and /("name").andHave(name) and /("description").andHave(description)

  def haveRoles(roles: Matcher[String]*): Matcher[String] =
    /("roles").andHave(allOf(roles:_*))

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
          |    },
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
          |    "page": 1,
          |    "per_page": 20,
          |    "total_pages": 1,
          |    "count": 3,
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
      case class Failure(statusCode: Int, json: String)

      val accountMemberRemovalError = Failure(400,
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

      val accountMemberUpdateError = Failure(400,
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

      val accountMemberCreationError = Failure(400,
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

      val accountMemberDoesNotExist = Failure(404,
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

      val accountDoesNotExist = Failure(404,
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