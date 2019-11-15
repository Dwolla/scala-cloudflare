package com.dwolla.cloudflare.domain.model

import cats.implicits._
import com.dwolla.circe._
import io.circe.{Decoder, Encoder}
import shapeless.tag.@@

package object ratelimits {

  type RateLimitId = String @@ RateLimitIdTag

  private[cloudflare] val tagRateLimitId: String => RateLimitId = shapeless.tag[RateLimitIdTag][String]

  implicit val booleanDecoder: Decoder[Boolean] = Decoder.decodeBoolean or Decoder.decodeString.emap {
    case s if s.toLowerCase() == "true" => true.asRight
    case s if s.toLowerCase() == "false" => false.asRight
    case _ => "Boolean".asLeft
  }
}

package ratelimits {
  import java.time.Duration

  import io.circe.Decoder.Result
  import io.circe.{ACursor, DecodingFailure, FailedCursor, HCursor, ObjectEncoder}
  import io.circe.export.Exported
  import io.circe.generic.auto._

  trait RateLimitIdTag

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

  case class RateLimitMatchRequest(methods: List[Method] = List.empty,
                                   schemes: List[Scheme] = List.empty,
                                   url: String)

  case class RateLimitCorrelation(by: String)

  sealed trait Method
  object Method {
    case object Get extends Method
    case object Post extends Method
    case object Put extends Method
    case object Delete extends Method
    case object Patch extends Method
    case object Head extends Method
    case object All extends Method
  }

  sealed trait Scheme
  object Scheme {
    case object Http extends Scheme
    case object Https extends Scheme
    case object All extends Scheme
  }

  case class RateLimitMatchResponse(origin_traffic: Option[Boolean] = None,
                                    headers: List[RateLimitMatchResponseHeader],
                                    status: List[Int] = List.empty,
                                   )

  case class RateLimitMatchResponseHeader(name: String,
                                          op: Op,
                                          value: String)

  sealed trait Op
  object Op {
    case object Equal extends Op
    case object NotEqual extends Op
  }

  case class RateLimitBypass(name: String, value: String)

  sealed trait RateLimitAction
  case class Simulate(timeout: Duration, response: Option[RateLimitActionResponse]) extends RateLimitAction
  case class Ban(timeout: Duration, response: Option[RateLimitActionResponse]) extends RateLimitAction
  case object Challenge extends RateLimitAction
  case object JsChallenge extends RateLimitAction
  case class RateLimitActionResponse(content_type: ContentType, body: String)

  sealed trait ContentType
  object ContentType {
    case object Text extends ContentType
    case object Xml extends ContentType
    case object Json extends ContentType
  }

  trait DurationAsSecondsCodec {
    implicit val durationEncoder: Encoder[Duration] = Encoder[Long].contramap(_.getSeconds)

    implicit val durationDecoder: Decoder[Duration] = Decoder[Long].map(Duration.ofSeconds)
  }

  trait RateLimitActionCodec extends EnumerationSnakeCodec with DurationAsSecondsCodec {
    import ContentType._
    import io.circe.literal._

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

  object RateLimitAction extends RateLimitActionCodec {

    import io.circe.generic.extras.Configuration
    import io.circe.generic.extras.semiauto._

    private implicit val genDevConfig: Configuration =
      Configuration
        .default
        .withSnakeCaseMemberNames
        .withSnakeCaseConstructorNames
        .withDiscriminator("mode")

    private val _ = genDevConfig // work around compiler warning bug

    implicit val rateLimitActionEncoder: Encoder[RateLimitAction] = deriveEncoder[RateLimitAction]
    implicit val rateLimitActionDecoder: Decoder[RateLimitAction] = deriveDecoder[RateLimitAction]
  }

  object RateLimit extends DurationAsSecondsCodec {
    implicit val opEncoder: Encoder[Op] = Encoder[String].contramap {
      case Op.Equal => "eq"
      case Op.NotEqual => "ne"
    }

    implicit val opDecoder: Decoder[Op] = Decoder[String].map {
      case "eq" => Op.Equal
      case "ne" => Op.NotEqual
    }

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

    implicit def listDecoder[A](implicit d: Decoder[A]): Decoder[List[A]] = new Decoder[List[A]] {
      def apply(c: HCursor): Result[List[A]] = tryDecode(c)

      override def tryDecode(c: ACursor): Decoder.Result[List[A]] = c match {
        case c: HCursor =>
          if (c.value.isNull) Right(List.empty)
          else Decoder.decodeList[A].tryDecode(c)
        case c: FailedCursor =>
          if (!c.incorrectFocus) Right(List.empty) else Left(DecodingFailure("List[A]", c.history))
      }
    }

    implicit val rateLimitEncoder: Exported[ObjectEncoder[RateLimit]] = exportEncoder[RateLimit]
    implicit val rateLimitDecoder: Exported[Decoder[RateLimit]] = exportDecoder[RateLimit]
  }
}
