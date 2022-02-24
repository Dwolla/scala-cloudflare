package com.dwolla.cloudflare

import cats._
import cats.implicits._
import com.dwolla.cloudflare.AccountMembersClientImpl.notFoundCodes
import com.dwolla.cloudflare.domain.dto.accounts._
import com.dwolla.cloudflare.domain.model.Exceptions.UnexpectedCloudflareErrorException
import com.dwolla.cloudflare.domain.model.accounts.Implicits.{toDto, _}
import com.dwolla.cloudflare.domain.model.accounts._
import com.dwolla.cloudflare.domain.model.{Implicits => _, _}
import io.circe.Json
import io.circe.optics.JsonPath._
import io.circe.syntax._
import fs2._
import org.http4s.Method._
import org.http4s._
import org.http4s.circe._
import org.http4s.client.dsl.Http4sClientDsl

import scala.util.matching.Regex

trait AccountMembersClient[F[_]] {
  def getById(accountId: AccountId, memberId: String): Stream[F, AccountMember]
  def addMember(accountId: AccountId, emailAddress: String, roleIds: List[String]): Stream[F, AccountMember]
  def updateMember(accountId: AccountId, accountMember: AccountMember): Stream[F, AccountMember]
  def removeMember(accountId: AccountId, accountMemberId: String): Stream[F, AccountMemberId]

  def getByUri(uri: String): Stream[F, AccountMember] = parseUri(uri).fold(Stream.empty.covaryAll[F, AccountMember]) {
    case (accountId, memberId) => getById(accountId, memberId)
  }

  def parseUri(uri: String): Option[(AccountId, AccountMemberId)] = uri match {
    case AccountMembersClient.uriRegex(accountId, memberId) => Option((tagAccountId(accountId), tagAccountMemberId(memberId)))
    case _ => None
  }
}

object AccountMembersClient {
  def apply[F[_] : MonadThrow](executor: StreamingCloudflareApiExecutor[F]): AccountMembersClient[F] = new AccountMembersClientImpl[F](executor)

  val uriRegex: Regex = """https://api.cloudflare.com/client/v4/accounts/(.+?)/members/(.+)""".r
}

class AccountMembersClientImpl[F[_] : MonadThrow](executor: StreamingCloudflareApiExecutor[F]) extends AccountMembersClient[F] with Http4sClientDsl[F] {
  override def getById(accountId: AccountId, accountMemberId: String): Stream[F, AccountMember] =
    for {
      res <- executor.fetch[AccountMemberDTO](GET(buildAccountMemberUri(accountId, accountMemberId))).returningEmptyOnErrorCodes(notFoundCodes: _*)
    } yield res

  override def addMember(accountId: AccountId, emailAddress: String, roleIds: List[String]): Stream[F, AccountMember] =
    createOrUpdate(POST(NewAccountMemberDTO(emailAddress, roleIds, Some("pending")).asJson, BaseUrl / "accounts" / accountId / "members"))

  override def updateMember(accountId: AccountId, accountMember: AccountMember): Stream[F, AccountMember] =
    createOrUpdate(PUT(toDto(accountMember).asJson, buildAccountMemberUri(accountId, accountMember.id)))

  override def removeMember(accountId: AccountId, accountMemberId: String): Stream[F, AccountMemberId] =
    for {
      json <- executor.fetch[Json](DELETE(buildAccountMemberUri(accountId, accountMemberId))).last.adaptError {
        case ex: UnexpectedCloudflareErrorException if ex.errors.flatMap(_.code.toSeq).exists(notFoundCodes.contains) =>
          AccountMemberDoesNotExistException(accountId, accountMemberId)
      }
    } yield tagAccountMemberId(json.flatMap(deletedRecordLens).getOrElse(accountMemberId))

  private def buildAccountMemberUri(accountId: AccountId, accountMemberId: String): Uri =
    BaseUrl / "accounts" / accountId / "members" / accountMemberId

  private def createOrUpdate(request: Request[F]): Stream[F, AccountMember] =
    executor.fetch[AccountMemberDTO](request).map(Implicits.toModel)

  private val deletedRecordLens: Json => Option[String] = root.id.string.getOption
}

object AccountMembersClientImpl {
  val notFoundCodes = List(1003)
}
