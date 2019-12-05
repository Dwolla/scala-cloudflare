package com.dwolla.cloudflare

import cats.implicits._
import com.dwolla.cloudflare.domain.model.accesscontrolrules._
import com.dwolla.cloudflare.domain.model.tagAccountId
import io.circe.syntax._
import io.circe._
import io.circe.optics.JsonPath._
import fs2._
import cats.effect.Sync
import com.dwolla.cloudflare.domain.model.AccountId
import com.dwolla.cloudflare.domain.model.Exceptions.UnexpectedCloudflareErrorException
import org.http4s.Method._
import org.http4s._
import org.http4s.circe._
import org.http4s.client.dsl.Http4sClientDsl

import scala.util.matching.Regex

trait AccessControlRuleClient[F[_]] {
  def list(accountId: AccountId, mode: Option[String] = None): Stream[F, AccessControlRule]
  def getById(accountId: AccountId, ruleId: String): Stream[F, AccessControlRule]
  def create(accountId: AccountId, rule: AccessControlRule): Stream[F, AccessControlRule]
  def update(accountId: AccountId, rule: AccessControlRule): Stream[F, AccessControlRule]
  def delete(accountId: AccountId, ruleId: String): Stream[F, AccessControlRuleId]

  def getByUri(uri: String): Stream[F, AccessControlRule] = parseUri(uri).fold(Stream.empty.covaryAll[F, AccessControlRule]) {
    case (accountId, ruleId) => getById(accountId, ruleId)
  }

  def parseUri(uri: String): Option[(AccountId, AccessControlRuleId)] = uri match {
    case AccessControlRuleClient.uriRegex(accountId, ruleId) => Option((tagAccountId(accountId), tagAccessControlRuleId(ruleId)))
    case _ => None
  }

  def buildUri(accountId: AccountId, ruleId: AccessControlRuleId): String =
    s"https://api.cloudflare.com/client/v4/accounts/$accountId/firewall/access_rules/$ruleId"
}

object AccessControlRuleClient {
  def apply[F[_] : Sync](executor: StreamingCloudflareApiExecutor[F]): AccessControlRuleClient[F] = new AccessControlRuleClientImpl[F](executor)

  val uriRegex: Regex = """https://api.cloudflare.com/client/v4/accounts/(.+?)/firewall/access_rules/(.+)""".r
}

class AccessControlRuleClientImpl[F[_] : Sync](executor: StreamingCloudflareApiExecutor[F]) extends AccessControlRuleClient[F] with Http4sClientDsl[F] {
  private def fetch(reqF: F[Request[F]]): Stream[F, AccessControlRule] =
    for {
      req <- Stream.eval(reqF)
      res <- executor.fetch[AccessControlRule](req)
    } yield res

  override def list(accountId: AccountId, mode: Option[String] = None): Stream[F, AccessControlRule] =
    fetch(GET(mode.toSeq.foldLeft(BaseUrl / "accounts" / accountId / "firewall" / "access_rules" / "rules")((uri: Uri, param: String) => uri.withQueryParam("mode", param))))

  override def getById(accountId: AccountId, ruleId: String): Stream[F, AccessControlRule] =
    fetch(GET(BaseUrl / "accounts" / accountId / "firewall" / "access_rules" / "rules" / ruleId))

  override def create(accountId: AccountId, rule: AccessControlRule): Stream[F, AccessControlRule] =
    fetch(POST(rule.asJson, BaseUrl / "accounts" / accountId / "firewall" / "access_rules" / "rules"))

  override def update(accountId: AccountId, rule: AccessControlRule): Stream[F, AccessControlRule] =
  // TODO it would really be better to do this check at compile time by baking the identification question into the types
    if (rule.id.isDefined)
      fetch(PATCH(rule.copy(id = None).asJson, BaseUrl / "accounts" / accountId / "firewall" / "access_rules" / "rules" / rule.id.get))
    else
      Stream.raiseError[F](CannotUpdateUnidentifiedAccessControlRule(rule))

  override def delete(accountId: AccountId, ruleId: String): Stream[F, AccessControlRuleId] =
    for {
      req <- Stream.eval(DELETE(BaseUrl / "accounts" / accountId / "firewall" / "access_rules" / "rules" / ruleId))
      json <- executor.fetch[Json](req).last.recover {
        case ex: UnexpectedCloudflareErrorException if ex.errors.flatMap(_.code.toSeq).exists(notFoundCodes.contains) =>
          None
      }
    } yield tagAccessControlRuleId(json.flatMap(deletedRecordLens).getOrElse(ruleId))

  private val deletedRecordLens: Json => Option[String] = root.id.string.getOption
  private val notFoundCodes = List(10001)
}

case class RuleIdDoesNotExistException(accounId: AccountId, ruleId: String) extends RuntimeException(
  s"The access control rule $ruleId not found for zone $accounId."
)

case class CannotUpdateUnidentifiedAccessControlRule(rule: AccessControlRule) extends RuntimeException(s"Cannot update unidentified access control rule $rule")
