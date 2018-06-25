package com.dwolla.cloudflare.clients

import cats._
import cats.implicits._
import com.dwolla.cloudflare.CloudflareApiExecutor
import com.dwolla.cloudflare.common.JsonEntity._
import com.dwolla.cloudflare.common.UriHelper
import com.dwolla.cloudflare.domain.dto.accounts.{AccountDTO, AccountMemberDTO, AccountRoleDTO, NewAccountMemberDTO}
import com.dwolla.cloudflare.domain.dto.{PagedResponseDTO, ResponseDTO}
import com.dwolla.cloudflare.domain.model.DeletedRecord
import com.dwolla.cloudflare.domain.model.Exceptions.UnexpectedCloudflareErrorException
import com.dwolla.cloudflare.domain.model.accounts.Implicits._
import com.dwolla.cloudflare.domain.model.accounts.{Account, AccountMember, AccountRole}
import com.dwolla.cloudflare.domain.model.response.Implicits._
import com.dwolla.cloudflare.domain.model.response.PagedResponse
import org.apache.http.client.methods._
import org.json4s.native._
import org.json4s.{DefaultFormats, Formats}

import scala.language.higherKinds

class AccountsClient[F[_] : Monad](executor: CloudflareApiExecutor[F]) {
  protected implicit val formats: Formats = DefaultFormats

  def list(page: Int = 1, perPage: Int = 25): F[PagedResponse[Set[Account]]] = {
    val parameters = UriHelper.buildParameterString(Seq(Option("page" → page), Option("per_page" → perPage), Option("direction" → "asc")))
    val request: HttpGet = new HttpGet(UriHelper.buildApiUri("accounts", Some(parameters)))

    executor.fetch(request) { response ⇒
      parseJson(response.getEntity.getContent).extract[PagedResponseDTO[Set[AccountDTO]]]
    }
  }

  def listAll(): F[Set[Account]] = {
    list(1).flatMap { pagedResponse ⇒
      val totalPages = pagedResponse.paging.totalPages
      val firstAccounts = pagedResponse.result

      (2 to totalPages).toList.traverse(page ⇒ list(page))
        .map(responses ⇒ responses.map(_.result))
        .map(accounts ⇒ accounts.foldLeft(firstAccounts) {
          (accumulated, a) ⇒ accumulated ++ a
        })
    }
  }

  def getById(accountId: String): F[Option[Account]] = {
    val request: HttpGet = new HttpGet(UriHelper.buildApiUri(s"accounts/$accountId"))

    executor.fetch(request) { response ⇒
      parseJson(response.getEntity.getContent).extract[ResponseDTO[Option[AccountDTO]]].result.map(toModel)
    }
  }

  def getByName(name: String): F[Option[Account]] = {
    listAll().map(all => all.find(a ⇒ a.name.toUpperCase == name.toUpperCase))
  }

  def getRoles(accountId: String, page: Int = 1, perPage: Int = 25): F[PagedResponse[Set[AccountRole]]] = {
    val parameters = UriHelper.buildParameterString(Seq(Option("page" → page), Option("per_page" → perPage)))
    val request: HttpGet = new HttpGet(UriHelper.buildApiUri(s"accounts/$accountId/roles", Some(parameters)))

    executor.fetch(request) { response ⇒
      parseJson(response.getEntity.getContent).extract[PagedResponseDTO[Set[AccountRoleDTO]]]
    }
  }

  def getMember(accountId: String, accountMemberId: String): F[Option[AccountMember]] = {
    val request: HttpGet = new HttpGet(buildAccountMemberUri(accountId, accountMemberId))

    executor.fetch(request) { response ⇒
      parseJson(response.getEntity.getContent).extract[ResponseDTO[Option[AccountMemberDTO]]].result.map(toModel)
    }
  }

  def addMember(accountId: String, emailAddress: String, roleIds: List[String], status: String = "pending"): F[AccountMember] = {
    val request: HttpPost = new HttpPost(UriHelper.buildApiUri(s"accounts/$accountId/members"))
    request.setEntity(NewAccountMemberDTO(emailAddress, roleIds, Some(status)))

    createOrUpdate(request)
  }

  def updateMember(accountId: String, accountMember: AccountMember): F[AccountMember] = {
    val request: HttpPut = new HttpPut(buildAccountMemberUri(accountId, accountMember.id))
    request.setEntity(accountMember)

    createOrUpdate(request)
  }

  def removeMember(accountId: String, accountMemberId: String): F[String] = {
    val request: HttpDelete = new HttpDelete(buildAccountMemberUri(accountId, accountMemberId))
    executor.fetch(request) { response ⇒
      val r = parseJson(response.getEntity.getContent).extract[ResponseDTO[DeletedRecord]]

      response.getStatusLine.getStatusCode match {
        case 200 ⇒
          r.result.id
        case 404 ⇒
          throw AccountMemberDoesNotExistException(accountId, accountMemberId)
        case _ ⇒
          throw UnexpectedCloudflareErrorException(r.errors.get)
      }
    }
  }

  private def createOrUpdate(request: HttpRequestBase): F[AccountMember] = {
    executor.fetch(request) { response ⇒
      val r = parseJson(response.getEntity.getContent).extract[ResponseDTO[AccountMemberDTO]]

      response.getStatusLine.getStatusCode match {
        case 200 ⇒
          r.result
        case _ ⇒
          throw UnexpectedCloudflareErrorException(r.errors.get)
      }
    }
  }

  private def buildAccountMemberUri(accountId: String, accountMemberId: String): String = {
    UriHelper.buildApiUri(s"accounts/$accountId/members/$accountMemberId")
  }
}

case class AccountMemberDoesNotExistException(accountId: String, accountMemberId: String) extends RuntimeException(
  s"The account member $accountMemberId not found for account $accountId."
)
