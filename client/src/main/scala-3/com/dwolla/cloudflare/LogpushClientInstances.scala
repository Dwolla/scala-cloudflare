package com.dwolla.cloudflare

import cats.*
import cats.tagless.aop.*
import com.dwolla.cloudflare.domain.model.*
import com.dwolla.cloudflare.domain.model.logpush.*
import com.dwolla.tracing.LowPriorityTraceableValueInstances.*
import natchez.TraceableValue

trait LogpushClientInstances:
  given Aspect[LogpushClient, TraceableValue, TraceableValue] =
    new Aspect[LogpushClient, TraceableValue, TraceableValue]:
      override def weave[F[_]](af: LogpushClient[F]): LogpushClient[[a] =>> Aspect.Weave[F, TraceableValue, TraceableValue, a]] =
        new LogpushClient[[a] =>> Aspect.Weave[F, TraceableValue, TraceableValue, a]]:
          override def list(zoneId: ZoneId): Aspect.Weave[F, TraceableValue, TraceableValue, LogpushJob] =
            Aspect.Weave(
              "LogpushClient",
              List(List(
                Aspect.Advice.byValue("zoneId", zoneId),
              )),
              Aspect.Advice("list", af.list(zoneId))
            )

          override def createOwnership(zoneId: ZoneId, destination: LogpushDestination): Aspect.Weave[F, TraceableValue, TraceableValue, LogpushOwnership] =
            Aspect.Weave(
              "LogpushClient",
              List(List(
                Aspect.Advice.byValue("zoneId", zoneId),
                Aspect.Advice.byValue("destination", destination),
              )),
              Aspect.Advice("createOwnership", af.createOwnership(zoneId, destination))
            )

          override def createJob(zoneId: ZoneId, job: CreateJob): Aspect.Weave[F, TraceableValue, TraceableValue, LogpushJob] =
            Aspect.Weave(
              "LogpushClient",
              List(List(
                Aspect.Advice.byValue("zoneId", zoneId),
                Aspect.Advice.byValue("job", job),
              )),
              Aspect.Advice("createJob", af.createJob(zoneId, job))
            )

      override def mapK[F[_], G[_]](af: LogpushClient[F])(fk: F ~> G): LogpushClient[G] =
        new LogpushClient[G]:
          override def list(zoneId: ZoneId): G[LogpushJob] =
            fk(af.list(zoneId))

          override def createOwnership(zoneId: ZoneId, destination: LogpushDestination): G[LogpushOwnership] =
            fk(af.createOwnership(zoneId, destination))

          override def createJob(zoneId: ZoneId, job: CreateJob): G[LogpushJob] =
            fk(af.createJob(zoneId, job))
