package com.dwolla.cloudflare

import cats._
import cats.effect._
import cats.implicits._
import com.dwolla.cloudflare.domain.dto.accesscontrolrules.AccessControlRuleDTO
import com.dwolla.cloudflare.domain.dto.accounts.AccountDTO
import com.dwolla.cloudflare.domain.model
import com.dwolla.cloudflare.domain.model.{AccountId, Error, tagAccountId}
import com.dwolla.cloudflare.domain.model.Exceptions.UnexpectedCloudflareErrorException
import com.dwolla.cloudflare.domain.model.accesscontrolrules.Implicits._
import com.dwolla.cloudflare.domain.model.accesscontrolrules._
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

trait AccessControlRuleClient[F[_]] {
  def list(accountId: AccountId, mode: Option[String]): Stream[F, Rule]

  def create(accountId: AccountId, rule: CreateRule): Stream[F, Rule]

  def delete(accountId: AccountId, ruleId: String): Stream[F, String]
}

object AccessControlRuleClient {
  def apply[F[_] : Sync](executor: StreamingCloudflareApiExecutor[F]): AccessControlRuleClient[F] = new AccessControlRuleClientImpl[F](executor)
}

class AccessControlRuleClientImpl[F[_] : Sync](executor: StreamingCloudflareApiExecutor[F]) extends AccessControlRuleClient[F] with Http4sClientDsl[F] {
  def list(accountId: AccountId, mode: Option[String]): Stream[F, Rule] =
    for {
      req ← Stream.eval(GET(mode.toSeq.foldLeft(BaseUrl / "accounts" / accountId / "firewall" / "access_rules" / "rules")((uri: Uri, param: String) ⇒ uri.withQueryParam("mode", param))))
      record ← executor.fetch[AccessControlRuleDTO](req)
    } yield Rule(record.id, record.notes, record.allowedModes, record.mode, record.configuration, record.createdOn, record.modifiedOn, record.scope)

  def create(accountId: AccountId, rule: CreateRule): Stream[F, Rule] =
    for {
      req ← Stream.eval(POST(BaseUrl / "accounts" / accountId / "firewall" / "access_rules" / "rules", rule.asJson))
      resp ← createOrFail(req)
    } yield resp

  def delete(accountId: AccountId, ruleId: String): Stream[F, String] = Stream.eval(deleteF(accountId, ruleId))

  def getAccountId(unverifiedAccountID: String): Stream[F, AccountId] =
    executor.fetch[AccountDTO](Request[F](uri = BaseUrl / "accounts" / unverifiedAccountID))
      .map(account => tagAccountId(account.id))

  private def deleteF(accountId: AccountId, ruleId: String): F[String] =
    for {
      req ← DELETE(BaseUrl / "accounts" / accountId / "firewall" / "access_rules" / "rules" / ruleId)
      id ← executor.raw(req) { res ⇒
        for {
          json ← res.decodeJson[Json]
          output ← handleDeleteResponseJson(json, res.status, accountId, ruleId)
        } yield output
      }
    } yield id

  private def handleDeleteResponseJson(json: Json, status: Status, accountId: AccountId, ruleId: String): F[String] =
    if (status.isSuccess)
      deletedRecordLens(json).fold(Applicative[F].pure(ruleId))(Applicative[F].pure)
    else {
      if (status == Status.NotFound)
        Sync[F].raiseError(RuleIdDoesNotExistException(accountId, ruleId))
      else
        Sync[F].raiseError(UnexpectedCloudflareErrorException(errorsLens(json)))
    }

  private def createOrFail(request: Request[F]): Stream[F, Rule] = Stream.eval(createOrF(request))

  private def createOrF(request: Request[F]): F[Rule] =
    executor.raw(request) { res ⇒
      for {
        json ← res.decodeJson[Json]
        output ← handleCreateUpdateResponseJson(json, res.status)
      } yield output
    }

  private def handleCreateUpdateResponseJson(json: Json, status: Status): F[Rule] =
    if (status.isSuccess)
      Applicative[F].pure(ruleLens(json))
    else {
      Sync[F].raiseError(UnexpectedCloudflareErrorException(errorsLens(json)))
    }

  private val deletedRecordLens: Json ⇒ Option[String] = root.result.id.string.getOption
  private val ruleLens: Json ⇒ Rule = root.result.as[AccessControlRuleDTO].getOption(_).get
  private val errorsLens: Json ⇒ List[Error] = root.errors.each.as[model.Error].getAll
}

case class RuleIdDoesNotExistException(accounId: AccountId, ruleId: String) extends RuntimeException(
  s"The access rule $ruleId not found for zone $accounId."
)
