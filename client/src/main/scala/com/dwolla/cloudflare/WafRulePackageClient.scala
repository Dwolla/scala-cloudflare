package com.dwolla.cloudflare

import com.dwolla.cloudflare.domain.model.wafrulepackages._
import com.dwolla.cloudflare.domain.model.{WafRulePackageId, ZoneId, tagWafRulePackageId, tagZoneId}
import io.circe.literal._
import io.circe.syntax._
import fs2._
import org.http4s.Method._
import org.http4s.Request
import org.http4s.circe._
import org.http4s.client.dsl.Http4sClientDsl

import scala.util.matching.Regex

trait WafRulePackageClient[F[_]] {
  def list(zoneId: ZoneId): Stream[F, WafRulePackage]
  def getById(zoneId: ZoneId, wafRulePackageId: WafRulePackageId): Stream[F, WafRulePackage]
  def edit(zoneId: ZoneId, wafRulePackageId: WafRulePackageId, sensitivity: Sensitivity, actionMode: ActionMode): Stream[F, WafRulePackage]
  def getRulePackageId(zoneId: ZoneId, name: WafRulePackageName): Stream[F, WafRulePackageId]

  def getByUri(uri: String): Stream[F, WafRulePackage] = parseUri(uri).fold(Stream.empty.covaryAll[F, WafRulePackage]) {
    case (zoneId, wafRulePackageId) => getById(zoneId, wafRulePackageId)
  }

  def parseUri(uri: String): Option[(ZoneId, WafRulePackageId)] = uri match {
    case WafRulePackageClient.uriRegex(zoneId, wafRulePackageId) => Option((tagZoneId(zoneId), tagWafRulePackageId(wafRulePackageId)))
    case _ => None
  }

  def buildUri(zoneId: ZoneId, wafRulePackageId: WafRulePackageId): String =
    s"https://api.cloudflare.com/client/v4/zones/$zoneId/firewall/waf/packages/$wafRulePackageId"
}

object WafRulePackageClient {
  def apply[F[_]](executor: StreamingCloudflareApiExecutor[F]): WafRulePackageClient[F] = new WafRulePackageClientImpl[F](executor)

  val uriRegex: Regex = """https://api.cloudflare.com/client/v4/zones/(.+?)/firewall/waf/packages/(.+)""".r
}

class WafRulePackageClientImpl[F[_]](executor: StreamingCloudflareApiExecutor[F]) extends WafRulePackageClient[F] with Http4sClientDsl[F] {
  private def fetch(req: Request[F]): Stream[F, WafRulePackage] =
    executor.fetch[WafRulePackage](req)

  override def list(zoneId: ZoneId): Stream[F, WafRulePackage] =
    fetch(GET(BaseUrl / "zones" / zoneId / "firewall" / "waf" / "packages"))

  override def getById(zoneId: ZoneId, wafRulePackageId: WafRulePackageId): Stream[F, WafRulePackage] =
    fetch(GET(BaseUrl / "zones" / zoneId / "firewall" / "waf" / "packages" / wafRulePackageId))

  override def edit(zoneId: ZoneId, wafRulePackageId: WafRulePackageId, sensitivity: Sensitivity, actionMode: ActionMode): Stream[F, WafRulePackage] =
    fetch(PATCH(json"""{"sensitivity": ${sensitivity.asJson}, "action_mode": ${actionMode.asJson}}""", BaseUrl / "zones" / zoneId / "firewall" / "waf" / "packages" / wafRulePackageId))

  override def getRulePackageId(zoneId: ZoneId, name: WafRulePackageName): Stream[F, WafRulePackageId] =
    fetch(GET(BaseUrl / "zones" / zoneId / "firewall" / "waf" / "packages" +*? name))
        .map(_.id)
        .collect {
           case id => id
        }
}
