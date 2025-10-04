package com.dwolla.cloudflare

import cats.*
import cats.effect.{Trace as _, *}
import cats.syntax.all.*
import com.dwolla.cloudflare.domain.model.accesscontrolrules.*
import com.dwolla.cloudflare.domain.model.{AccountId, ZoneId, tagAccountId, tagZoneId}
import com.dwolla.tagless.*
import io.circe.syntax.*
import io.circe.*
import io.circe.optics.JsonPath.*
import fs2.*
import com.dwolla.cloudflare.domain.model.Exceptions.UnexpectedCloudflareErrorException
import org.http4s.Method.*
import org.http4s.*
import org.http4s.circe.*
import org.http4s.client.dsl.Http4sClientDsl
import natchez.TraceableValue

trait AccessControlRuleClient[F[_]] {
  def list(level: Level, mode: Option[String]): F[AccessControlRule]
  def getById(level: Level, ruleId: String): F[AccessControlRule]
  def create(level: Level, rule: AccessControlRule): F[AccessControlRule]
  def update(level: Level, rule: AccessControlRule): F[AccessControlRule]
  def delete(level: Level, ruleId: String): F[AccessControlRuleId]
  def getByUri(uri: String): F[AccessControlRule]

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

object AccessControlRuleClient extends AccessControlRuleClientInstances {
  import com.dwolla.tracing.syntax.*
  
  def apply[F[_] : MonadCancelThrow : natchez.Trace](executor: StreamingCloudflareApiExecutor[F]): AccessControlRuleClient[Stream[F, *]] =
    apply(executor, _.traceWithInputsAndOutputs)
  
  def apply[F[_] : ApplicativeThrow](executor: StreamingCloudflareApiExecutor[F],
                                     transform: AccessControlRuleClient[Stream[F, *]] => AccessControlRuleClient[Stream[F, *]]): AccessControlRuleClient[Stream[F, *]] =
    WeaveKnot(knot(executor))(transform)

  private def knot[F[_] : ApplicativeThrow](executor: StreamingCloudflareApiExecutor[F]): Eval[AccessControlRuleClient[Stream[F, *]]] => AccessControlRuleClient[Stream[F, *]] =
    new AccessControlRuleClientImpl[F](executor, _)

  val accountLevelUriRegex = """https://api.cloudflare.com/client/v4/accounts/(.+?)/firewall/access_rules/rules/(.+)""".r
  val zoneLevelUriRegex = """https://api.cloudflare.com/client/v4/zones/(.+?)/firewall/access_rules/rules/(.+)""".r
}

private class AccessControlRuleClientImpl[F[_] : ApplicativeThrow](executor: StreamingCloudflareApiExecutor[F],
                                                                  self: Eval[AccessControlRuleClient[Stream[F, *]]])
  extends AccessControlRuleClient[Stream[F, *]] with Http4sClientDsl[F] {

  private def fetch(req: Request[F]): Stream[F, AccessControlRule] =
    executor.fetch[AccessControlRule](req)

  override def getByUri(uri: String): Stream[F, AccessControlRule] =
    parseUri(uri)
      .fold(MonoidK[Stream[F, *]].empty[AccessControlRule]) {
        case (level, AccessControlRuleId(ruleId)) => self.value.getById(level, ruleId)
      }

  override def list(level: Level, mode: Option[String] = None): Stream[F, AccessControlRule] = {
    fetch(GET(mode.toSeq.foldLeft(buildBaseUrl(level))((uri: Uri, param: String) => uri.withQueryParam("mode", param))))
  }

  override def getById(level: Level, ruleId: String): Stream[F, AccessControlRule] =
    fetch(GET(buildBaseUrl(level) / ruleId))

  override def create(level: Level, rule: AccessControlRule): Stream[F, AccessControlRule] =
    fetch(POST(rule.asJson, buildBaseUrl(level)))

  override def update(level: Level, rule: AccessControlRule): Stream[F, AccessControlRule] =
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

  implicit val traceableValue: TraceableValue[Level] = TraceableValue[String].contramap {
    case Account(id) => s"Account ${id.value}"
    case Zone(id) => s"Zone ${id.value}"
  }
}
