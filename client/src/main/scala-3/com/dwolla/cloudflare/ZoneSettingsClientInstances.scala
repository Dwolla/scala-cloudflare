package com.dwolla.cloudflare

import cats.*
import cats.data.*
import cats.syntax.all.*
import cats.tagless.aop.*
import com.dwolla.cloudflare.CloudflareSettingFunctions.CloudflareSettingFunction
import com.dwolla.tracing.LowPriorityTraceableValueInstances.*
import natchez.*
import com.dwolla.cloudflare.domain.model.*

trait ZoneSettingsClientInstances:
  private given TraceableValue[ValidatedNel[Throwable, Unit]] with
    def toTraceValue(v: ValidatedNel[Throwable, Unit]): TraceValue =
      v match
        case Validated.Valid(_) => "ok"
        case Validated.Invalid(nel) =>
          nel
            .map(t => Option(t.getMessage).getOrElse(t.toString))
            .distinct
            .mkString_("; ")

  given Aspect[ZoneSettingsClient, TraceableValue, TraceableValue] =
    new Aspect[ZoneSettingsClient, TraceableValue, TraceableValue]:
      override def weave[F[_]](af: ZoneSettingsClient[F]): ZoneSettingsClient[[a] =>> Aspect.Weave[F, TraceableValue, TraceableValue, a]] =
        new ZoneSettingsClient[[a] =>> Aspect.Weave[F, TraceableValue, TraceableValue, a]]:
          override def settings: Set[CloudflareSettingFunction] = af.settings
          override def updateSettings(zone: Zone): Aspect.Weave[F, TraceableValue, TraceableValue, ValidatedNel[Throwable, Unit]] =
            Aspect.Weave(
              "ZoneSettingsClient",
              List(List(
                Aspect.Advice.byValue("zone", zone),
              )),
              Aspect.Advice("updateSettings", af.updateSettings(zone))
            )

      override def mapK[F[_], G[_]](af: ZoneSettingsClient[F])(fk: F ~> G): ZoneSettingsClient[G] =
        new ZoneSettingsClient[G]:
          override def settings: Set[CloudflareSettingFunction] = af.settings
          override def updateSettings(zone: Zone): G[ValidatedNel[Throwable, Unit]] =
            fk(af.updateSettings(zone))
