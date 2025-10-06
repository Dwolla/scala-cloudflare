package com.dwolla.cloudflare

import cats.*
import cats.effect.{Trace as _, *}
import com.dwolla.cloudflare.domain.dto.accounts.*
import com.dwolla.cloudflare.domain.model.accounts.Implicits.*
import com.dwolla.cloudflare.domain.model.accounts.*
import com.dwolla.cloudflare.domain.model.{Implicits as _, *}
import com.dwolla.tagless.*
import com.dwolla.tracing.syntax.*
import fs2.*
import org.http4s.Method.*
import org.http4s.client.dsl.Http4sClientDsl

import scala.util.matching.Regex

trait AccountsClient[F[_]] {
  def list(): F[Account]
  def getById(accountId: String): F[Account]
  def getByName(name: String): F[Account]
  def listRoles(accountId: AccountId): F[AccountRole]
  def getByUri(uri: String): F[Account]

  def parseUri(uri: String): Option[AccountId] = uri match {
    case AccountsClient.uriRegex(accountId) => Option(tagAccountId(accountId))
    case _ => None
  }
}

object AccountsClient extends AccountsClientInstances {
  def apply[F[_] : MonadCancelThrow : natchez.Trace](executor: StreamingCloudflareApiExecutor[F]): AccountsClient[Stream[F, *]] =
    apply(executor, _.traceWithInputsAndOutputs)

  def apply[F[_] : ApplicativeThrow](executor: StreamingCloudflareApiExecutor[F],
                                     transform: AccountsClient[Stream[F, *]] => AccountsClient[Stream[F, *]]): AccountsClient[Stream[F, *]] =
    WeaveKnot(knot(executor))(transform)

  private def knot[F[_] : ApplicativeThrow](executor: StreamingCloudflareApiExecutor[F]): Eval[AccountsClient[Stream[F, *]]] => AccountsClient[Stream[F, *]] =
    new AccountsClientImpl[F](executor, _)

  val uriRegex: Regex = """https://api.cloudflare.com/client/v4/accounts/(.+?)""".r
}

private class AccountsClientImpl[F[_]: ApplicativeThrow](executor: StreamingCloudflareApiExecutor[F],
                                                         self: Eval[AccountsClient[Stream[F, *]]])
  extends AccountsClient[Stream[F, *]] with Http4sClientDsl[F] {

  override def list(): Stream[F, Account] =
    executor.fetch[AccountDTO](GET(BaseUrl / "accounts" withQueryParam("direction", "asc")))
      .map(toModel)

  override def getById(accountId: String): Stream[F, Account] =
    executor.fetch[AccountDTO](GET(BaseUrl / "accounts" / accountId))
      .returningEmptyOnErrorCodes(AccountsClientImpl.notFoundCodes: _*)
      .map(toModel)

  override def getByName(name: String): Stream[F, Account] =
    self.value.list().filter(a => a.name.toUpperCase == name.toUpperCase)

  override def listRoles(accountId: AccountId): Stream[F, AccountRole] =
    executor.fetch[AccountRoleDTO](GET(BaseUrl / "accounts" / accountId / "roles"))
      .map(toModel)

  override def getByUri(uri: String): Stream[F, Account] =
    parseUri(uri)
      .fold(MonoidK[Stream[F, *]].empty[Account]) { id =>
        self.value.getById(id.value)
      }
}

private object AccountsClientImpl {
  val notFoundCodes = List(1003)
}

case class AccountMemberDoesNotExistException(accountId: AccountId, accountMemberId: String) extends RuntimeException(
  s"The account member $accountMemberId not found for account $accountId."
)
