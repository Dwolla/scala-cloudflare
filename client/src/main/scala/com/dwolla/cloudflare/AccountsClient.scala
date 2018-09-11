package com.dwolla.cloudflare

import cats.effect._
import cats.implicits._
import com.dwolla.cloudflare.AccountsClientImpl._
import com.dwolla.cloudflare.domain.dto.accounts._
import com.dwolla.cloudflare.domain.model.Exceptions.UnexpectedCloudflareErrorException
import com.dwolla.cloudflare.domain.model.accounts.Implicits._
import com.dwolla.cloudflare.domain.model.accounts._
import com.dwolla.cloudflare.domain.model.{Implicits ⇒ _, _}
import io.circe.Json
import io.circe.generic.auto._
import io.circe.optics.JsonPath._
import io.circe.syntax._
import fs2._
import org.http4s.Method._
import org.http4s._
import org.http4s.circe._
import org.http4s.client.dsl.Http4sClientDsl

import scala.language.higherKinds

trait AccountsClient[F[_]] {
  def list(): Stream[F, Account]
  def getById(accountId: String): Stream[F, Account]
  def getByName(name: String): Stream[F, Account]
  def listRoles(accountId: AccountId): Stream[F, AccountRole]
  def getMember(accountId: AccountId, accountMemberId: String): Stream[F, AccountMember]
  def addMember(accountId: AccountId, emailAddress: String, roleIds: List[String]): Stream[F, AccountMember]
  def updateMember(accountId: AccountId, accountMember: AccountMember): Stream[F, AccountMember]
  def removeMember(accountId: AccountId, accountMemberId: String): Stream[F, AccountMemberId]
}

object AccountsClient {
  def apply[F[_] : Sync](executor: StreamingCloudflareApiExecutor[F]): AccountsClient[F] = new AccountsClientImpl[F](executor)
}

object AccountsClientImpl {
  val notFoundCodes = List(1003)
}

class AccountsClientImpl[F[_]: Sync](executor: StreamingCloudflareApiExecutor[F]) extends AccountsClient[F] with Http4sClientDsl[F] {
  override def list(): Stream[F, Account] = {
    for {
      req ← Stream.eval(GET(BaseUrl / "accounts" withQueryParam("direction", "asc")))
      record ← executor.fetch[AccountDTO](req)
    } yield record
  }

  override def getById(accountId: String): Stream[F, Account] =
    for {
      req ← Stream.eval(GET(BaseUrl / "accounts" / accountId))
      res ← executor.fetch[AccountDTO](req).returningEmptyOnErrorCodes(notFoundCodes: _*)
    } yield res

  override def getByName(name: String): Stream[F, Account] = {
    list()
      .filter(a ⇒ a.name.toUpperCase == name.toUpperCase)
  }

  override def listRoles(accountId: AccountId): Stream[F, AccountRole] = {
    for {
      req ← Stream.eval(GET(BaseUrl / "accounts" / accountId / "roles"))
      record ← executor.fetch[AccountRoleDTO](req)
    } yield record
  }

  override def getMember(accountId: AccountId, accountMemberId: String): Stream[F, AccountMember] =
    for {
      req ← Stream.eval(GET(buildAccountMemberUri(accountId, accountMemberId)))
      res ← executor.fetch[AccountMemberDTO](req).returningEmptyOnErrorCodes(notFoundCodes: _*)
    } yield res

  override def addMember(accountId: AccountId, emailAddress: String, roleIds: List[String]): Stream[F, AccountMember] =
    for {
      req ← Stream.eval(POST(BaseUrl / "accounts" / accountId / "members", NewAccountMemberDTO(emailAddress, roleIds, Some("pending")).asJson))
      resp ← createOrUpdate(req)
    } yield resp

  override def updateMember(accountId: AccountId, accountMember: AccountMember): Stream[F, AccountMember] = {
    for {
      req ← Stream.eval(PUT(buildAccountMemberUri(accountId, accountMember.id), toDto(accountMember).asJson))
      resp ← createOrUpdate(req)
    } yield resp
  }

  override def removeMember(accountId: AccountId, accountMemberId: String): Stream[F, AccountMemberId] =
  /*_*/
    for {
      req ← Stream.eval(DELETE(buildAccountMemberUri(accountId, accountMemberId)))
      json ← executor.fetch[Json](req).last.adaptError {
        case ex: UnexpectedCloudflareErrorException if ex.errors.flatMap(_.code.toSeq).exists(notFoundCodes.contains) ⇒
          AccountMemberDoesNotExistException(accountId, accountMemberId)
      }
    } yield tagAccountMemberId(json.flatMap(deletedRecordLens).getOrElse(accountMemberId))
  /*_*/

  private def buildAccountMemberUri(accountId: AccountId, accountMemberId: String): Uri =
    BaseUrl / "accounts" / accountId / "members" / accountMemberId

  private def createOrUpdate(request: Request[F]): Stream[F, AccountMember] =
    for {
      res ← executor.fetch[AccountMemberDTO](request)
    } yield res

  private val deletedRecordLens: Json ⇒ Option[String] = root.id.string.getOption
}

case class AccountMemberDoesNotExistException(accountId: AccountId, accountMemberId: String) extends RuntimeException(
  s"The account member $accountMemberId not found for account $accountId."
)
