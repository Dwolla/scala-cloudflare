package com.dwolla.cloudflare

import cats.data.*
import cats.syntax.all.*
import cats.tagless.aop.*
import cats.tagless.Derive
import com.dwolla.tracing.LowPriorityTraceableValueInstances.*
import natchez.*

trait ZoneSettingsClientInstances {
  private implicit val validatedNelThrowableUnitTraceableValue: TraceableValue[ValidatedNel[Throwable, Unit]] = new TraceableValue[ValidatedNel[Throwable, Unit]] {
    def toTraceValue(v: ValidatedNel[Throwable, Unit]): TraceValue =
      v match {
        case Validated.Valid(_) => "ok"
        case Validated.Invalid(nel) =>
          nel
            .map(t => Option(t.getMessage).getOrElse(t.toString))
            .distinct
            .mkString_("; ")
      }
  }

  implicit val traceableValueAspect: Aspect[ZoneSettingsClient, TraceableValue, TraceableValue] = Derive.aspect
}
