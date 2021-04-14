package com.dwolla.cloudflare.domain.model

import java.time.Instant

import shapeless.tag.@@
import org.http4s.Uri
import org.http4s.circe._
import com.dwolla.circe._
import io.circe._
import io.circe.export.Exported
import io.circe.generic.auto._

package object pagerules {

  type PageRuleId = String @@ PageRuleIdTag

  private[cloudflare] val tagPageRuleId: String => PageRuleId = shapeless.tag[PageRuleIdTag][String]

}

package pagerules {

  import scala.annotation.nowarn

  trait PageRuleIdTag

  case class PageRule(id: Option[PageRuleId] = None,
                      targets: List[PageRuleTarget],
                      actions: List[PageRuleAction],
                      priority: Int,
                      status: PageRuleStatus,
                      modified_on: Option[Instant] = None,
                      created_on: Option[Instant] = None,
                     )
  object PageRule {
    implicit val pageRuleEncoder: Exported[Encoder[PageRule]] = exportEncoder[PageRule]
    implicit val pageRuleDecoder: Exported[Decoder[PageRule]] = exportDecoder[PageRule]
  }

  case class PageRuleTarget(target: String, constraint: PageRuleConstraint)
  case class PageRuleConstraint(operator: String,
                                value: String)

  sealed trait PageRuleAction
  case class AlwaysOnline(value: PageRuleActionEnabled) extends PageRuleAction
  case object AlwaysUseHttps extends PageRuleAction
  case class Minify(html: PageRuleActionEnabled, css: PageRuleActionEnabled, js: PageRuleActionEnabled) extends PageRuleAction
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

  sealed trait PageRuleActionEnabled
  object PageRuleActionEnabled {
    case object On extends PageRuleActionEnabled
    case object Off extends PageRuleActionEnabled
  }

  sealed trait SslSetting
  object SslSetting {
    case object Off extends SslSetting
    case object Flexible extends SslSetting
    case object Full extends SslSetting
    case object Strict extends SslSetting
    case object OriginPull extends SslSetting
  }

  sealed trait PolishValue
  object PolishValue {
    case object Lossless extends PolishValue
    case object Lossy extends PolishValue
    case object Off extends PolishValue
  }

  sealed trait SecurityLevelValue
  object SecurityLevelValue {
    case object Off extends SecurityLevelValue
    case object EssentiallyOff extends SecurityLevelValue
    case object Low extends SecurityLevelValue
    case object Medium extends SecurityLevelValue
    case object High extends SecurityLevelValue
    case object UnderAttack extends SecurityLevelValue
  }

  sealed trait CacheLevelValue
  object CacheLevelValue {
    case object Bypass extends CacheLevelValue
    case object NoQueryString extends CacheLevelValue
    case object IgnoreQueryString extends CacheLevelValue
    case object Standard extends CacheLevelValue
    case object CacheEverything extends CacheLevelValue
  }

  trait PageRuleActionCodec extends EnumerationSnakeCodec {
    import CacheLevelValue._
    import io.circe.literal._

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

    implicit val forwardingUrlEncoder: Encoder[ForwardingUrl] = a =>
      json"""{
               "id": "forwarding_url",
               "value": {
                 "url": ${a.url},
                 "status_code": ${a.status_code}
               }
             }"""

    implicit val forwardingUrlDecoder: Decoder[ForwardingUrl] = c => {
      val value = c.downField("value")

      for {
        url <- value.downField("url").as[Uri]
        statusCode <- value.downField("status_code").as[ForwardingStatusCode]
        _ <- {
          val a = c.downField("id")
          a.as[String].flatMap {
            case "forwarding_url" => Right(())
            case _ => Left(DecodingFailure("id must be `forwarding_url`", a.history))
          }
        }
      } yield ForwardingUrl(url, statusCode)
    }

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

    implicit val redirectEncoder: Encoder[ForwardingStatusCode] = Encoder[Int].contramap {
      case PermanentRedirect => 301
      case TemporaryRedirect => 302
    }

    implicit val redirectDecoder: Decoder[ForwardingStatusCode] = Decoder[Int].map {
      case 301 => PermanentRedirect
      case 302 => TemporaryRedirect
    }
  }

  object PageRuleAction extends PageRuleActionCodec {
    import io.circe.generic.extras.semiauto._
    import io.circe.generic.extras.Configuration

    @nowarn("msg=private val genDevConfig in object .* is never used")
    private implicit val genDevConfig: Configuration =
      Configuration
        .default
        .withSnakeCaseMemberNames
        .withSnakeCaseConstructorNames
        .withDiscriminator("id")

    val derivedEncoder: Encoder[PageRuleAction] = deriveConfiguredEncoder[PageRuleAction]
    implicit val decoder: Decoder[PageRuleAction] = deriveConfiguredDecoder[PageRuleAction]

    implicit val pageRuleActionEncoder: Encoder[PageRuleAction] = {
      case a: Minify => minifyEncoder(a)
      case a: ForwardingUrl => forwardingUrlEncoder(a)
      case other => derivedEncoder(other)
    }
  }

  sealed trait PageRuleStatus
  object PageRuleStatus extends EnumerationSnakeCodec {
    case object Active extends PageRuleStatus
    case object Disabled extends PageRuleStatus
  }

}
