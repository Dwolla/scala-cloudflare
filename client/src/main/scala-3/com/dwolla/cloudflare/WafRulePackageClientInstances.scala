package com.dwolla.cloudflare

import cats.*
import cats.tagless.aop.*
import com.dwolla.cloudflare.domain.model.*
import com.dwolla.cloudflare.domain.model.wafrulepackages.*
import com.dwolla.tracing.LowPriorityTraceableValueInstances.*
import natchez.TraceableValue

trait WafRulePackageClientInstances:
  given Aspect[WafRulePackageClient, TraceableValue, TraceableValue] =
    new Aspect[WafRulePackageClient, TraceableValue, TraceableValue]:
      override def weave[F[_]](af: WafRulePackageClient[F]): WafRulePackageClient[[a] =>> Aspect.Weave[F, TraceableValue, TraceableValue, a]] =
        new WafRulePackageClient[[a] =>> Aspect.Weave[F, TraceableValue, TraceableValue, a]]:
          override def list(zoneId: ZoneId): Aspect.Weave[F, TraceableValue, TraceableValue, WafRulePackage] =
            Aspect.Weave(
              "WafRulePackageClient",
              List(List(
                Aspect.Advice.byValue("zoneId", zoneId),
              )),
              Aspect.Advice("list", af.list(zoneId))
            )

          override def getById(zoneId: ZoneId, wafRulePackageId: WafRulePackageId): Aspect.Weave[F, TraceableValue, TraceableValue, WafRulePackage] =
            Aspect.Weave(
              "WafRulePackageClient",
              List(List(
                Aspect.Advice.byValue("zoneId", zoneId),
                Aspect.Advice.byValue("wafRulePackageId", wafRulePackageId),
              )),
              Aspect.Advice("getById", af.getById(zoneId, wafRulePackageId))
            )

          override def edit(zoneId: ZoneId, wafRulePackageId: WafRulePackageId, sensitivity: Sensitivity, actionMode: ActionMode): Aspect.Weave[F, TraceableValue, TraceableValue, WafRulePackage] =
            Aspect.Weave(
              "WafRulePackageClient",
              List(List(
                Aspect.Advice.byValue("zoneId", zoneId),
                Aspect.Advice.byValue("wafRulePackageId", wafRulePackageId),
                Aspect.Advice.byValue("sensitivity", sensitivity),
                Aspect.Advice.byValue("actionMode", actionMode),
              )),
              Aspect.Advice("edit", af.edit(zoneId, wafRulePackageId, sensitivity, actionMode))
            )

          override def getRulePackageId(zoneId: ZoneId, name: WafRulePackageName): Aspect.Weave[F, TraceableValue, TraceableValue, WafRulePackageId] =
            Aspect.Weave(
              "WafRulePackageClient",
              List(List(
                Aspect.Advice.byValue("zoneId", zoneId),
                Aspect.Advice.byValue("name", name),
              )),
              Aspect.Advice("getRulePackageId", af.getRulePackageId(zoneId, name))
            )

          override def getByUri(uri: String): Aspect.Weave[F, TraceableValue, TraceableValue, WafRulePackage] =
            Aspect.Weave(
              "WafRulePackageClient",
              List(List(
                Aspect.Advice.byValue("uri", uri),
              )),
              Aspect.Advice("getByUri", af.getByUri(uri))
            )

      override def mapK[F[_], G[_]](af: WafRulePackageClient[F])(fk: F ~> G): WafRulePackageClient[G] =
        new WafRulePackageClient[G]:
          override def list(zoneId: ZoneId): G[WafRulePackage] = fk(af.list(zoneId))
          override def getById(zoneId: ZoneId, wafRulePackageId: WafRulePackageId): G[WafRulePackage] = fk(af.getById(zoneId, wafRulePackageId))
          override def edit(zoneId: ZoneId, wafRulePackageId: WafRulePackageId, sensitivity: Sensitivity, actionMode: ActionMode): G[WafRulePackage] = fk(af.edit(zoneId, wafRulePackageId, sensitivity, actionMode))
          override def getRulePackageId(zoneId: ZoneId, name: WafRulePackageName): G[WafRulePackageId] = fk(af.getRulePackageId(zoneId, name))
          override def getByUri(uri: String): G[WafRulePackage] = fk(af.getByUri(uri))
