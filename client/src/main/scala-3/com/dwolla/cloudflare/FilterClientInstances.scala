package com.dwolla.cloudflare

import cats.*
import cats.tagless.aop.*
import com.dwolla.cloudflare.domain.model.*
import com.dwolla.cloudflare.domain.model.filters.*
import com.dwolla.tracing.LowPriorityTraceableValueInstances.*
import natchez.TraceableValue

trait FilterClientInstances:
  given Aspect[FilterClient, TraceableValue, TraceableValue] =
    new Aspect[FilterClient, TraceableValue, TraceableValue]:
      override def weave[F[_]](af: FilterClient[F]): FilterClient[[a] =>> Aspect.Weave[F, TraceableValue, TraceableValue, a]] =
        new FilterClient[[a] =>> Aspect.Weave[F, TraceableValue, TraceableValue, a]]:
          override def list(zoneId: ZoneId): Aspect.Weave[F, TraceableValue, TraceableValue, Filter] =
            Aspect.Weave(
              "FilterClient",
              List(List(
                Aspect.Advice.byValue("zoneId", zoneId),
              )),
              Aspect.Advice("list", af.list(zoneId))
            )

          override def getById(zoneId: ZoneId, filterId: String): Aspect.Weave[F, TraceableValue, TraceableValue, Filter] =
            Aspect.Weave(
              "FilterClient",
              List(List(
                Aspect.Advice.byValue("zoneId", zoneId),
                Aspect.Advice.byValue("filterId", filterId),
              )),
              Aspect.Advice("getById", af.getById(zoneId, filterId))
            )

          override def create(zoneId: ZoneId, filter: Filter): Aspect.Weave[F, TraceableValue, TraceableValue, Filter] =
            Aspect.Weave(
              "FilterClient",
              List(List(
                Aspect.Advice.byValue("zoneId", zoneId),
                Aspect.Advice.byValue("filter", filter),
              )),
              Aspect.Advice("create", af.create(zoneId, filter))
            )

          override def update(zoneId: ZoneId, filter: Filter): Aspect.Weave[F, TraceableValue, TraceableValue, Filter] =
            Aspect.Weave(
              "FilterClient",
              List(List(
                Aspect.Advice.byValue("zoneId", zoneId),
                Aspect.Advice.byValue("filter", filter),
              )),
              Aspect.Advice("update", af.update(zoneId, filter))
            )

          override def delete(zoneId: ZoneId, filterId: String): Aspect.Weave[F, TraceableValue, TraceableValue, FilterId] =
            Aspect.Weave(
              "FilterClient",
              List(List(
                Aspect.Advice.byValue("zoneId", zoneId),
                Aspect.Advice.byValue("filterId", filterId),
              )),
              Aspect.Advice("delete", af.delete(zoneId, filterId))
            )

          override def getByUri(uri: String): Aspect.Weave[F, TraceableValue, TraceableValue, Filter] =
            Aspect.Weave(
              "FilterClient",
              List(List(
                Aspect.Advice.byValue("uri", uri),
              )),
              Aspect.Advice("getByUri", af.getByUri(uri))
            )

      override def mapK[F[_], G[_]](af: FilterClient[F])(fk: F ~> G): FilterClient[G] =
        new FilterClient[G]:
          override def list(zoneId: ZoneId): G[Filter] =
            fk(af.list(zoneId))

          override def getById(zoneId: ZoneId, filterId: String): G[Filter] =
            fk(af.getById(zoneId, filterId))

          override def create(zoneId: ZoneId, filter: Filter): G[Filter] =
            fk(af.create(zoneId, filter))

          override def update(zoneId: ZoneId, filter: Filter): G[Filter] =
            fk(af.update(zoneId, filter))

          override def delete(zoneId: ZoneId, filterId: String): G[FilterId] =
            fk(af.delete(zoneId, filterId))

          override def getByUri(uri: String): G[Filter] =
            fk(af.getByUri(uri))
