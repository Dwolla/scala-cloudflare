package com.dwolla.cloudflare

import com.dwolla.cloudflare.common.JsonEntity._
import com.dwolla.cloudflare.domain.dto.accounts.{AccountDTO, AccountMemberDTO, AccountRoleDTO, NewAccountMemberDTO}
import com.dwolla.cloudflare.domain.model.accounts.Implicits._
import com.dwolla.cloudflare.domain.model.accounts.{Account, AccountMember, AccountRole}
import org.apache.http.client.methods.{HttpGet, HttpPost}
import org.json4s.native._
import org.json4s.{DefaultFormats, Formats}

import scala.concurrent.{ExecutionContext, Future}

class AccountsClient(executor: CloudflareApiExecutor)(implicit val ec: ExecutionContext) {
  protected implicit val formats: Formats = DefaultFormats

  def listAccounts(): Future[Set[Account]] = {
    val request: HttpGet = new HttpGet(s"https://api.cloudflare.com/client/v4/accounts?direction=asc")

    executor.fetch(request) { response ⇒
      (parseJson(response.getEntity.getContent) \ "result").extract[Set[AccountDTO]].map(toModel)
    }
  }

  def getById(accountId: String): Future[Option[Account]] = {
    val request: HttpGet = new HttpGet(s"https://api.cloudflare.com/client/v4/accounts/$accountId")

    executor.fetch(request) { response ⇒
      (parseJson(response.getEntity.getContent) \ "result").extract[Option[AccountDTO]].map(toModel)
    }
  }

  def getByName(name: String): Future[Option[Account]] = {
    listAccounts() map (accounts ⇒ {
      accounts.find(a ⇒ a.name.toUpperCase == name.toUpperCase)
    })
  }

  def getRolesForAccount(accountId: String): Future[Set[AccountRole]] = {
    val request: HttpGet = new HttpGet(s"https://api.cloudflare.com/client/v4/accounts/$accountId/roles")

    executor.fetch(request) { response ⇒
      (parseJson(response.getEntity.getContent) \ "result").extract[Set[AccountRoleDTO]].map(toModel)
    }
  }

  def addMemberToAccount(accountId: String, emailAddress: String, roleIds: List[String], status: String = "pending"): Future[AccountMember] = {
    val request: HttpPost = new HttpPost(s"https://api.cloudflare.com/client/v4/accounts/$accountId/members")
    request.setEntity(NewAccountMemberDTO(emailAddress, roleIds, Some(status)))

    executor.fetch(request) { response ⇒
      (parseJson(response.getEntity.getContent) \ "result").extract[AccountMemberDTO]
    }
  }
}
