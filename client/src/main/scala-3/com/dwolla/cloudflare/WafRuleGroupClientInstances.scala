package com.dwolla.cloudflare

import cats.*
import cats.tagless.aop.*
import com.dwolla.cloudflare.domain.model.*
import com.dwolla.cloudflare.domain.model.wafrulegroups.*
import com.dwolla.tracing.LowPriorityTraceableValueInstances.*
import natchez.TraceableValue

trait WafRuleGroupClientInstances:
  given Aspect[WafRuleGroupClient, TraceableValue, TraceableValue] =
    new Aspect[WafRuleGroupClient, TraceableValue, TraceableValue]:
      override def weave[F[_]](af: WafRuleGroupClient[F]): WafRuleGroupClient[[a] =>> Aspect.Weave[F, TraceableValue, TraceableValue, a]] =
        new WafRuleGroupClient[[a] =>> Aspect.Weave[F, TraceableValue, TraceableValue, a]]:
          override def list(zoneId: ZoneId, wafRulePackageId: WafRulePackageId): Aspect.Weave[F, TraceableValue, TraceableValue, WafRuleGroup] =
            Aspect.Weave(
              "WafRuleGroupClient",
              List(List(
                Aspect.Advice.byValue("zoneId", zoneId),
                Aspect.Advice.byValue("wafRulePackageId", wafRulePackageId),
              )),
              Aspect.Advice("list", af.list(zoneId, wafRulePackageId))
            )

          override def getById(zoneId: ZoneId, wafRulePackageId: WafRulePackageId, wafRuleGroupId: WafRuleGroupId): Aspect.Weave[F, TraceableValue, TraceableValue, WafRuleGroup] =
            Aspect.Weave(
              "WafRuleGroupClient",
              List(List(
                Aspect.Advice.byValue("zoneId", zoneId),
                Aspect.Advice.byValue("wafRulePackageId", wafRulePackageId),
                Aspect.Advice.byValue("wafRuleGroupId", wafRuleGroupId),
              )),
              Aspect.Advice("getById", af.getById(zoneId, wafRulePackageId, wafRuleGroupId))
            )

          override def setMode(zoneId: ZoneId, wafRulePackageId: WafRulePackageId, wafRuleGroupId: WafRuleGroupId, mode: Mode): Aspect.Weave[F, TraceableValue, TraceableValue, WafRuleGroup] =
            Aspect.Weave(
              "WafRuleGroupClient",
              List(List(
                Aspect.Advice.byValue("zoneId", zoneId),
                Aspect.Advice.byValue("wafRulePackageId", wafRulePackageId),
                Aspect.Advice.byValue("wafRuleGroupId", wafRuleGroupId),
                Aspect.Advice.byValue("mode", mode),
              )),
              Aspect.Advice("setMode", af.setMode(zoneId, wafRulePackageId, wafRuleGroupId, mode))
            )

          override def getRuleGroupId(zoneId: ZoneId, wafRulePackageId: WafRulePackageId, name: WafRuleGroupName): Aspect.Weave[F, TraceableValue, TraceableValue, WafRuleGroupId] =
            Aspect.Weave(
              "WafRuleGroupClient",
              List(List(
                Aspect.Advice.byValue("zoneId", zoneId),
                Aspect.Advice.byValue("wafRulePackageId", wafRulePackageId),
                Aspect.Advice.byValue("name", name),
              )),
              Aspect.Advice("getRuleGroupId", af.getRuleGroupId(zoneId, wafRulePackageId, name))
            )

          override def getByUri(uri: String): Aspect.Weave[F, TraceableValue, TraceableValue, WafRuleGroup] =
            Aspect.Weave(
              "WafRuleGroupClient",
              List(List(
                Aspect.Advice.byValue("uri", uri),
              )),
              Aspect.Advice("getByUri", af.getByUri(uri))
            )

      override def mapK[F[_], G[_]](af: WafRuleGroupClient[F])(fk: F ~> G): WafRuleGroupClient[G] =
        new WafRuleGroupClient[G]:
          override def list(zoneId: ZoneId, wafRulePackageId: WafRulePackageId): G[WafRuleGroup] =
            fk(af.list(zoneId, wafRulePackageId))

          override def getById(zoneId: ZoneId, wafRulePackageId: WafRulePackageId, wafRuleGroupId: WafRuleGroupId): G[WafRuleGroup] =
            fk(af.getById(zoneId, wafRulePackageId, wafRuleGroupId))

          override def setMode(zoneId: ZoneId, wafRulePackageId: WafRulePackageId, wafRuleGroupId: WafRuleGroupId, mode: Mode): G[WafRuleGroup] =
            fk(af.setMode(zoneId, wafRulePackageId, wafRuleGroupId, mode))

          override def getRuleGroupId(zoneId: ZoneId, wafRulePackageId: WafRulePackageId, name: WafRuleGroupName): G[WafRuleGroupId] =
            fk(af.getRuleGroupId(zoneId, wafRulePackageId, name))

          override def getByUri(uri: String): G[WafRuleGroup] =
            fk(af.getByUri(uri))
