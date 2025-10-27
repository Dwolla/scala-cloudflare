package com.dwolla.cloudflare

import cats.*
import cats.effect.{Trace as _, *}
import cats.syntax.all.*
import com.dwolla.cloudflare.domain.model.*
import com.dwolla.cloudflare.domain.model.Exceptions.UnexpectedCloudflareErrorException
import com.dwolla.cloudflare.domain.model.wafrulegroups.*
import com.dwolla.tagless.*
import com.dwolla.tracing.syntax.*
import io.circe.literal.*
import io.circe.syntax.*
import fs2.*
import org.http4s.Method.*
import org.http4s.circe.*
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.syntax.all.*
import org.http4s.{Request, Uri}

import scala.util.matching.Regex

trait WafRuleGroupClient[F[_]] {
  def list(zoneId: ZoneId, wafRulePackageId: WafRulePackageId): F[WafRuleGroup]
  def getById(zoneId: ZoneId, wafRulePackageId: WafRulePackageId, wafRuleGroupId: WafRuleGroupId): F[WafRuleGroup]
  def setMode(zoneId: ZoneId, wafRulePackageId: WafRulePackageId, wafRuleGroupId: WafRuleGroupId, mode: Mode): F[WafRuleGroup]
  def getRuleGroupId(zoneId: ZoneId, wafRulePackageId: WafRulePackageId, name: WafRuleGroupName): F[WafRuleGroupId]
  def getByUri(uri: String): F[WafRuleGroup]

  def parseUri(uri: String): Option[(ZoneId, WafRulePackageId, WafRuleGroupId)] = uri match {
    case WafRuleGroupClient.uriRegex(zoneId, wafRulePackageId, wafRuleGroupId) => Option((tagZoneId(zoneId), tagWafRulePackageId(wafRulePackageId), tagWafRuleGroupId(wafRuleGroupId)))
    case _ => None
  }

  def buildUri(zoneId: ZoneId, wafRulePackageId: WafRulePackageId, wafRuleGroupId: WafRuleGroupId): Uri =
    uri"https://api.cloudflare.com/client/v4/zones" / zoneId / "firewall" / "waf" / "packages" / wafRulePackageId / "groups" / wafRuleGroupId
}

object WafRuleGroupClient extends WafRuleGroupClientInstances {
  def apply[F[_] : MonadCancelThrow : natchez.Trace](executor: StreamingCloudflareApiExecutor[F]): WafRuleGroupClient[Stream[F, *]] =
    apply(executor, _.traceWithInputsAndOutputs)

  def apply[F[_] : ApplicativeThrow](executor: StreamingCloudflareApiExecutor[F],
                                     transform: WafRuleGroupClient[Stream[F, *]] => WafRuleGroupClient[Stream[F, *]]): WafRuleGroupClient[Stream[F, *]] =
    WeaveKnot(knot(executor))(transform)

  private def knot[F[_] : ApplicativeThrow](executor: StreamingCloudflareApiExecutor[F]): Eval[WafRuleGroupClient[Stream[F, *]]] => WafRuleGroupClient[Stream[F, *]] =
    new WafRuleGroupClientImpl[F](executor, _)

  val uriRegex: Regex = """https://api.cloudflare.com/client/v4/zones/(.+?)/firewall/waf/packages/(.+)/groups/(.+)""".r
}

private class WafRuleGroupClientImpl[F[_] : ApplicativeThrow](executor: StreamingCloudflareApiExecutor[F],
                                                             self: Eval[WafRuleGroupClient[Stream[F, *]]])
  extends WafRuleGroupClient[Stream[F, *]] with Http4sClientDsl[F] {
  private def fetch(req: Request[F]): Stream[F, WafRuleGroup] =
    executor.fetch[WafRuleGroup](req)

  override def list(zoneId: ZoneId, wafRulePackageId: WafRulePackageId): Stream[F, WafRuleGroup] =
    fetch(GET(BaseUrl / "zones" / zoneId / "firewall" / "waf" / "packages" / wafRulePackageId / "groups"))

  override def getById(zoneId: ZoneId, wafRulePackageId: WafRulePackageId, wafRuleGroupId: WafRuleGroupId): Stream[F, WafRuleGroup] =
    fetch(GET(BaseUrl / "zones" / zoneId / "firewall" / "waf" / "packages" / wafRulePackageId / "groups" / wafRuleGroupId))

  override def setMode(zoneId: ZoneId, wafRulePackageId: WafRulePackageId, wafRuleGroupId: WafRuleGroupId, mode: Mode): Stream[F, WafRuleGroup] =
    executor.fetch[WafRuleGroup](PATCH(json"""{"mode" : ${mode.asJson}}""", BaseUrl / "zones" / zoneId / "firewall" / "waf" / "packages" / wafRulePackageId / "groups" / wafRuleGroupId))
      .last
      .unNone
      .recoverWith {
        case ex: UnexpectedCloudflareErrorException if ex.errors.flatMap(_.code.toSeq).exists(alreadyEnabledOrDisabledCodes.contains) => Stream.empty
      }
      .orIfEmpty(self.value.getById(zoneId, wafRulePackageId, wafRuleGroupId))

  override def getRuleGroupId(zoneId: ZoneId, wafRulePackageId: WafRulePackageId, name: WafRuleGroupName): Stream[F, WafRuleGroupId] =
    fetch(GET(BaseUrl / "zones" / zoneId / "firewall" / "waf" / "packages" / wafRulePackageId / "groups" +*? name))
        .map(_.id)

  override def getByUri(uri: String): Stream[F, WafRuleGroup] =
    parseUri(uri).fold(MonoidK[Stream[F, *]].empty[WafRuleGroup]) {
      (self.value.getById _).tupled
    }

  private val alreadyEnabledOrDisabledCodes = List(1019, 1020)
}
