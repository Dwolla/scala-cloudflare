package com.dwolla.cloudflare

import cats._
import cats.syntax.all._
import com.dwolla.cloudflare.domain.model.Exceptions.UnexpectedCloudflareErrorException
import com.dwolla.cloudflare.domain.model._
import com.dwolla.cloudflare.domain.model.wafrulegroups._
import io.circe.literal._
import io.circe.syntax._
import fs2._
import org.http4s.Method._
import org.http4s.Request
import org.http4s.circe._
import org.http4s.client.dsl.Http4sClientDsl

import scala.util.matching.Regex

trait WafRuleGroupClient[F[_]] {
  def list(zoneId: ZoneId, wafRulePackageId: WafRulePackageId): Stream[F, WafRuleGroup]
  def getById(zoneId: ZoneId, wafRulePackageId: WafRulePackageId, wafRuleGroupId: WafRuleGroupId): Stream[F, WafRuleGroup]
  def setMode(zoneId: ZoneId, wafRulePackageId: WafRulePackageId, wafRuleGroupId: WafRuleGroupId, mode: Mode): Stream[F, WafRuleGroup]
  def getRuleGroupId(zoneId: ZoneId, wafRulePackageId: WafRulePackageId, name: WafRuleGroupName): Stream[F, WafRuleGroupId]

  def getByUri(uri: String): Stream[F, WafRuleGroup] = parseUri(uri).fold(Stream.empty.covaryAll[F, WafRuleGroup]) {
    case (zoneId, wafRulePackageId, wafRuleGroupId) => getById(zoneId, wafRulePackageId, wafRuleGroupId)
  }

  def parseUri(uri: String): Option[(ZoneId, WafRulePackageId, WafRuleGroupId)] = uri match {
    case WafRuleGroupClient.uriRegex(zoneId, wafRulePackageId, wafRuleGroupId) => Option((tagZoneId(zoneId), tagWafRulePackageId(wafRulePackageId), tagWafRuleGroupId(wafRuleGroupId)))
    case _ => None
  }

  def buildUri(zoneId: ZoneId, wafRulePackageId: WafRulePackageId, wafRuleGroupId: WafRuleGroupId): String =
    s"https://api.cloudflare.com/client/v4/zones/$zoneId/firewall/waf/packages/$wafRulePackageId/groups/$wafRuleGroupId"
}

object WafRuleGroupClient {
  def apply[F[_] : ApplicativeThrow](executor: StreamingCloudflareApiExecutor[F]): WafRuleGroupClient[F] = new WafRuleGroupClientImpl[F](executor)

  val uriRegex: Regex = """https://api.cloudflare.com/client/v4/zones/(.+?)/firewall/waf/packages/(.+)/groups/(.+)""".r
}

class WafRuleGroupClientImpl[F[_] : ApplicativeThrow](executor: StreamingCloudflareApiExecutor[F]) extends WafRuleGroupClient[F] with Http4sClientDsl[F] {
  private def fetch(req: Request[F]): Stream[F, WafRuleGroup] =
    executor.fetch[WafRuleGroup](req)

  override def list(zoneId: ZoneId, wafRulePackageId: WafRulePackageId): Stream[F, WafRuleGroup] =
    fetch(GET(BaseUrl / "zones" / zoneId / "firewall" / "waf" / "packages" / wafRulePackageId / "groups"))

  override def getById(zoneId: ZoneId, wafRulePackageId: WafRulePackageId, wafRuleGroupId: WafRuleGroupId): Stream[F, WafRuleGroup] =
    fetch(GET(BaseUrl / "zones" / zoneId / "firewall" / "waf" / "packages" / wafRulePackageId / "groups" / wafRuleGroupId))

  override def setMode(zoneId: ZoneId, wafRulePackageId: WafRulePackageId, wafRuleGroupId: WafRuleGroupId, mode: Mode): Stream[F, WafRuleGroup] =
    for {
      res <- executor.fetch[WafRuleGroup](PATCH(json"""{"mode" : ${mode.asJson}}""", BaseUrl / "zones" / zoneId / "firewall" / "waf" / "packages" / wafRulePackageId / "groups" / wafRuleGroupId)).last.recover {
        case ex: UnexpectedCloudflareErrorException if ex.errors.flatMap(_.code.toSeq).exists(alreadyEnabledOrDisabledCodes.contains) =>
          None
      }.flatMap(_.fold(getById(zoneId, wafRulePackageId, wafRuleGroupId))(Stream.emit(_)))
    } yield res

  override def getRuleGroupId(zoneId: ZoneId, wafRulePackageId: WafRulePackageId, name: WafRuleGroupName): Stream[F, WafRuleGroupId] =
    fetch(GET(BaseUrl / "zones" / zoneId / "firewall" / "waf" / "packages" / wafRulePackageId / "groups" +*? name))
        .map(_.id)
        .collect {
          case id => id
        }

  private val alreadyEnabledOrDisabledCodes = List(1019, 1020)
}
