package com.dwolla.cloudflare.domain.model

import java.time.Duration

import com.dwolla.circe._
import io.circe.generic.semiauto
import io.circe._
import com.dwolla.cloudflare.CloudflareNewtype

package object ratelimits {

  type RateLimitId = RateLimitId.Type
  object RateLimitId extends CloudflareNewtype[String]

  private[cloudflare] val tagRateLimitId: String => RateLimitId = RateLimitId(_)
}

package ratelimits {

  case class RateLimit(id: Option[RateLimitId] = None,
                       disabled: Option[Boolean] = None,
                       description: Option[String] = None,
                       `match`: RateLimitMatch,
                       correlate: Option[RateLimitCorrelation] = None,
                       bypass: List[RateLimitBypass] = List.empty,
                       threshold: Int,
                       period: Duration,
                       action: RateLimitAction)

  case class RateLimitMatch(request: RateLimitMatchRequest, response: Option[RateLimitMatchResponse] = None)

  object RateLimitMatch {
    implicit val rateLimitMatchCodec: Codec[RateLimitMatch] = semiauto.deriveCodec
  }

  case class RateLimitMatchRequest(methods: List[Method] = List.empty,
                                   schemes: List[Scheme] = List.empty,
                                   url: String)

  object RateLimitMatchRequest extends NullAsEmptyListCodec {
    implicit val rateLimitMatchRequestCodec: Codec[RateLimitMatchRequest] = semiauto.deriveCodec
  }

  case class RateLimitCorrelation(by: String)

  object RateLimitCorrelation {
    implicit val rateLimitCorrelationCodec: Codec[RateLimitCorrelation] = semiauto.deriveCodec
  }

  sealed trait Method
  object Method {
    case object Get extends Method
    case object Post extends Method
    case object Put extends Method
    case object Delete extends Method
    case object Patch extends Method
    case object Head extends Method
    case object All extends Method

    implicit val methodEncoder: Encoder[Method] = Encoder[String].contramap {
      case Method.Get => "GET"
      case Method.Post => "POST"
      case Method.Put => "PUT"
      case Method.Delete => "DELETE"
      case Method.Patch => "PATCH"
      case Method.Head => "HEAD"
      case Method.All => "_ALL_"
    }

    implicit val methodDecoder: Decoder[Method] = Decoder[String].map {
      case "GET" => Method.Get
      case "POST" => Method.Post
      case "PUT" => Method.Put
      case "DELETE" => Method.Delete
      case "PATCH" => Method.Patch
      case "HEAD" => Method.Head
      case "_ALL_" => Method.All
    }
  }

  sealed trait Scheme
  object Scheme {
    case object Http extends Scheme
    case object Https extends Scheme
    case object All extends Scheme

    implicit val schemeEncoder: Encoder[Scheme] = Encoder[String].contramap {
      case Scheme.Http => "HTTP"
      case Scheme.Https => "HTTPS"
      case Scheme.All => "_ALL_"
    }

    implicit val schemeDecoder: Decoder[Scheme] = Decoder[String].map {
      case "HTTP" => Scheme.Http
      case "HTTPS" => Scheme.Https
      case "_ALL_" => Scheme.All
    }
  }

  case class RateLimitMatchResponse(origin_traffic: Option[Boolean] = None,
                                    headers: List[RateLimitMatchResponseHeader],
                                    status: List[Int] = List.empty,
                                   )

  object RateLimitMatchResponse extends NullAsEmptyListCodec with StringAsBooleanCodec {
    implicit val rateLimitMatchResponseCodec: Codec[RateLimitMatchResponse] = semiauto.deriveCodec
  }


  case class RateLimitMatchResponseHeader(name: String,
                                          op: Op,
                                          value: String)

  object RateLimitMatchResponseHeader {
    implicit val rateLimitMatchResponseHeaderCodec: Codec[RateLimitMatchResponseHeader] = semiauto.deriveCodec
  }

  sealed trait Op
  object Op {
    case object Equal extends Op
    case object NotEqual extends Op

    implicit val opEncoder: Encoder[Op] = Encoder[String].contramap {
      case Op.Equal => "eq"
      case Op.NotEqual => "ne"
    }

    implicit val opDecoder: Decoder[Op] = Decoder[String].map {
      case "eq" => Op.Equal
      case "ne" => Op.NotEqual
    }
  }

  case class RateLimitBypass(name: String, value: String)

  object RateLimitBypass {
    implicit val rateLimitBypassCodec: Codec[RateLimitBypass] = semiauto.deriveCodec
  }

  sealed trait RateLimitAction
  case class Simulate(timeout: Duration, response: Option[RateLimitActionResponse]) extends RateLimitAction
  case class Ban(timeout: Duration, response: Option[RateLimitActionResponse]) extends RateLimitAction
  case object Challenge extends RateLimitAction
  case object JsChallenge extends RateLimitAction

  case class RateLimitActionResponse(content_type: ContentType, body: String)

  object RateLimitActionResponse {
    implicit val rateLimitActionResponseCodec: Codec[RateLimitActionResponse] = semiauto.deriveCodec
  }

  sealed trait ContentType
  object ContentType {
    case object Text extends ContentType
    case object Xml extends ContentType
    case object Json extends ContentType

    implicit val contentTypeEncoder: Encoder[ContentType] = Encoder[String].contramap {
      case Text => "text/plain"
      case Xml => "text/xml"
      case Json => "application/json"
    }

    implicit val contentTypeDecoder: Decoder[ContentType] = Decoder[String].map {
      case "text/plain" => Text
      case "text/xml" => Xml
      case "application/json" => Json
    }
  }

  object RateLimitAction extends DurationAsSecondsCodec {
    import io.circe.generic.extras.Configuration

    private implicit val genDevConfig: Configuration =
      Configuration
        .default
        .withSnakeCaseMemberNames
        .withSnakeCaseConstructorNames
        .withDiscriminator("mode")

    implicit val rateLimitActionCodec: Codec[RateLimitAction] = generic.extras.semiauto.deriveConfiguredCodec
  }

  object RateLimit extends DurationAsSecondsCodec with NullAsEmptyListCodec with StringAsBooleanCodec {
    implicit val rateLimitCodec: Codec[RateLimit] = semiauto.deriveCodec
  }
}
