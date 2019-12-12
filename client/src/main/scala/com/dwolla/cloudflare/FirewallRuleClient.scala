package com.dwolla.cloudflare

import cats.implicits._
import com.dwolla.cloudflare.domain.model.{ZoneId, tagZoneId}
import com.dwolla.cloudflare.domain.model.firewallrules._
import io.circe.syntax._
import io.circe._
import io.circe.optics.JsonPath._
import fs2._
import cats.effect.Sync
import com.dwolla.cloudflare.domain.model.Exceptions.UnexpectedCloudflareErrorException
import org.http4s.Method._
import org.http4s.Request
import org.http4s.circe._
import org.http4s.client.dsl.Http4sClientDsl

import scala.util.matching.Regex

trait FirewallRuleClient[F[_]] {
  def list(zoneId: ZoneId): Stream[F, FirewallRule]
  def getById(zoneId: ZoneId, firewallRuleId: String): Stream[F, FirewallRule]
  def create(zoneId: ZoneId, firewallRule: FirewallRule): Stream[F, FirewallRule]
  def update(zoneId: ZoneId, firewallRule: FirewallRule): Stream[F, FirewallRule]
  def delete(zoneId: ZoneId, firewallRuleId: String): Stream[F, FirewallRuleId]

  def getByUri(uri: String): Stream[F, FirewallRule] = parseUri(uri).fold(Stream.empty.covaryAll[F, FirewallRule]) {
    case (zoneId, firewallRuleId) => getById(zoneId, firewallRuleId)
  }

  def parseUri(uri: String): Option[(ZoneId, FirewallRuleId)] = uri match {
    case FirewallRuleClient.uriRegex(zoneId, firewallRuleId) => Option((tagZoneId(zoneId), tagFirewallRuleId(firewallRuleId)))
    case _ => None
  }

  def buildUri(zoneId: ZoneId, firewallRuleId: FirewallRuleId): String =
    s"https://api.cloudflare.com/client/v4/zones/$zoneId/firewall/rules/$firewallRuleId"

}

object FirewallRuleClient {
  def apply[F[_] : Sync](executor: StreamingCloudflareApiExecutor[F]): FirewallRuleClient[F] = new FirewallRuleClientImpl[F](executor)

  val uriRegex: Regex = """https://api.cloudflare.com/client/v4/zones/(.+?)/firewall/rules/(.+)""".r
}

class FirewallRuleClientImpl[F[_] : Sync](executor: StreamingCloudflareApiExecutor[F]) extends FirewallRuleClient[F] with Http4sClientDsl[F] {
  private def fetch(reqF: F[Request[F]]): Stream[F, FirewallRule] =
    for {
      req <- Stream.eval(reqF)
      res <- executor.fetch[FirewallRule](req)
    } yield res

  override def list(zoneId: ZoneId): Stream[F, FirewallRule] =
    fetch(GET(BaseUrl / "zones" / zoneId / "firewall" / "rules"))

  override def getById(zoneId: ZoneId, firewallRuleId: String): Stream[F, FirewallRule] =
    fetch(GET(BaseUrl / "zones" / zoneId / "firewall" / "rules" / firewallRuleId))

  override def create(zoneId: ZoneId, firewallRule: FirewallRule): Stream[F, FirewallRule] =
//    Stream.raiseError[F](new RuntimeException(firewallRule.asJson.toString()))
    fetch(POST(List(firewallRule).asJson, BaseUrl / "zones" / zoneId / "firewall" / "rules"))

  override def update(zoneId: ZoneId, firewallRule: FirewallRule): Stream[F, FirewallRule] =
    // TODO it would really be better to do this check at compile time by baking the identification question into the types
    if (firewallRule.id.isDefined)
      fetch(PUT(firewallRule.copy(id = None).asJson, BaseUrl / "zones" / zoneId / "firewall" / "rules" / firewallRule.id.get))
    else
      Stream.raiseError[F](CannotUpdateUnidentifiedFirewallRule(firewallRule))

  override def delete(zoneId: ZoneId, firewallRuleId: String): Stream[F, FirewallRuleId] =
    for {
      req <- Stream.eval(DELETE(BaseUrl / "zones" / zoneId / "firewall" / "rules" / firewallRuleId))
      json <- executor.fetch[Json](req).last.recover {
        case ex: UnexpectedCloudflareErrorException if ex.errors.flatMap(_.code.toSeq).exists(notFoundCodes.contains) =>
          None
      }
    } yield tagFirewallRuleId(json.flatMap(deletedRecordLens).getOrElse(firewallRuleId))

  private val deletedRecordLens: Json => Option[String] = root.id.string.getOption
  private val notFoundCodes = List(1002, 7000, 7003)
}

case class CannotUpdateUnidentifiedFirewallRule(firewallRule: FirewallRule) extends RuntimeException(s"Cannot update unidentified firewall rule $firewallRule")
