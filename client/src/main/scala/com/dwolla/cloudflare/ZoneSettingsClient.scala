package com.dwolla.cloudflare

import io.circe.*
import io.circe.syntax.*
import cats.*
import cats.data.Validated.*
import cats.data.*
import cats.effect.{Trace as _, *}
import cats.syntax.all.*
import fs2.*
import _root_.org.http4s.circe.*
import com.dwolla.cloudflare.CloudflareSettingFunctions.*
import com.dwolla.cloudflare.domain.dto.ZoneSettingsDTO
import com.dwolla.cloudflare.domain.model.*
import com.dwolla.tracing.TraceWeaveCapturingInputsAndOutputs
import org.http4s.Method.*
import org.http4s.Uri
import org.http4s.client.dsl.Http4sClientDsl
import natchez.*
import cats.tagless.aop.*
import cats.tagless.syntax.all.*

trait ZoneSettingsClient[F[_]] {
  def updateSettings(zone: Zone): F[ValidatedNel[Throwable, Unit]]
  def settings: Set[CloudflareSettingFunction]
}

object ZoneSettingsClient extends ZoneSettingsClientInstances {
  def apply[F[_] : Concurrent : Trace](executor: StreamingCloudflareApiExecutor[F]): ZoneSettingsClient[Stream[F, *]] =
    apply(executor, 5)

  def apply[F[_] : Concurrent : Trace](executor: StreamingCloudflareApiExecutor[F],
                                       maxConcurrency: Int): ZoneSettingsClient[Stream[F, *]] =
    apply(executor, maxConcurrency, new TraceWeaveCapturingInputsAndOutputs)

  def apply[F[_] : Concurrent, Dom[_], Cod[_]](executor: StreamingCloudflareApiExecutor[F],
                                               maxConcurrency: Int,
                                               transform: Aspect.Weave[Stream[F, *], Dom, Cod, *] ~> Stream[F, *],
                                              )
                                              (implicit A1: Aspect[ZoneSettingsClient, Dom, Cod],
                                               A2: Aspect[ZoneClient, Dom, Cod]): ZoneSettingsClient[Stream[F, *]] =
    apply(executor, maxConcurrency, transform, allSettings)

  def apply[F[_] : Concurrent, Dom[_], Cod[_]](executor: StreamingCloudflareApiExecutor[F],
                                               maxConcurrency: Int,
                                               transform: Aspect.Weave[Stream[F, *], Dom, Cod, *] ~> Stream[F, *],
                                               settings: Set[CloudflareSettingFunction],
                                              )
                                              (implicit A1: Aspect[ZoneSettingsClient, Dom, Cod],
                                               A2: Aspect[ZoneClient, Dom, Cod]): ZoneSettingsClient[Stream[F, *]] =
    A1.mapK(ZoneSettingsClientImpl(executor, maxConcurrency, ZoneClient(executor, transform), settings).weave[Dom, Cod])(transform)

}

object CloudflareSettingFunctions {
  /**
    * Given a Zone and a Zone ID, return a Some((Uri, Json)) if the
    * setting should be updated, or a None if it should not.
    */
  type CloudflareSettingFunction = Zone => ZoneId => Option[(Uri, Json)]

  val setTlsLevel: CloudflareSettingFunction = zone => zoneId => Option((BaseUrl / "zones" / zoneId / "settings" / "ssl", zone.tlsLevel.asJson))

  val setSecurityLevel: CloudflareSettingFunction =
    zone => zoneId => zone.securityLevel.map(sl => (BaseUrl / "zones" / zoneId / "settings" / "security_level", sl.asJson))

  val setWaf: CloudflareSettingFunction =
    zone => zoneId => zone.waf.map(w => (BaseUrl / "zones" / zoneId / "settings" / "waf", w.asJson))

  val allSettings: Set[CloudflareSettingFunction] = Set(setTlsLevel, setSecurityLevel, setWaf)
}

private object ZoneSettingsClientImpl {
  def apply[F[_] : Concurrent](executor: StreamingCloudflareApiExecutor[F],
                               maxConcurrency: Int,
                               zoneClient: ZoneClient[Stream[F, *]],
                               settings: Set[CloudflareSettingFunction],
                              ): ZoneSettingsClient[Stream[F, *]] =
    new ZoneSettingsClientImpl(executor, maxConcurrency, zoneClient, settings)
}

private class ZoneSettingsClientImpl[F[_] : Concurrent](executor: StreamingCloudflareApiExecutor[F],
                                                        maxConcurrency: Int,
                                                        zoneClient: ZoneClient[Stream[F, *]],
                                                        override val settings: Set[CloudflareSettingFunction],
                                                       )
  extends ZoneSettingsClient[Stream[F, *]]
    with Http4sClientDsl[F] {
  implicit private val nelSemigroup: Semigroup[NonEmptyList[Throwable]] =
    SemigroupK[NonEmptyList].algebra[Throwable]

  override def updateSettings(zone: Zone): Stream[F, ValidatedNel[Throwable, Unit]] = {
    for {
      zoneId <- zoneClient.getZoneId(zone.name)
      res <- applySettings(zoneId, zone).map(_.attempt).parJoin(maxConcurrency)
    } yield res.toValidatedNel
  }.foldMonoid

  private def applySettings(zoneId: ZoneId, zone: Zone): Stream[F, Stream[F, Unit]] =
    Stream.emits(settings.toList).map(_(zone)(zoneId)).collect {
      case Some((uri, zoneSetting)) => patchValue(uri, zoneSetting)
    }

  private def patchValue[T](uri: Uri, cloudflareSettingValue: Json) =
    executor.fetch[ZoneSettingsDTO](PATCH(cloudflareSettingValue, uri)).void
}
