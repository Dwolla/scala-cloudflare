package com.dwolla.cloudflare

import cats.*
import cats.tagless.aop.*
import com.dwolla.cloudflare.domain.model.*
import com.dwolla.cloudflare.domain.model.ratelimits.*
import com.dwolla.tracing.LowPriorityTraceableValueInstances.*
import natchez.TraceableValue

trait RateLimitClientInstances:
  given Aspect[RateLimitClient, TraceableValue, TraceableValue] =
    new Aspect[RateLimitClient, TraceableValue, TraceableValue]:
      override def weave[F[_]](af: RateLimitClient[F]): RateLimitClient[[a] =>> Aspect.Weave[F, TraceableValue, TraceableValue, a]] =
        new RateLimitClient[[a] =>> Aspect.Weave[F, TraceableValue, TraceableValue, a]]:
          override def list(zoneId: ZoneId): Aspect.Weave[F, TraceableValue, TraceableValue, RateLimit] =
            Aspect.Weave(
              "RateLimitClient",
              List(List(
                Aspect.Advice.byValue("zoneId", zoneId),
              )),
              Aspect.Advice("list", af.list(zoneId))
            )

          override def getById(zoneId: ZoneId, rateLimitId: String): Aspect.Weave[F, TraceableValue, TraceableValue, RateLimit] =
            Aspect.Weave(
              "RateLimitClient",
              List(List(
                Aspect.Advice.byValue("zoneId", zoneId),
                Aspect.Advice.byValue("rateLimitId", rateLimitId),
              )),
              Aspect.Advice("getById", af.getById(zoneId, rateLimitId))
            )

          override def create(zoneId: ZoneId, rateLimit: RateLimit): Aspect.Weave[F, TraceableValue, TraceableValue, RateLimit] =
            Aspect.Weave(
              "RateLimitClient",
              List(List(
                Aspect.Advice.byValue("zoneId", zoneId),
                Aspect.Advice.byValue("rateLimit", rateLimit),
              )),
              Aspect.Advice("create", af.create(zoneId, rateLimit))
            )

          override def update(zoneId: ZoneId, rateLimit: RateLimit): Aspect.Weave[F, TraceableValue, TraceableValue, RateLimit] =
            Aspect.Weave(
              "RateLimitClient",
              List(List(
                Aspect.Advice.byValue("zoneId", zoneId),
                Aspect.Advice.byValue("rateLimit", rateLimit),
              )),
              Aspect.Advice("update", af.update(zoneId, rateLimit))
            )

          override def delete(zoneId: ZoneId, rateLimitId: String): Aspect.Weave[F, TraceableValue, TraceableValue, RateLimitId] =
            Aspect.Weave(
              "RateLimitClient",
              List(List(
                Aspect.Advice.byValue("zoneId", zoneId),
                Aspect.Advice.byValue("rateLimitId", rateLimitId),
              )),
              Aspect.Advice("delete", af.delete(zoneId, rateLimitId))
            )

          override def getByUri(uri: String): Aspect.Weave[F, TraceableValue, TraceableValue, RateLimit] =
            Aspect.Weave(
              "RateLimitClient",
              List(List(
                Aspect.Advice.byValue("uri", uri),
              )),
              Aspect.Advice("getByUri", af.getByUri(uri))
            )

      override def mapK[F[_], G[_]](af: RateLimitClient[F])(fk: F ~> G): RateLimitClient[G] =
        new RateLimitClient[G]:
          override def list(zoneId: ZoneId): G[RateLimit] = fk(af.list(zoneId))
          override def getById(zoneId: ZoneId, rateLimitId: String): G[RateLimit] = fk(af.getById(zoneId, rateLimitId))
          override def create(zoneId: ZoneId, rateLimit: RateLimit): G[RateLimit] = fk(af.create(zoneId, rateLimit))
          override def update(zoneId: ZoneId, rateLimit: RateLimit): G[RateLimit] = fk(af.update(zoneId, rateLimit))
          override def delete(zoneId: ZoneId, rateLimitId: String): G[RateLimitId] = fk(af.delete(zoneId, rateLimitId))
          override def getByUri(uri: String): G[RateLimit] = fk(af.getByUri(uri))
