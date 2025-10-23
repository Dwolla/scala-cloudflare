package com.dwolla.cloudflare

import cats.*
import cats.effect.{Trace as _, *}
import cats.syntax.all.*
import com.dwolla.cloudflare.domain.model.Exceptions.UnexpectedCloudflareErrorException
import com.dwolla.cloudflare.domain.model.firewallrules.*
import com.dwolla.cloudflare.domain.model.{ZoneId, tagZoneId}
import com.dwolla.tagless.*
import com.dwolla.tracing.syntax.*
import io.circe.*
import io.circe.optics.JsonPath.*
import io.circe.syntax.*
import fs2.*
import org.http4s.Method.*
import org.http4s.circe.*
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.syntax.all.*
import org.http4s.{Request, Uri}

import scala.util.matching.Regex

trait FirewallRuleClient[F[_]] {
  def list(zoneId: ZoneId): F[FirewallRule]
  def getById(zoneId: ZoneId, firewallRuleId: String): F[FirewallRule]
  def create(zoneId: ZoneId, firewallRule: FirewallRule): F[FirewallRule]
  def update(zoneId: ZoneId, firewallRule: FirewallRule): F[FirewallRule]
  def delete(zoneId: ZoneId, firewallRuleId: String): F[FirewallRuleId]
  def getByUri(uri: String): F[FirewallRule]

  def parseUri(uri: String): Option[(ZoneId, FirewallRuleId)] = uri match {
    case FirewallRuleClient.uriRegex(zoneId, firewallRuleId) => Option((tagZoneId(zoneId), tagFirewallRuleId(firewallRuleId)))
    case _ => None
  }

  def buildUri(zoneId: ZoneId, firewallRuleId: FirewallRuleId): Uri =
    uri"https://api.cloudflare.com/client/v4/zones" / zoneId / "firewall" / "rules" / firewallRuleId
}

object FirewallRuleClient extends FirewallRuleClientInstances {
  def apply[F[_] : MonadCancelThrow : natchez.Trace](executor: StreamingCloudflareApiExecutor[F]): FirewallRuleClient[Stream[F, *]] =
    apply(executor, _.traceWithInputsAndOutputs)

  def apply[F[_] : ApplicativeThrow](executor: StreamingCloudflareApiExecutor[F],
                                     transform: FirewallRuleClient[Stream[F, *]] => FirewallRuleClient[Stream[F, *]]): FirewallRuleClient[Stream[F, *]] =
    WeaveKnot(knot(executor))(transform)

  private def knot[F[_] : ApplicativeThrow](executor: StreamingCloudflareApiExecutor[F]): Eval[FirewallRuleClient[Stream[F, *]]] => FirewallRuleClient[Stream[F, *]] =
    new FirewallRuleClientImpl[F](executor, _)

  val uriRegex: Regex = """https://api.cloudflare.com/client/v4/zones/(.+?)/firewall/rules/(.+)""".r
}

private class FirewallRuleClientImpl[F[_] : ApplicativeThrow](executor: StreamingCloudflareApiExecutor[F],
                                                              self: Eval[FirewallRuleClient[Stream[F, *]]])
  extends FirewallRuleClient[Stream[F, *]]
    with Http4sClientDsl[F] {
  private def fetch(req: Request[F]): Stream[F, FirewallRule] =
    executor.fetch[FirewallRule](req)

  override def list(zoneId: ZoneId): Stream[F, FirewallRule] =
    fetch(GET(BaseUrl / "zones" / zoneId / "firewall" / "rules"))

  override def getById(zoneId: ZoneId, firewallRuleId: String): Stream[F, FirewallRule] =
    fetch(GET(BaseUrl / "zones" / zoneId / "firewall" / "rules" / firewallRuleId))

  override def create(zoneId: ZoneId, firewallRule: FirewallRule): Stream[F, FirewallRule] =
    fetch(POST(List(firewallRule).asJson, BaseUrl / "zones" / zoneId / "firewall" / "rules"))

  override def update(zoneId: ZoneId, firewallRule: FirewallRule): Stream[F, FirewallRule] =
    // TODO it would really be better to do this check at compile time by baking the identification question into the types
    if (firewallRule.id.isDefined)
      fetch(PUT(firewallRule.copy(id = None).asJson, BaseUrl / "zones" / zoneId / "firewall" / "rules" / firewallRule.id.get))
    else
      Stream.raiseError[F](CannotUpdateUnidentifiedFirewallRule(firewallRule))

  override def delete(zoneId: ZoneId, firewallRuleId: String): Stream[F, FirewallRuleId] =
    for {
      json <- executor.fetch[Json](DELETE(BaseUrl / "zones" / zoneId / "firewall" / "rules" / firewallRuleId)).last.recover {
        case ex: UnexpectedCloudflareErrorException if ex.errors.flatMap(_.code.toSeq).exists(notFoundCodes.contains) =>
          None
      }
    } yield tagFirewallRuleId(json.flatMap(deletedRecordLens).getOrElse(firewallRuleId))

  override def getByUri(uri: String): Stream[F, FirewallRule] =
    parseUri(uri).fold(MonoidK[Stream[F, *]].empty[FirewallRule]) {
      case (zoneId, FirewallRuleId(firewallRuleId)) => self.value.getById(zoneId, firewallRuleId)
    }

  private val deletedRecordLens: Json => Option[String] = root.id.string.getOption
  private val notFoundCodes = List(1002, 7000, 7003)
}

case class CannotUpdateUnidentifiedFirewallRule(firewallRule: FirewallRule) extends RuntimeException(s"Cannot update unidentified firewall rule $firewallRule")
