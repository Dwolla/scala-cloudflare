package com.dwolla.cloudflare

import cats.*
import cats.effect.{Trace as _, *}
import cats.syntax.all.*
import com.dwolla.cloudflare.domain.model.Exceptions.UnexpectedCloudflareErrorException
import com.dwolla.cloudflare.domain.model.wafrules.*
import com.dwolla.cloudflare.domain.model.{WafRulePackageId, ZoneId, tagWafRulePackageId, tagZoneId}
import com.dwolla.tagless.*
import com.dwolla.tracing.syntax.*
import io.circe.literal.*
import io.circe.syntax.*
import fs2.*
import org.http4s.Method.*
import org.http4s.{Request, Uri}
import org.http4s.circe.*
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.syntax.all.*

import scala.util.matching.Regex

trait WafRuleClient[F[_]] {
  def list(zoneId: ZoneId, wafRulePackageId: WafRulePackageId): F[WafRule]
  def getById(zoneId: ZoneId, wafRulePackageId: WafRulePackageId, wafRuleId: WafRuleId): F[WafRule]
  def setMode(zoneId: ZoneId, wafRulePackageId: WafRulePackageId, wafRuleId: WafRuleId, mode: Mode): F[WafRule]
  def getByUri(uri: String): F[WafRule]

  def parseUri(uri: String): Option[(ZoneId, WafRulePackageId, WafRuleId)] = uri match {
    case WafRuleClient.uriRegex(zoneId, wafRulePackageId, wafRuleId) => Option((tagZoneId(zoneId), tagWafRulePackageId(wafRulePackageId), tagWafRuleId(wafRuleId)))
    case _ => None
  }

  def buildUri(zoneId: ZoneId, wafRulePackageId: WafRulePackageId, wafRuleId: WafRuleId): Uri =
    uri"https://api.cloudflare.com/client/v4/zones" / zoneId / "firewall" / "waf" / "packages" / wafRulePackageId / "rules" / wafRuleId
}

object WafRuleClient extends WafRuleClientInstances {
  def apply[F[_] : MonadCancelThrow : natchez.Trace](executor: StreamingCloudflareApiExecutor[F]): WafRuleClient[Stream[F, *]] =
    apply(executor, _.traceWithInputsAndOutputs)

  def apply[F[_] : ApplicativeThrow](executor: StreamingCloudflareApiExecutor[F],
                                     transform: WafRuleClient[Stream[F, *]] => WafRuleClient[Stream[F, *]]): WafRuleClient[Stream[F, *]] =
    WeaveKnot(knot(executor))(transform)

  private def knot[F[_] : ApplicativeThrow](executor: StreamingCloudflareApiExecutor[F]): Eval[WafRuleClient[Stream[F, *]]] => WafRuleClient[Stream[F, *]] =
    new WafRuleClientImpl[F](executor, _)

  val uriRegex: Regex = """https://api.cloudflare.com/client/v4/zones/(.+?)/firewall/waf/packages/(.+)/rules/(.+)""".r
}

private class WafRuleClientImpl[F[_] : ApplicativeThrow](executor: StreamingCloudflareApiExecutor[F],
                                                         self: Eval[WafRuleClient[Stream[F, *]]])
  extends WafRuleClient[Stream[F, *]] with Http4sClientDsl[F] {
  private def fetch(req: Request[F]): Stream[F, WafRule] =
    executor.fetch[WafRule](req)

  override def list(zoneId: ZoneId, wafRulePackageId: WafRulePackageId): Stream[F, WafRule] =
    fetch(GET(BaseUrl / "zones" / zoneId / "firewall" / "waf" / "packages" / wafRulePackageId / "rules"))

  override def getById(zoneId: ZoneId, wafRulePackageId: WafRulePackageId, wafRuleId: WafRuleId): Stream[F, WafRule] =
    fetch(GET(BaseUrl / "zones" / zoneId / "firewall" / "waf" / "packages" / wafRulePackageId / "rules" / wafRuleId))

  override def setMode(zoneId: ZoneId, wafRulePackageId: WafRulePackageId, wafRuleId: WafRuleId, mode: Mode): Stream[F, WafRule] =
    executor
      .fetch[WafRule](PATCH(json"""{"mode" : ${mode.asJson}}""", BaseUrl / "zones" / zoneId / "firewall" / "waf" / "packages" / wafRulePackageId / "rules" / wafRuleId))
      .last
      .unNone
      .recoverWith {
        case ex: UnexpectedCloudflareErrorException if ex.errors.flatMap(_.code.toSeq).exists(alreadyEnabledOrDisabledCodes.contains) => Stream.empty
      }
      .orIfEmpty(self.value.getById(zoneId, wafRulePackageId, wafRuleId))

  override def getByUri(uri: String): Stream[F, WafRule] =
    parseUri(uri).fold(MonoidK[Stream[F, *]].empty[WafRule]) {
      (self.value.getById _).tupled
    }

  private val alreadyEnabledOrDisabledCodes = List(1008, 1009)
}
