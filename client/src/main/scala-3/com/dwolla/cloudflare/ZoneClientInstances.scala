package com.dwolla.cloudflare

import cats.*
import cats.tagless.aop.*
import com.dwolla.tracing.LowPriorityTraceableValueInstances.*
import natchez.*

trait ZoneClientInstances:
  given Aspect[ZoneClient, TraceableValue, TraceableValue] =
    new Aspect[ZoneClient, TraceableValue, TraceableValue]:
      override def weave[F[_]](af: ZoneClient[F]): ZoneClient[[a] =>> Aspect.Weave[F, TraceableValue, TraceableValue, a]] =
        new ZoneClient[[a] =>> Aspect.Weave[F, TraceableValue, TraceableValue, a]]:
          override def getZoneId(domain: String): Aspect.Weave[F, TraceableValue, TraceableValue, com.dwolla.cloudflare.domain.model.ZoneId] =
            Aspect.Weave(
              "ZoneClient",
              List(List(
                Aspect.Advice.byValue("domain", domain),
              )),
              Aspect.Advice("getZoneId", af.getZoneId(domain))
            )

      override def mapK[F[_], G[_]](af: ZoneClient[F])(fk: F ~> G): ZoneClient[G] =
        new ZoneClient[G]:
          override def getZoneId(domain: String): G[com.dwolla.cloudflare.domain.model.ZoneId] =
            fk(af.getZoneId(domain))
