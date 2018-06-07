package dwolla.cloudflare

import java.net.URI

import cats.implicits._
import com.dwolla.cloudflare.domain.model.accounts._
import com.dwolla.cloudflare.{AccountsClient, CloudflareAuthorization, _}
import dwolla.cloudflare.SampleAccountsResponses.Failures
import dwolla.testutils.httpclient.SimpleHttpRequestMatcher.http
import org.apache.http.HttpVersion.HTTP_1_1
import org.apache.http.client.HttpClient
import org.apache.http.client.methods._
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.message._
import org.apache.http._
import org.json4s.DefaultFormats
import org.specs2.concurrent.ExecutionEnv
import org.specs2.matcher.JsonMatchers
import org.specs2.mock.Mockito
import org.specs2.mock.mockito.ArgumentCapture
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

import scala.concurrent._
import scala.reflect.ClassTag

class AccountsClientSpec(implicit ee: ExecutionEnv) extends Specification with Mockito with JsonMatchers {
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
      mockListAccounts(SampleAccountsResponses.Successes.listAccounts)

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

      mockListAccounts(SampleAccountsResponses.Successes.listAccounts)

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
      mockListAccounts(SampleAccountsResponses.Successes.listAccounts)

      val output: Future[Option[Account]] = client.getByName("Test Stuff")
      output must beNone.await
    }
  }

  "getById" should {
    "get account by id" in new Setup {
      val accountId = "fake-account-id1"

      mockGetAccountById(accountId, SampleAccountsResponses.Successes.getAccount)

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

      val failure: Failures.Failure = SampleAccountsResponses.Failures.accountDoesNotExist
      val captor: ArgumentCapture[HttpGet] = mockExecuteWithCaptor[HttpGet](fakeResponse(new BasicStatusLine(HTTP_1_1, failure.statusCode, "Not Found"), new StringEntity(failure.json)))

      val output: Future[Option[Account]] = client.getById(accountId)
      output must beNone.await

      val httpRequest: HttpGet = captor.value
      httpRequest.getMethod must_== "GET"
      httpRequest.getURI must_== new URI(s"https://api.cloudflare.com/client/v4/accounts/$accountId")
    }
  }

  "getRolesForAccount" should {
    "return all roles for an account" in new Setup {
      val accountId = "fake-account-id"

      val captor = mockExecuteWithCaptor[HttpGet](fakeResponse(new BasicStatusLine(HTTP_1_1, 200, "Ok"), new StringEntity(SampleAccountsResponses.Successes.getRoles)))

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

  "addMemberToAccount" should {
    "add new member" in new Setup {
      val accountId = "fake-account-id1"
      val email = "me@abc123test.com"
      val roleIds = List("1111", "2222")

      val captor: ArgumentCapture[HttpPost] = mockExecuteWithCaptor[HttpPost](fakeResponse(new BasicStatusLine(HTTP_1_1, 200, "Ok"), new StringEntity(SampleAccountsResponses.Successes.createAccountMember)))

      val output: Future[AccountMember] = client.addMemberToAccount(accountId, email, roleIds)
      output must be_==(
        AccountMember(
          id = "fake-account-member-id",
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

      val httpRequest: HttpPost = captor.value
      httpRequest.getMethod must_== "POST"
      httpRequest.getURI must_== new URI(s"https://api.cloudflare.com/client/v4/accounts/$accountId/members")
    }
  }

  def mockExecuteWithCaptor[T <: HttpUriRequest : ClassTag](response: HttpResponse)(implicit mockHttpClient: HttpClient): ArgumentCapture[T] = {
    val captor = capture[T]
    mockHttpClient.execute(captor) returns response

    captor
  }

  def mockListAccounts(responseBody: String)(implicit mockHttpClient: HttpClient): Unit = {
    val response = fakeResponse(new BasicStatusLine(HTTP_1_1, 200, "Ok"), new StringEntity(responseBody))
    mockHttpClient.execute(http(new HttpGet(s"https://api.cloudflare.com/client/v4/accounts?direction=asc"))) returns response
  }

  def mockGetAccountById(accountId: String, responseBody: String)(implicit mockHttpClient: HttpClient): Unit = {
    val response = fakeResponse(new BasicStatusLine(HTTP_1_1, 200, "Ok"), new StringEntity(responseBody))
    mockHttpClient.execute(http(new HttpGet(s"https://api.cloudflare.com/client/v4/accounts/$accountId"))) returns response
  }

  def fakeResponse(statusLine: StatusLine, entity: HttpEntity) = {
    val res = new BasicHttpResponse(statusLine) with CloseableHttpResponse {
      val promisedClose = Promise[Unit]

      override def close(): Unit = promisedClose.success(Unit)

      def isClosed: Boolean = promisedClose.isCompleted
    }

    res.setEntity(entity)

    res
  }
}

private object SampleAccountsResponses {
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

    val createAccountMember =
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
  }

  object Failures {
    case class Failure(statusCode: Int, json: String)

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
