package com.dwolla.cloudflare

import io.circe._
import io.circe.syntax._
import cats._
import cats.data.Validated._
import cats.data._
import cats.effect._
import cats.syntax.all.*
import fs2._
import _root_.org.http4s.circe._
import com.dwolla.cloudflare.CloudflareSettingFunctions._
import com.dwolla.cloudflare.domain.dto.ZoneSettingsDTO
import com.dwolla.cloudflare.domain.model._
import org.http4s.Method._
import org.http4s.Uri
import org.http4s.client.dsl.Http4sClientDsl

trait ZoneSettingsClient[F[_]] {
  def updateSettings(zone: Zone): Stream[F, ValidatedNel[Throwable, Unit]]
}

object ZoneSettingsClient {
  def apply[F[_] : Concurrent](executor: StreamingCloudflareApiExecutor[F], maxConcurrency: Int = 5): ZoneSettingsClient[F] =
    new ZoneSettingsClientImpl(executor, maxConcurrency)
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

class ZoneSettingsClientImpl[F[_] : Concurrent](executor: StreamingCloudflareApiExecutor[F], maxConcurrency: Int) extends ZoneSettingsClient[F] with Http4sClientDsl[F] {
  implicit private val nelSemigroup: Semigroup[NonEmptyList[Throwable]] =
    SemigroupK[NonEmptyList].algebra[Throwable]

  private val zoneClient = new ZoneClientImpl(executor)

  val settings: Set[CloudflareSettingFunction] = allSettings

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
