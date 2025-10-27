package com.dwolla.cloudflare

import cats.*
import cats.tagless.aop.*
import com.dwolla.cloudflare.domain.model.*
import com.dwolla.cloudflare.domain.model.pagerules.*
import com.dwolla.tracing.LowPriorityTraceableValueInstances.*
import natchez.TraceableValue

trait PageRuleClientInstances:
  given Aspect[PageRuleClient, TraceableValue, TraceableValue] =
    new Aspect[PageRuleClient, TraceableValue, TraceableValue]:
      override def weave[F[_]](af: PageRuleClient[F]): PageRuleClient[[a] =>> Aspect.Weave[F, TraceableValue, TraceableValue, a]] =
        new PageRuleClient[[a] =>> Aspect.Weave[F, TraceableValue, TraceableValue, a]]:
          override def list(zoneId: ZoneId): Aspect.Weave[F, TraceableValue, TraceableValue, PageRule] =
            Aspect.Weave(
              "PageRuleClient",
              List(List(
                Aspect.Advice.byValue("zoneId", zoneId),
              )),
              Aspect.Advice("list", af.list(zoneId))
            )

          override def getById(zoneId: ZoneId, pageRuleId: String): Aspect.Weave[F, TraceableValue, TraceableValue, PageRule] =
            Aspect.Weave(
              "PageRuleClient",
              List(List(
                Aspect.Advice.byValue("zoneId", zoneId),
                Aspect.Advice.byValue("pageRuleId", pageRuleId),
              )),
              Aspect.Advice("getById", af.getById(zoneId, pageRuleId))
            )

          override def create(zoneId: ZoneId, pageRule: PageRule): Aspect.Weave[F, TraceableValue, TraceableValue, PageRule] =
            Aspect.Weave(
              "PageRuleClient",
              List(List(
                Aspect.Advice.byValue("zoneId", zoneId),
                Aspect.Advice.byValue("pageRule", pageRule),
              )),
              Aspect.Advice("create", af.create(zoneId, pageRule))
            )

          override def update(zoneId: ZoneId, pageRule: PageRule): Aspect.Weave[F, TraceableValue, TraceableValue, PageRule] =
            Aspect.Weave(
              "PageRuleClient",
              List(List(
                Aspect.Advice.byValue("zoneId", zoneId),
                Aspect.Advice.byValue("pageRule", pageRule),
              )),
              Aspect.Advice("update", af.update(zoneId, pageRule))
            )

          override def delete(zoneId: ZoneId, pageRuleId: String): Aspect.Weave[F, TraceableValue, TraceableValue, PageRuleId] =
            Aspect.Weave(
              "PageRuleClient",
              List(List(
                Aspect.Advice.byValue("zoneId", zoneId),
                Aspect.Advice.byValue("pageRuleId", pageRuleId),
              )),
              Aspect.Advice("delete", af.delete(zoneId, pageRuleId))
            )

          override def getByUri(uri: String): Aspect.Weave[F, TraceableValue, TraceableValue, PageRule] =
            Aspect.Weave(
              "PageRuleClient",
              List(List(
                Aspect.Advice.byValue("uri", uri),
              )),
              Aspect.Advice("getByUri", af.getByUri(uri))
            )

      override def mapK[F[_], G[_]](af: PageRuleClient[F])(fk: F ~> G): PageRuleClient[G] =
        new PageRuleClient[G]:
          override def list(zoneId: ZoneId): G[PageRule] = fk(af.list(zoneId))
          override def getById(zoneId: ZoneId, pageRuleId: String): G[PageRule] = fk(af.getById(zoneId, pageRuleId))
          override def create(zoneId: ZoneId, pageRule: PageRule): G[PageRule] = fk(af.create(zoneId, pageRule))
          override def update(zoneId: ZoneId, pageRule: PageRule): G[PageRule] = fk(af.update(zoneId, pageRule))
          override def delete(zoneId: ZoneId, pageRuleId: String): G[PageRuleId] = fk(af.delete(zoneId, pageRuleId))
          override def getByUri(uri: String): G[PageRule] = fk(af.getByUri(uri))
