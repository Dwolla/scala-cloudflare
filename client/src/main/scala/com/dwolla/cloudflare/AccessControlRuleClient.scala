package com.dwolla.cloudflare

import cats._
import cats.syntax.all._
import com.dwolla.cloudflare.domain.model.accesscontrolrules._
import com.dwolla.cloudflare.domain.model.{AccountId, ZoneId, tagAccountId, tagZoneId}
import io.circe.syntax._
import io.circe._
import io.circe.optics.JsonPath._
import fs2._
import com.dwolla.cloudflare.domain.model.Exceptions.UnexpectedCloudflareErrorException
import org.http4s.Method._
import org.http4s._
import org.http4s.circe._
import org.http4s.client.dsl.Http4sClientDsl

trait AccessControlRuleClient[F[_]] {
  def list(level: Level, mode: Option[String] = None): Stream[F, AccessControlRule]
  def getById(level: Level, ruleId: String): Stream[F, AccessControlRule]
  def create(level: Level, rule: AccessControlRule): Stream[F, AccessControlRule]
  def update(level: Level, rule: AccessControlRule): Stream[F, AccessControlRule]
  def delete(level: Level, ruleId: String): Stream[F, AccessControlRuleId]

  def getByUri(uri: String): Stream[F, AccessControlRule] = parseUri(uri).fold(Stream.empty.covaryAll[F, AccessControlRule]) {
    case (level, ruleId) => getById(level, ruleId)
  }

  def parseUri(uri: String): Option[(Level, AccessControlRuleId)] = uri match {
    case AccessControlRuleClient.accountLevelUriRegex(accountId, ruleId) => Option((Level.Account(tagAccountId(accountId)), tagAccessControlRuleId(ruleId)))
    case AccessControlRuleClient.zoneLevelUriRegex(zoneId, ruleId) => Option((Level.Zone(tagZoneId(zoneId)), tagAccessControlRuleId(ruleId)))
    case _ => None
  }

  def buildUri(level: Level, ruleId: AccessControlRuleId): Uri =
    buildBaseUrl(level) / ruleId

  def buildBaseUrl(level: Level): Uri = {
    val baseUrlWithLevel = level match {
      case Level.Account(id) => BaseUrl / "accounts" / id
      case Level.Zone(id) => BaseUrl / "zones" / id
    }
    baseUrlWithLevel / "firewall" / "access_rules" / "rules"
  }
}

object AccessControlRuleClient {
  def apply[F[_] : ApplicativeThrow](executor: StreamingCloudflareApiExecutor[F]): AccessControlRuleClient[F] = new AccessControlRuleClientImpl[F](executor)

  val accountLevelUriRegex = """https://api.cloudflare.com/client/v4/accounts/(.+?)/firewall/access_rules/rules/(.+)""".r
  val zoneLevelUriRegex = """https://api.cloudflare.com/client/v4/zones/(.+?)/firewall/access_rules/rules/(.+)""".r
}

class AccessControlRuleClientImpl[F[_] : ApplicativeThrow](executor: StreamingCloudflareApiExecutor[F]) extends AccessControlRuleClient[F] with Http4sClientDsl[F] {
  private def fetch(req: Request[F]): Stream[F, AccessControlRule] =
    executor.fetch[AccessControlRule](req)

  override def list(level: Level, mode: Option[String] = None): Stream[F, AccessControlRule] = {
    fetch(GET(mode.toSeq.foldLeft(buildBaseUrl(level))((uri: Uri, param: String) => uri.withQueryParam("mode", param))))
  }

  override def getById(level: Level, ruleId: String): Stream[F, AccessControlRule] =
    fetch(GET(buildBaseUrl(level) / ruleId))

  override def create(level: Level, rule: AccessControlRule): Stream[F, AccessControlRule] =
    fetch(POST(rule.asJson, buildBaseUrl(level)))

  override def update(level: Level, rule: AccessControlRule): Stream[F, AccessControlRule] =
  // TODO it would really be better to do this check at compile time by baking the identification question into the types
    if (rule.id.isDefined)
      fetch(PATCH(rule.copy(id = None).asJson, buildBaseUrl(level) / rule.id.get))
    else
      Stream.raiseError[F](CannotUpdateUnidentifiedAccessControlRule(rule))

  override def delete(level: Level, ruleId: String): Stream[F, AccessControlRuleId] =
    for {
      json <- executor.fetch[Json](DELETE(buildBaseUrl(level) / ruleId)).last.recover {
        case ex: UnexpectedCloudflareErrorException if ex.errors.flatMap(_.code.toSeq).exists(notFoundCodes.contains) =>
          None
      }
    } yield tagAccessControlRuleId(json.flatMap(deletedRecordLens).getOrElse(ruleId))

  private val deletedRecordLens: Json => Option[String] = root.id.string.getOption
  private val notFoundCodes = List(10001)
}

case class CannotUpdateUnidentifiedAccessControlRule(rule: AccessControlRule) extends RuntimeException(s"Cannot update unidentified access control rule $rule")

sealed trait Level
object Level {
  case class Account(accountId: AccountId) extends Level
  case class Zone(zoneId: ZoneId) extends Level
}
