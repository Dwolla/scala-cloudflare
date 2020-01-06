package com.dwolla.cloudflare

import _root_.io.circe.literal._
import cats.effect.Sync
import com.dwolla.cloudflare.domain.model.wafrules._
import com.dwolla.cloudflare.domain.model.{WafRulePackageId, ZoneId, tagWafRulePackageId, tagZoneId}
import io.circe.syntax._
import fs2._
import org.http4s.Method._
import org.http4s.Request
import org.http4s.circe._
import org.http4s.client.dsl.Http4sClientDsl

import scala.util.matching.Regex

trait WafRuleClient[F[_]] {
  def list(zoneId: ZoneId, wafRulePackageId: WafRulePackageId): Stream[F, WafRule]
  def getById(zoneId: ZoneId, wafRulePackageId: WafRulePackageId, wafRuleId: WafRuleId): Stream[F, WafRule]
  def setMode(zoneId: ZoneId, wafRulePackageId: WafRulePackageId, wafRuleId: WafRuleId, mode: Mode): Stream[F, WafRule]

  def getByUri(uri: String): Stream[F, WafRule] = parseUri(uri).fold(Stream.empty.covaryAll[F, WafRule]) {
    case (zoneId, wafRulePackageId, wafRuleId) => getById(zoneId, wafRulePackageId, wafRuleId)
  }

  def parseUri(uri: String): Option[(ZoneId, WafRulePackageId, WafRuleId)] = uri match {
    case WafRuleClient.uriRegex(zoneId, wafRulePackageId, wafRuleId) => Option((tagZoneId(zoneId), tagWafRulePackageId(wafRulePackageId), tagWafRuleId(wafRuleId)))
    case _ => None
  }

  def buildUri(zoneId: ZoneId, wafRulePackageId: WafRulePackageId, wafRuleId: WafRuleId): String =
    s"https://api.cloudflare.com/client/v4/zones/$zoneId/firewall/waf/packages/$wafRulePackageId/rules/$wafRuleId"
}

object WafRuleClient {
  def apply[F[_] : Sync](executor: StreamingCloudflareApiExecutor[F]): WafRuleClient[F] = new WafRuleClientImpl[F](executor)

  val uriRegex: Regex = """https://api.cloudflare.com/client/v4/zones/(.+?)/firewall/waf/packages/(.+)/rules/(.+)""".r
}

class WafRuleClientImpl[F[_] : Sync](executor: StreamingCloudflareApiExecutor[F]) extends WafRuleClient[F] with Http4sClientDsl[F] {
  private def fetch(reqF: F[Request[F]]): Stream[F, WafRule] =
    for {
      req <- Stream.eval(reqF)
      res <- executor.fetch[WafRule](req)
    } yield res

  override def list(zoneId: ZoneId, wafRulePackageId: WafRulePackageId): Stream[F, WafRule] =
    fetch(GET(BaseUrl / "zones" / zoneId / "firewall" / "waf" / "packages" / wafRulePackageId / "rules"))

  override def getById(zoneId: ZoneId, wafRulePackageId: WafRulePackageId, wafRuleId: WafRuleId): Stream[F, WafRule] =
    fetch(GET(BaseUrl / "zones" / zoneId / "firewall" / "waf" / "packages" / wafRulePackageId / "rules" / wafRuleId))

  override def setMode(zoneId: ZoneId, wafRulePackageId: WafRulePackageId, wafRuleId: WafRuleId, mode: Mode): Stream[F, WafRule] =
    fetch(PATCH(json"""{"mode" : ${mode.asJson}}""", BaseUrl / "zones" / zoneId / "firewall" / "waf" / "packages" / wafRulePackageId / "rules" / wafRuleId))
}
