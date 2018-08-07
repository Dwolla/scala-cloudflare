package com.dwolla.cloudflare

import cats._
import cats.effect._
import cats.implicits._
import com.dwolla.cloudflare.domain.dto.accounts._
import com.dwolla.cloudflare.domain.model
import com.dwolla.cloudflare.domain.model.Error
import com.dwolla.cloudflare.domain.model.Exceptions.UnexpectedCloudflareErrorException
import com.dwolla.cloudflare.domain.model.accounts.Implicits._
import com.dwolla.cloudflare.domain.model.accounts._
import io.circe._
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
  def listRoles(accountId: String): Stream[F, AccountRole]
  def getMember(accountId: String, accountMemberId: String): Stream[F, AccountMember]
  def addMember(accountId: String, emailAddress: String, roleIds: List[String]): Stream[F, AccountMember]
  def updateMember(accountId: String, accountMember: AccountMember): Stream[F, AccountMember]
  def removeMember(accountId: String, accountMemberId: String): Stream[F, String]
}

object AccountsClient {
  def apply[F[_] : Sync](executor: StreamingCloudflareApiExecutor[F]): AccountsClient[F] = new AccountsClientImpl[F](executor)
}

class AccountsClientImpl[F[_]: Sync](executor: StreamingCloudflareApiExecutor[F]) extends AccountsClient[F] with Http4sClientDsl[F] {
  def list(): Stream[F, Account] = {
    for {
      req ← Stream.eval(GET(BaseUrl / "accounts" withQueryParam("direction", "asc")))
      record ← executor.fetch[AccountDTO](req)
    } yield record
  }

  def getById(accountId: String): Stream[F, Account] =
    for {
      req ← Stream.eval(GET(BaseUrl / "accounts" / accountId))
      res ← executor.fetch[AccountDTO](req)
    } yield res

  def getByName(name: String): Stream[F, Account] = {
    list()
      .filter(a ⇒ a.name.toUpperCase == name.toUpperCase)
  }

  def listRoles(accountId: String): Stream[F, AccountRole] = {
    for {
      req ← Stream.eval(GET(BaseUrl / "accounts" / accountId / "roles"))
      record ← executor.fetch[AccountRoleDTO](req)
    } yield record
  }

  def getMember(accountId: String, accountMemberId: String): Stream[F, AccountMember] =
    for {
      req ← Stream.eval(GET(buildAccountMemberUri(accountId, accountMemberId)))
      res ← executor.fetch[AccountMemberDTO](req)
    } yield res

  def addMember(accountId: String, emailAddress: String, roleIds: List[String]): Stream[F, AccountMember] = {
    for {
      dto ← Stream.eval(Applicative[F].pure(NewAccountMemberDTO(emailAddress, roleIds, Some("pending"))))
      req ← Stream.eval(POST(BaseUrl / "accounts" / accountId / "members", dto.asJson))
      resp ← createOrUpdate(req)
    } yield resp
  }

  def updateMember(accountId: String, accountMember: AccountMember): Stream[F, AccountMember] = {
    for {
      req ← Stream.eval(PUT(buildAccountMemberUri(accountId, accountMember.id), toDto(accountMember).asJson))
      resp ← createOrUpdate(req)
    } yield resp
  }

  def removeMember(accountId: String, accountMemberId: String): Stream[F, String] = Stream.eval(removeMemberF(accountId, accountMemberId))

  private def buildAccountMemberUri(accountId: String, accountMemberId: String): Uri =
    BaseUrl / "accounts" / accountId / "members" / accountMemberId

  private def createOrUpdate(request: Request[F]): Stream[F, AccountMember] = Stream.eval(createOrUpdateF(request))

  private def createOrUpdateF(request: Request[F]): F[AccountMember] = {
    executor.raw(request) { res ⇒
      for {
        json ← res.decodeJson[Json]
        output ← handleCreateUpdateResponseJson(json, res.status)
      } yield output
    }
  }

  private def removeMemberF(accountId: String, accountMemberId: String): F[String] =
    for {
      req ← DELETE(buildAccountMemberUri(accountId, accountMemberId))
      id ← executor.raw(req) { res ⇒
        for {
          json ← res.decodeJson[Json]
          output ← handleDeleteResponseJson(json, res.status, accountId, accountMemberId)
        } yield output
      }
    } yield id

  private def handleDeleteResponseJson(json: Json, status: Status, accountId: String, accountMemberId: String): F[String] =
    if (status.isSuccess)
      deletedRecordLens(json).fold(Applicative[F].pure(accountMemberId))(Applicative[F].pure)
    else {
      if (status == Status.NotFound)
        Sync[F].raiseError(AccountMemberDoesNotExistException(accountId, accountMemberId))
      else
        Sync[F].raiseError(UnexpectedCloudflareErrorException(errorsLens(json)))
    }

  private def handleCreateUpdateResponseJson(json: Json, status: Status): F[AccountMember] =
    if (status.isSuccess)
      Applicative[F].pure(accountMemberLens(json))
    else {
      Sync[F].raiseError(UnexpectedCloudflareErrorException(errorsLens(json)))
    }

  private val deletedRecordLens: Json ⇒ Option[String] = root.result.id.string.getOption
  private val accountMemberLens: Json ⇒ AccountMember = root.result.as[AccountMemberDTO].getOption(_).get
  private val errorsLens: Json ⇒ List[Error] = root.errors.each.as[model.Error].getAll
}

case class AccountMemberDoesNotExistException(accountId: String, accountMemberId: String) extends RuntimeException(
  s"The account member $accountMemberId not found for account $accountId."
)
