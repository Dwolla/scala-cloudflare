package com.dwolla.cloudflare.domain.model

import com.dwolla.cloudflare.domain.model.ZoneSettings._
import io.circe._
import io.circe.generic.semiauto._

object ZoneSettings {
  sealed trait CloudflareTlsLevel
  object CloudflareTlsLevel {
    case object Off extends CloudflareTlsLevel
    case object FlexibleTls extends CloudflareTlsLevel
    case object FullTls extends CloudflareTlsLevel
    case object FullTlsStrict extends CloudflareTlsLevel
    case object StrictTlsOnlyOriginPull extends CloudflareTlsLevel

    implicit val cloudflareTlsLevelEncoder: Encoder[CloudflareTlsLevel] =
      encoderBuilder[CloudflareTlsLevel] {
        case Off => "off"
        case FlexibleTls => "flexible"
        case FullTls => "full"
        case FullTlsStrict => "strict"
        case StrictTlsOnlyOriginPull => "origin_pull"
      }
  }

  sealed trait CloudflareSecurityLevel
  object CloudflareSecurityLevel {
    case object EssentiallyOff extends CloudflareSecurityLevel
    case object Low extends CloudflareSecurityLevel
    case object Medium extends CloudflareSecurityLevel
    case object High extends CloudflareSecurityLevel
    case object UnderAttack extends CloudflareSecurityLevel

    implicit val cloudflareSecurityLevelEncoder: Encoder[CloudflareSecurityLevel] =
      encoderBuilder[CloudflareSecurityLevel] {
        case EssentiallyOff => "essentially_off"
        case Low => "low"
        case Medium => "medium"
        case High => "high"
        case UnderAttack => "under_attack"
      }
  }

  sealed trait CloudflareWaf
  object CloudflareWaf {
    case object Off extends CloudflareWaf
    case object On extends CloudflareWaf

    implicit val cloudflareWafEncoder: Encoder[CloudflareWaf] =
      encoderBuilder[CloudflareWaf] {
        case Off => "off"
        case On => "on"
      }
  }



  private def encoderBuilder[T](f: T => String): Encoder[T] = Encoder[CloudflareSettingValue].contramap(f.andThen(CloudflareSettingValue(_)))

  case class CloudflareSettingValue(value: String)

  object CloudflareSettingValue {
    implicit val cloudflareSettingValueCodec: Codec[CloudflareSettingValue] = deriveCodec
}

}

case class Zone(name: String,
                tlsLevel: CloudflareTlsLevel,
                securityLevel: Option[CloudflareSecurityLevel],
                waf: Option[CloudflareWaf],
               )

object Zone {
  implicit val zoneEncoder: Encoder[Zone] = deriveEncoder
}
