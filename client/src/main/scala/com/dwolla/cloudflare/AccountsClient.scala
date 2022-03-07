package com.dwolla.cloudflare

import cats._
import com.dwolla.cloudflare.AccountsClientImpl._
import com.dwolla.cloudflare.domain.dto.accounts._
import com.dwolla.cloudflare.domain.model.accounts.Implicits._
import com.dwolla.cloudflare.domain.model.accounts._
import com.dwolla.cloudflare.domain.model.{Implicits => _, _}
import fs2._
import org.http4s.Method._
import org.http4s.client.dsl.Http4sClientDsl

import scala.util.matching.Regex

trait AccountsClient[F[_]] {
  def list(): Stream[F, Account]
  def getById(accountId: String): Stream[F, Account]
  def getByName(name: String): Stream[F, Account]
  def listRoles(accountId: AccountId): Stream[F, AccountRole]

  def getByUri(uri: String): Stream[F, Account] = parseUri(uri).fold(Stream.empty.covaryAll[F, Account])(getById)

  def parseUri(uri: String): Option[AccountId] = uri match {
    case AccountsClient.uriRegex(accountId) => Option(tagAccountId(accountId))
    case _ => None
  }
}

object AccountsClient {
  def apply[F[_] : ApplicativeThrow](executor: StreamingCloudflareApiExecutor[F]): AccountsClient[F] = new AccountsClientImpl[F](executor)

  val uriRegex: Regex = """https://api.cloudflare.com/client/v4/accounts/(.+?)""".r
}

object AccountsClientImpl {
  val notFoundCodes = List(1003)
}

class AccountsClientImpl[F[_]: ApplicativeThrow](executor: StreamingCloudflareApiExecutor[F]) extends AccountsClient[F] with Http4sClientDsl[F] {
  override def list(): Stream[F, Account] = {
    for {
      record <- executor.fetch[AccountDTO](GET(BaseUrl / "accounts" withQueryParam("direction", "asc")))
    } yield record
  }

  override def getById(accountId: String): Stream[F, Account] =
    for {
      res <- executor.fetch[AccountDTO](GET(BaseUrl / "accounts" / accountId)).returningEmptyOnErrorCodes(notFoundCodes: _*)
    } yield res

  override def getByName(name: String): Stream[F, Account] = {
    list()
      .filter(a => a.name.toUpperCase == name.toUpperCase)
  }

  override def listRoles(accountId: AccountId): Stream[F, AccountRole] = {
    for {
      record <- executor.fetch[AccountRoleDTO](GET(BaseUrl / "accounts" / accountId / "roles"))
    } yield record
  }

}

case class AccountMemberDoesNotExistException(accountId: AccountId, accountMemberId: String) extends RuntimeException(
  s"The account member $accountMemberId not found for account $accountId."
)
