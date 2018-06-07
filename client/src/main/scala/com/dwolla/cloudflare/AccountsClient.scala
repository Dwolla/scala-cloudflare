package com.dwolla.cloudflare

import com.dwolla.cloudflare.common.JsonEntity._
import com.dwolla.cloudflare.domain.dto.accounts.{AccountDTO, AccountMemberDTO, AccountRoleDTO, NewAccountMemberDTO}
import com.dwolla.cloudflare.domain.model.accounts.Implicits._
import com.dwolla.cloudflare.domain.model.accounts.{Account, AccountMember, AccountRole}
import org.apache.http.client.methods.{HttpGet, HttpPost}
import org.json4s.native._
import org.json4s.{DefaultFormats, Formats}
import cats._
import cats.implicits._

import scala.language.higherKinds

class AccountsClient[F[_] : Monad](executor: CloudflareApiExecutor[F]) {
  protected implicit val formats: Formats = DefaultFormats

  def listAccounts(): F[Set[Account]] = {
    val request: HttpGet = new HttpGet(s"https://api.cloudflare.com/client/v4/accounts?direction=asc")

    executor.fetch(request) { response ⇒
      (parseJson(response.getEntity.getContent) \ "result").extract[Set[AccountDTO]].map(toModel)
    }
  }

  def getById(accountId: String): F[Option[Account]] = {
    val request: HttpGet = new HttpGet(s"https://api.cloudflare.com/client/v4/accounts/$accountId")

    executor.fetch(request) { response ⇒
      (parseJson(response.getEntity.getContent) \ "result").extract[Option[AccountDTO]].map(toModel)
    }
  }

  def getByName(name: String): F[Option[Account]] = {
    listAccounts() map (accounts ⇒ {
      accounts.find(a ⇒ a.name.toUpperCase == name.toUpperCase)
    })
  }

  def getRolesForAccount(accountId: String): F[Set[AccountRole]] = {
    val request: HttpGet = new HttpGet(s"https://api.cloudflare.com/client/v4/accounts/$accountId/roles")

    executor.fetch(request) { response ⇒
      (parseJson(response.getEntity.getContent) \ "result").extract[Set[AccountRoleDTO]].map(toModel)
    }
  }

  def addMemberToAccount(accountId: String, emailAddress: String, roleIds: List[String], status: String = "pending"): F[AccountMember] = {
    val request: HttpPost = new HttpPost(s"https://api.cloudflare.com/client/v4/accounts/$accountId/members")
    request.setEntity(NewAccountMemberDTO(emailAddress, roleIds, Some(status)))

    executor.fetch(request) { response ⇒
      (parseJson(response.getEntity.getContent) \ "result").extract[AccountMemberDTO]
    }
  }
}
