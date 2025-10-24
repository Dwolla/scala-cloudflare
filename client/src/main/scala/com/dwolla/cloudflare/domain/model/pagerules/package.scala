package com.dwolla.cloudflare.domain.model

import io.circe.*
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto.*
import io.circe.generic.semiauto.deriveCodec
import io.circe.literal.*
import io.circe.syntax.EncoderOps
import org.http4s.Uri
import org.http4s.circe.*
import com.dwolla.cloudflare.CloudflareNewtype

import java.time.Instant

package object pagerules {
  type PageRuleId = PageRuleId.Type
  object PageRuleId extends CloudflareNewtype[String]

  private[cloudflare] val tagPageRuleId: String => PageRuleId = PageRuleId(_)
}

package pagerules {

  case class PageRule(id: Option[PageRuleId] = None,
                      targets: List[PageRuleTarget],
                      actions: List[PageRuleAction],
                      priority: Int,
                      status: PageRuleStatus,
                      modified_on: Option[Instant] = None,
                      created_on: Option[Instant] = None,
                     )
  object PageRule {
    implicit val codec: Codec[PageRule] = deriveCodec
  }

  case class PageRuleTarget(target: String, constraint: PageRuleConstraint)
  object PageRuleTarget {
    implicit val codec: Codec[PageRuleTarget] = deriveCodec
  }
  case class PageRuleConstraint(operator: String,
                                value: String)
  object PageRuleConstraint {
    implicit val codec: Codec[PageRuleConstraint] = deriveCodec
  }

  sealed trait PageRuleAction

  case class AlwaysOnline(value: PageRuleActionEnabled) extends PageRuleAction
  case object AlwaysUseHttps extends PageRuleAction

  case class Minify(html: PageRuleActionEnabled, css: PageRuleActionEnabled, js: PageRuleActionEnabled) extends PageRuleAction
  object Minify {
    implicit val minifyEncoder: Encoder[Minify] = a =>
      json"""{
               "id": "minify",
               "value": {
                 "html": ${a.html},
                 "css": ${a.css},
                 "js": ${a.js}
               }
             }"""

    implicit val minifyDecoder: Decoder[Minify] = c => {
      val value = c.downField("value")

      for {
        html <- value.downField("html").as[PageRuleActionEnabled]
        css <- value.downField("css").as[PageRuleActionEnabled]
        js <- value.downField("js").as[PageRuleActionEnabled]
        _ <- {
          val a = c.downField("id")
          a.as[String].flatMap {
            case "minify" => Right(())
            case _ => Left(DecodingFailure("id must be `minify`", a.history))
          }
        }
      } yield Minify(html, css, js)
    }
  }

  case class AutomaticHttpsRewrites(value: PageRuleActionEnabled) extends PageRuleAction
  case class BrowserCacheTtl(value: Int) extends PageRuleAction
  case class BrowserCheck(value: PageRuleActionEnabled) extends PageRuleAction
  case class BypassCacheOnCookie(value: String) extends PageRuleAction
  case class CacheByDeviceType(value: PageRuleActionEnabled) extends PageRuleAction
  case class CacheDeceptionArmor(value: PageRuleActionEnabled) extends PageRuleAction
  case class CacheLevel(value: CacheLevelValue) extends PageRuleAction
  case class CacheOnCookie(value: String) extends PageRuleAction
  case object DisableApps extends PageRuleAction
  case object DisablePerformance extends PageRuleAction
  case object DisableSecurity extends PageRuleAction
  case class EdgeCacheTtl(value: Int) extends PageRuleAction
  case class EmailObfuscation(value: PageRuleActionEnabled) extends PageRuleAction
  case class ForwardingUrl(url: Uri, status_code: ForwardingStatusCode) extends PageRuleAction
  object ForwardingUrl {
    implicit val forwardingUrlEncoder: Encoder[ForwardingUrl] = a =>
      json"""{
               "id": "forwarding_url",
               "value": {
                 "url": ${a.url},
                 "status_code": ${a.status_code}
               }
             }"""

    implicit val forwardingUrlDecoder: Decoder[ForwardingUrl] = c =>
      for {
        _ <- {
          val a = c.downField("id")
          a.as[String].flatMap {
            case "forwarding_url" => Right(())
            case _ => Left(DecodingFailure("id must be `forwarding_url`", a.history))
          }
        }
        value = c.downField("value")
        url <- value.downField("url").as[Uri]
        statusCode <- value.downField("status_code").as[ForwardingStatusCode]
      } yield ForwardingUrl(url, statusCode)
  }

  case class HostHeaderOverride(value: String) extends PageRuleAction
  case class IpGeolocation(value: PageRuleActionEnabled) extends PageRuleAction
  case class Mirage(value: PageRuleActionEnabled) extends PageRuleAction
  case class OpportunisticEncryption(value: PageRuleActionEnabled) extends PageRuleAction
  case class ExplicitCacheControl(value: PageRuleActionEnabled) extends PageRuleAction
  case class OriginErrorPagePassThru(value: PageRuleActionEnabled) extends PageRuleAction
  case class Polish(value: PolishValue) extends PageRuleAction
  case class SortQueryStringForCache(value: PageRuleActionEnabled) extends PageRuleAction
  case class ResolveOverride(value: String) extends PageRuleAction
  case class RespectStrongEtag(value: PageRuleActionEnabled) extends PageRuleAction
  case class ResponseBuffering(value: PageRuleActionEnabled) extends PageRuleAction
  case class RocketLoader(value: PageRuleActionEnabled) extends PageRuleAction
  case class Ssl(value: SslSetting) extends PageRuleAction
  case class SecurityLevel(value: SecurityLevelValue) extends PageRuleAction
  case class ServerSideExclude(value: PageRuleActionEnabled) extends PageRuleAction
  case class TrueClientIpHeader(value: PageRuleActionEnabled) extends PageRuleAction
  case class Waf(value: PageRuleActionEnabled) extends PageRuleAction

  sealed trait ForwardingStatusCode
  case object PermanentRedirect extends ForwardingStatusCode
  case object TemporaryRedirect extends ForwardingStatusCode
  object ForwardingStatusCode {
    implicit val forwardingStatusCodeEncoder: Encoder[ForwardingStatusCode] = Encoder[Int].contramap {
      case PermanentRedirect => 301
      case TemporaryRedirect => 302
    }
    implicit val forwardingStatusCodeDecoder: Decoder[ForwardingStatusCode] = Decoder[Int].map {
      case 301 => PermanentRedirect
      case 302 => TemporaryRedirect
    }
  }

  sealed trait PageRuleActionEnabled
  object PageRuleActionEnabled {
    case object On extends PageRuleActionEnabled
    case object Off extends PageRuleActionEnabled

    implicit val encoder: Encoder[PageRuleActionEnabled] = Encoder[String].contramap {
      case On => "on"
      case Off => "off"
    }
    implicit val decoder: Decoder[PageRuleActionEnabled] = Decoder[String].map {
      case "on" => On
      case "off" => Off
    }
  }

  sealed trait SslSetting
  object SslSetting {
    case object Off extends SslSetting
    case object Flexible extends SslSetting
    case object Full extends SslSetting
    case object Strict extends SslSetting
    case object OriginPull extends SslSetting

    implicit val encoder: Encoder[SslSetting] = Encoder[String].contramap {
      case Off => "off"
      case Flexible => "flexible"
      case Full => "full"
      case Strict => "strict"
      case OriginPull => "origin_pull"
    }
    implicit val decoder: Decoder[SslSetting] = Decoder[String].map {
      case "off" => Off
      case "flexible" => Flexible
      case "full" => Full
      case "strict" => Strict
      case "origin_pull" => OriginPull
    }
  }

  sealed trait PolishValue
  object PolishValue {
    case object Lossless extends PolishValue
    case object Lossy extends PolishValue
    case object Off extends PolishValue

    implicit val encoder: Encoder[PolishValue] = Encoder[String].contramap {
      case Lossless => "lossless"
      case Lossy => "lossy"
      case Off => "off"
    }
    implicit val decoder: Decoder[PolishValue] = Decoder[String].map {
      case "lossless" => Lossless
      case "lossy" => Lossy
      case "off" => Off
    }
  }

  sealed trait SecurityLevelValue
  object SecurityLevelValue {
    case object Off extends SecurityLevelValue
    case object EssentiallyOff extends SecurityLevelValue
    case object Low extends SecurityLevelValue
    case object Medium extends SecurityLevelValue
    case object High extends SecurityLevelValue
    case object UnderAttack extends SecurityLevelValue

    implicit val encoder: Encoder[SecurityLevelValue] = Encoder[String].contramap {
      case Off => "off"
      case EssentiallyOff => "essentially_off"
      case Low => "low"
      case Medium => "medium"
      case High => "high"
      case UnderAttack => "under_attack"
    }
    implicit val decoder: Decoder[SecurityLevelValue] = Decoder[String].map {
      case "off" => Off
      case "essentially_off" => EssentiallyOff
      case "low" => Low
      case "medium" => Medium
      case "high" => High
      case "under_attack" => UnderAttack
    }
  }

  sealed trait CacheLevelValue
  object CacheLevelValue {
    case object Bypass extends CacheLevelValue
    case object NoQueryString extends CacheLevelValue
    case object IgnoreQueryString extends CacheLevelValue
    case object Standard extends CacheLevelValue
    case object CacheEverything extends CacheLevelValue

    implicit val encoderCacheLevelValue: Encoder[CacheLevelValue] = Encoder[String].contramap {
      case Bypass => "bypass"
      case NoQueryString => "basic"
      case IgnoreQueryString => "simplified"
      case Standard => "aggressive"
      case CacheEverything => "cache_everything"
    }

    implicit val decoderCacheLevelValue: Decoder[CacheLevelValue] = Decoder[String].map {
      case "bypass" => Bypass
      case "basic" => NoQueryString
      case "simplified" => IgnoreQueryString
      case "aggressive" => Standard
      case "cache_everything" => CacheEverything
    }
  }

  object PageRuleAction {
    private implicit val genDevConfig: Configuration =
      Configuration
        .default
        .withSnakeCaseMemberNames
        .withSnakeCaseConstructorNames
        .withDiscriminator("id")

    val derivedEncoder: Encoder[PageRuleAction] = deriveConfiguredEncoder[PageRuleAction]
    implicit val decoder: Decoder[PageRuleAction] = deriveConfiguredDecoder[PageRuleAction]

    // the derived encoder isn't correct for Minify and ForwardingUrl, so use their own encoders instead.
    // all the other encoders are derived by deriveConfiguredEncoder, so they don't need instances
    // in their companion objects
    implicit val pageRuleActionEncoder: Encoder[PageRuleAction] = {
      case a: Minify => a.asJson
      case a: ForwardingUrl => a.asJson
      case other => derivedEncoder(other)
    }
  }

  sealed trait PageRuleStatus
  object PageRuleStatus {
    case object Active extends PageRuleStatus
    case object Disabled extends PageRuleStatus

    implicit val encoder: Encoder[PageRuleStatus] = Encoder[String].contramap {
      case Active => "active"
      case Disabled => "disabled"
    }
    implicit val decoder: Decoder[PageRuleStatus] = Decoder[String].map {
      case "active" => Active
      case "disabled" => Disabled
    }
  }
}
