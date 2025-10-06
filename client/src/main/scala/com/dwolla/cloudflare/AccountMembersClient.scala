package com.dwolla.cloudflare

import cats.*
import cats.effect.{Trace as _, *}
import cats.syntax.all.*
import com.dwolla.cloudflare.AccountMembersClientImpl.notFoundCodes
import com.dwolla.cloudflare.domain.dto.accounts.*
import com.dwolla.cloudflare.domain.model.Exceptions.UnexpectedCloudflareErrorException
import com.dwolla.cloudflare.domain.model.accounts.*
import com.dwolla.cloudflare.domain.model.accounts.Implicits.*
import com.dwolla.cloudflare.domain.model.{Implicits as _, *}
import com.dwolla.tagless.*
import com.dwolla.tracing.syntax.*
import io.circe.Json
import io.circe.optics.JsonPath.*
import io.circe.syntax.*
import fs2.*
import org.http4s.*
import org.http4s.Method.*
import org.http4s.circe.*
import org.http4s.client.dsl.Http4sClientDsl

import scala.util.matching.Regex

trait AccountMembersClient[F[_]] {
  def getById(accountId: AccountId, memberId: String): F[AccountMember]
  def addMember(accountId: AccountId, emailAddress: String, roleIds: List[String]): F[AccountMember]
  def updateMember(accountId: AccountId, accountMember: AccountMember): F[AccountMember]
  def removeMember(accountId: AccountId, accountMemberId: String): F[AccountMemberId]
  def getByUri(uri: String): F[AccountMember]

  def parseUri(uri: String): Option[(AccountId, AccountMemberId)] = uri match {
    case AccountMembersClient.uriRegex(accountId, memberId) => Option((tagAccountId(accountId), tagAccountMemberId(memberId)))
    case _ => None
  }
}

object AccountMembersClient extends AccountMembersClientInstances {

  def apply[F[_] : MonadCancelThrow : natchez.Trace](executor: StreamingCloudflareApiExecutor[F]): AccountMembersClient[Stream[F, *]] =
    apply(executor, _.traceWithInputsAndOutputs)

  def apply[F[_] : ApplicativeThrow](executor: StreamingCloudflareApiExecutor[F],
                                     transform: AccountMembersClient[Stream[F, *]] => AccountMembersClient[Stream[F, *]]): AccountMembersClient[Stream[F, *]] =
    WeaveKnot(knot(executor))(transform)

  private def knot[F[_] : ApplicativeThrow](executor: StreamingCloudflareApiExecutor[F]): Eval[AccountMembersClient[Stream[F, *]]] => AccountMembersClient[Stream[F, *]] =
    new AccountMembersClientImpl[F](executor, _)

  val uriRegex: Regex = """https://api.cloudflare.com/client/v4/accounts/(.+?)/members/(.+)""".r
}

private class AccountMembersClientImpl[F[_] : ApplicativeThrow](executor: StreamingCloudflareApiExecutor[F],
                                                                self: Eval[AccountMembersClient[Stream[F, *]]]) extends AccountMembersClient[Stream[F, *]] with Http4sClientDsl[F] {
  override def getByUri(uri: String): Stream[F, AccountMember] =
    parseUri(uri)
      .fold(MonoidK[Stream[F, *]].empty[AccountMember]) {
        case (accountId, AccountMemberId(memberId)) => self.value.getById(accountId, memberId)
      }

  override def getById(accountId: AccountId, accountMemberId: String): Stream[F, AccountMember] =
    executor.fetch[AccountMemberDTO](GET(buildAccountMemberUri(accountId, AccountMemberId(accountMemberId))))
      .returningEmptyOnErrorCodes(notFoundCodes: _*)
      .map(toModel)

  override def addMember(accountId: AccountId, emailAddress: String, roleIds: List[String]): Stream[F, AccountMember] =
    createOrUpdate(POST(NewAccountMemberDTO(emailAddress, roleIds, Some("pending")).asJson, BaseUrl / "accounts" / accountId / "members"))

  override def updateMember(accountId: AccountId, accountMember: AccountMember): Stream[F, AccountMember] =
    createOrUpdate(PUT(toDto(accountMember).asJson, buildAccountMemberUri(accountId, accountMember.id)))

  override def removeMember(accountId: AccountId, accountMemberId: String): Stream[F, AccountMemberId] =
    for {
      json <- executor.fetch[Json](DELETE(buildAccountMemberUri(accountId, AccountMemberId(accountMemberId)))).last.adaptError {
        case ex: UnexpectedCloudflareErrorException if ex.errors.flatMap(_.code.toSeq).exists(notFoundCodes.contains) =>
          AccountMemberDoesNotExistException(accountId, accountMemberId)
      }
    } yield tagAccountMemberId(json.flatMap(deletedRecordLens).getOrElse(accountMemberId))

  private def buildAccountMemberUri(accountId: AccountId, accountMemberId: AccountMemberId): Uri =
    BaseUrl / "accounts" / accountId / "members" / accountMemberId

  private def createOrUpdate(request: Request[F]): Stream[F, AccountMember] =
    executor.fetch[AccountMemberDTO](request).map(Implicits.toModel)

  private val deletedRecordLens: Json => Option[String] = root.id.string.getOption
}

object AccountMembersClientImpl {
  val notFoundCodes = List(1003)
}
