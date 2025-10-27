package com.dwolla.cloudflare

import cats.*
import cats.effect.{Trace as _, *}
import com.dwolla.cloudflare.domain.model.wafrulepackages._
import com.dwolla.cloudflare.domain.model.{WafRulePackageId, ZoneId, tagWafRulePackageId, tagZoneId}
import com.dwolla.tagless.*
import com.dwolla.tracing.syntax.*
import io.circe.literal._
import io.circe.syntax._
import fs2.*
import org.http4s.Method.*
import org.http4s.{Request, Uri}
import org.http4s.circe._
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.syntax.all.*

import scala.util.matching.Regex

trait WafRulePackageClient[F[_]] {
  def list(zoneId: ZoneId): F[WafRulePackage]
  def getById(zoneId: ZoneId, wafRulePackageId: WafRulePackageId): F[WafRulePackage]
  def edit(zoneId: ZoneId, wafRulePackageId: WafRulePackageId, sensitivity: Sensitivity, actionMode: ActionMode): F[WafRulePackage]
  def getRulePackageId(zoneId: ZoneId, name: WafRulePackageName): F[WafRulePackageId]
  def getByUri(uri: String): F[WafRulePackage]

  def parseUri(uri: String): Option[(ZoneId, WafRulePackageId)] = uri match {
    case WafRulePackageClient.uriRegex(zoneId, wafRulePackageId) => Option((tagZoneId(zoneId), tagWafRulePackageId(wafRulePackageId)))
    case _ => None
  }

  def buildUri(zoneId: ZoneId, wafRulePackageId: WafRulePackageId): Uri =
    uri"https://api.cloudflare.com/client/v4/zones" / zoneId / "firewall" / "waf" / "packages" / wafRulePackageId
}

object WafRulePackageClient extends WafRulePackageClientInstances {
  def apply[F[_] : MonadCancelThrow : natchez.Trace](executor: StreamingCloudflareApiExecutor[F]): WafRulePackageClient[Stream[F, *]] =
    apply(executor, _.traceWithInputsAndOutputs)

  def apply[F[_]](executor: StreamingCloudflareApiExecutor[F],
                  transform: WafRulePackageClient[Stream[F, *]] => WafRulePackageClient[Stream[F, *]]): WafRulePackageClient[Stream[F, *]] =
    WeaveKnot(knot(executor))(transform)

  private def knot[F[_]](executor: StreamingCloudflareApiExecutor[F]): Eval[WafRulePackageClient[Stream[F, *]]] => WafRulePackageClient[Stream[F, *]] =
    new WafRulePackageClientImpl[F](executor, _)

  val uriRegex: Regex = """https://api.cloudflare.com/client/v4/zones/(.+?)/firewall/waf/packages/(.+)""".r
}

private class WafRulePackageClientImpl[F[_]](executor: StreamingCloudflareApiExecutor[F],
                                             self: Eval[WafRulePackageClient[Stream[F, *]]])
  extends WafRulePackageClient[Stream[F, *]]
    with Http4sClientDsl[F] {
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

  override def getByUri(uri: String): Stream[F, WafRulePackage] =
    parseUri(uri).fold(MonoidK[Stream[F, *]].empty[WafRulePackage]) {
      case (zoneId, wafRulePackageId) => self.value.getById(zoneId, wafRulePackageId)
    }
}
