package com.dwolla.cloudflare

import cats.*
import cats.tagless.aop.*
import com.dwolla.cloudflare.domain.model.*
import com.dwolla.cloudflare.domain.model.wafrules.*
import com.dwolla.tracing.LowPriorityTraceableValueInstances.*
import natchez.TraceableValue

trait WafRuleClientInstances:
  given Aspect[WafRuleClient, TraceableValue, TraceableValue] =
    new Aspect[WafRuleClient, TraceableValue, TraceableValue]:
      override def weave[F[_]](af: WafRuleClient[F]): WafRuleClient[[a] =>> Aspect.Weave[F, TraceableValue, TraceableValue, a]] =
        new WafRuleClient[[a] =>> Aspect.Weave[F, TraceableValue, TraceableValue, a]]:
          override def list(zoneId: ZoneId, wafRulePackageId: WafRulePackageId): Aspect.Weave[F, TraceableValue, TraceableValue, WafRule] =
            Aspect.Weave(
              "WafRuleClient",
              List(List(
                Aspect.Advice.byValue("zoneId", zoneId),
                Aspect.Advice.byValue("wafRulePackageId", wafRulePackageId),
              )),
              Aspect.Advice("list", af.list(zoneId, wafRulePackageId))
            )

          override def getById(zoneId: ZoneId, wafRulePackageId: WafRulePackageId, wafRuleId: WafRuleId): Aspect.Weave[F, TraceableValue, TraceableValue, WafRule] =
            Aspect.Weave(
              "WafRuleClient",
              List(List(
                Aspect.Advice.byValue("zoneId", zoneId),
                Aspect.Advice.byValue("wafRulePackageId", wafRulePackageId),
                Aspect.Advice.byValue("wafRuleId", wafRuleId),
              )),
              Aspect.Advice("getById", af.getById(zoneId, wafRulePackageId, wafRuleId))
            )

          override def setMode(zoneId: ZoneId, wafRulePackageId: WafRulePackageId, wafRuleId: WafRuleId, mode: Mode): Aspect.Weave[F, TraceableValue, TraceableValue, WafRule] =
            Aspect.Weave(
              "WafRuleClient",
              List(List(
                Aspect.Advice.byValue("zoneId", zoneId),
                Aspect.Advice.byValue("wafRulePackageId", wafRulePackageId),
                Aspect.Advice.byValue("wafRuleId", wafRuleId),
                Aspect.Advice.byValue("mode", mode),
              )),
              Aspect.Advice("setMode", af.setMode(zoneId, wafRulePackageId, wafRuleId, mode))
            )

          override def getByUri(uri: String): Aspect.Weave[F, TraceableValue, TraceableValue, WafRule] =
            Aspect.Weave(
              "WafRuleClient",
              List(List(
                Aspect.Advice.byValue("uri", uri),
              )),
              Aspect.Advice("getByUri", af.getByUri(uri))
            )

      override def mapK[F[_], G[_]](af: WafRuleClient[F])(fk: F ~> G): WafRuleClient[G] =
        new WafRuleClient[G]:
          override def list(zoneId: ZoneId, wafRulePackageId: WafRulePackageId): G[WafRule] =
            fk(af.list(zoneId, wafRulePackageId))

          override def getById(zoneId: ZoneId, wafRulePackageId: WafRulePackageId, wafRuleId: WafRuleId): G[WafRule] =
            fk(af.getById(zoneId, wafRulePackageId, wafRuleId))

          override def setMode(zoneId: ZoneId, wafRulePackageId: WafRulePackageId, wafRuleId: WafRuleId, mode: Mode): G[WafRule] =
            fk(af.setMode(zoneId, wafRulePackageId, wafRuleId, mode))

          override def getByUri(uri: String): G[WafRule] =
            fk(af.getByUri(uri))
