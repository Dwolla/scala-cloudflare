package com.dwolla.cloudflare

import cats.effect._
import com.dwolla.cloudflare.domain.dto.accesscontrolrules.AccessControlRuleDTO
import com.dwolla.cloudflare.domain.model.accesscontrolrules.Implicits._
import com.dwolla.cloudflare.domain.model.accesscontrolrules._
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

trait AccessControlRuleClient[F[_]] {
  def list(accountId: AccountId, mode: Option[String]): Stream[F, Rule]
  def getById(accountId: AccountId, ruleId: String): Stream[F, Rule]
  def create(accountId: AccountId, rule: CreateRule): Stream[F, Rule]
  def update(accountId: AccountId, rule: Rule): Stream[F, Rule]
  def delete(accountId: AccountId, ruleId: String): Stream[F, AccessControlRuleId]

  def parseUri(uri: String): Option[(AccountId, AccessControlRuleId)] = uri match {
    case AccessControlRuleClient.uriRegex(zoneId, rateLimitId) => Option((tagAccountId(zoneId), tagAccessControlRuleId(rateLimitId)))
    case _ => None
  }

}

object AccessControlRuleClient {
  def apply[F[_] : Sync](executor: StreamingCloudflareApiExecutor[F]): AccessControlRuleClient[F] = new AccessControlRuleClientImpl[F](executor)

  val uriRegex: Regex = """https://api.cloudflare.com/client/v4/accounts/(.+?)/firewall/access_rules/(.+)""".r
}

class AccessControlRuleClientImpl[F[_] : Sync](executor: StreamingCloudflareApiExecutor[F]) extends AccessControlRuleClient[F] with Http4sClientDsl[F] {
  override def list(accountId: AccountId, mode: Option[String]): Stream[F, Rule] =
    for {
      req <- Stream.eval(GET(mode.toSeq.foldLeft(BaseUrl / "accounts" / accountId / "firewall" / "access_rules" / "rules")((uri: Uri, param: String) => uri.withQueryParam("mode", param))))
      record <- executor.fetch[AccessControlRuleDTO](req)
    } yield Rule(
      tagAccessControlRuleId(record.id),
      record.notes,
      record.allowedModes,
      record.mode,
      record.configuration,
      record.createdOn,
      record.modifiedOn,
      record.scope,
    )

  override def create(accountId: AccountId, rule: CreateRule): Stream[F, Rule] =
    for {
      req <- Stream.eval(POST(rule.asJson, BaseUrl / "accounts" / accountId / "firewall" / "access_rules" / "rules"))
      resp <- executor.fetch[AccessControlRuleDTO](req).map(Implicits.toModel)
    } yield resp

  override def getById(accountId: AccountId, ruleId: String): Stream[F, Rule] =
    for {
      req <- Stream.eval(GET(BaseUrl / "accounts" / accountId / "firewall" / "access_rules" / "rules" / ruleId))
      resp <- executor.fetch[AccessControlRuleDTO](req).returningEmptyOnErrorCodes(10001).map(Implicits.toModel)
    } yield resp

  override def update(accountId: AccountId, rule: Rule): Stream[F, Rule] =
    for {
      req <- Stream.eval(PATCH(rule.asJson, BaseUrl / "accounts" / accountId / "firewall" / "access_rules" / "rules" / rule.id))
      resp <- executor.fetch[AccessControlRuleDTO](req).map(Implicits.toModel)
    } yield resp

  override def delete(accountId: AccountId, ruleId: String): Stream[F, AccessControlRuleId] =
    for {
      req <- Stream.eval(DELETE(BaseUrl / "accounts" / accountId / "firewall" / "access_rules" / "rules" / ruleId))
      json <- executor.fetch[Json](req).last
    } yield tagAccessControlRuleId(json.flatMap(deletedRecordLens).getOrElse(ruleId))

  private val deletedRecordLens: Json => Option[String] = root.id.string.getOption
}

case class RuleIdDoesNotExistException(accounId: AccountId, ruleId: String) extends RuntimeException(
  s"The access rule $ruleId not found for zone $accounId."
)
