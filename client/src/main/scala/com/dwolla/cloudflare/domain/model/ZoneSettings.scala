package com.dwolla.cloudflare.domain.model

import com.dwolla.cloudflare.domain.model.ZoneSettings._
import io.circe._
import io.circe.generic.auto._

object ZoneSettings {
  sealed trait CloudflareTlsLevel

  case object Off extends CloudflareTlsLevel
  case object FlexibleTls extends CloudflareTlsLevel
  case object FullTls extends CloudflareTlsLevel
  case object FullTlsStrict extends CloudflareTlsLevel
  case object StrictTlsOnlyOriginPull extends CloudflareTlsLevel

  implicit val CloudflareTlsLevelEncoder: Encoder[CloudflareTlsLevel] =
    encoderBuilder[CloudflareTlsLevel] {
      case Off ⇒ "off"
      case FlexibleTls ⇒ "flexible"
      case FullTls ⇒ "full"
      case FullTlsStrict ⇒ "strict"
      case StrictTlsOnlyOriginPull ⇒ "origin_pull"
    }

  sealed trait CloudflareSecurityLevel

  case object EssentiallyOff extends CloudflareSecurityLevel
  case object Low extends CloudflareSecurityLevel
  case object Medium extends CloudflareSecurityLevel
  case object High extends CloudflareSecurityLevel
  case object UnderAttack extends CloudflareSecurityLevel

  implicit val CloudflareSecurityLevelEncoder: Encoder[CloudflareSecurityLevel] =
    encoderBuilder[CloudflareSecurityLevel] {
      case EssentiallyOff ⇒ "essentially_off"
      case Low ⇒ "low"
      case Medium ⇒ "medium"
      case High ⇒ "high"
      case UnderAttack ⇒ "under_attack"
    }

  private def encoderBuilder[T](f: T ⇒ String): Encoder[T] = Encoder[CloudflareSettingValue].contramap(f.andThen(CloudflareSettingValue))

  case class CloudflareSettingValue(value: String)

}

case class Zone(name: String,
                tlsLevel: CloudflareTlsLevel,
                securityLevel: Option[CloudflareSecurityLevel],
               )
