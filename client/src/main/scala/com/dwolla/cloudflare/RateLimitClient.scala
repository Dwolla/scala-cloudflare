package com.dwolla.cloudflare

import cats.effect._
import cats.implicits._
import com.dwolla.cloudflare.domain.dto.ratelimits._
import com.dwolla.cloudflare.domain.model.Exceptions.UnexpectedCloudflareErrorException
import com.dwolla.cloudflare.domain.model.{Implicits ⇒ _, _}
import com.dwolla.cloudflare.domain.model.ratelimits.Implicits._
import com.dwolla.cloudflare.domain.model.ratelimits._
import io.circe.generic.auto._
import io.circe.optics.JsonPath._
import io.circe.syntax._
import io.circe.{Decoder, DecodingFailure, HCursor, Json}
import fs2._
import org.http4s.Method._
import org.http4s.circe._
import org.http4s.client.dsl.Http4sClientDsl

import scala.language.higherKinds
import scala.util.matching.Regex

trait RateLimitClient[F[_]] {
  def list(zoneId: ZoneId): Stream[F, RateLimit]
  def getById(zoneId: ZoneId, rateLimitId: String): Stream[F, RateLimit]
  def create(zoneId: ZoneId, rateLimit: CreateRateLimit): Stream[F, RateLimit]
  def update(zoneId: ZoneId, rateLimit: RateLimit): Stream[F, RateLimit]
  def delete(zoneId: ZoneId, rateLimitId: String): Stream[F, RateLimitId]

  def getByUri(uri: String): Stream[F, RateLimit] = parseUri(uri).fold(Stream.empty.covaryAll[F, RateLimit]) {
    case (zoneId, rateLimitId) ⇒ getById(zoneId, rateLimitId)
  }

  def parseUri(uri: String): Option[(ZoneId, RateLimitId)] = uri match {
    case RateLimitClient.uriRegex(zoneId, rateLimitId) ⇒ Option((tagZoneId(zoneId), tagRateLimitId(rateLimitId)))
    case _ ⇒ None
  }
}

object RateLimitClient {
  def apply[F[_] : Sync](executor: StreamingCloudflareApiExecutor[F]): RateLimitClient[F] = new RateLimitClientImpl[F](executor)

  val uriRegex: Regex = """https://api.cloudflare.com/client/v4/zones/(.+?)/rate_limits/(.+)""".r
}

object RateLimitClientImpl {
  val notFoundCodes = List(10001)
}

class RateLimitClientImpl[F[_] : Sync](executor: StreamingCloudflareApiExecutor[F]) extends RateLimitClient[F] with Http4sClientDsl[F] {
  import RateLimitClientImpl._

  implicit val rateLimitActionDtoDecoder: Decoder[RateLimitActionDTO] =
    (c: HCursor) ⇒ for {
      mode ← c.downField("mode").as[String]
      timeout ← c.downField("timeout").as[Option[Int]]
      response ← c.downField("response").as[Option[RateLimitActionResponseDTO]]
      dto ← (mode, timeout) match {
        case ("challenge", None) ⇒ Right(ChallengeRateLimitActionDTO)
        case ("js_challenge", None) ⇒ Right(JsChallengeRateLimitActionDTO)
        case ("ban", Some(t)) ⇒ Right(BanRateLimitActionDTO(t, response))
        case ("simulate", Some(t)) ⇒ Right(SimulateRateLimitActionDTO(t, response))
        case _ ⇒ Left(DecodingFailure("Invalid mode for rate limit action", c.history))
      }
    } yield dto

  override def list(zoneId: ZoneId): Stream[F, RateLimit] = {
    for {
      req ← Stream.eval(GET(BaseUrl / "zones" / zoneId / "rate_limits"))
      record ← executor.fetch[RateLimitDTO](req)
    } yield record
  }

  override def getById(zoneId: ZoneId, rateLimitId: String): Stream[F, RateLimit] =
    for {
      req ← Stream.eval(GET(BaseUrl / "zones" / zoneId / "rate_limits" / rateLimitId))
      res ← executor.fetch[RateLimitDTO](req).returningEmptyOnErrorCodes(notFoundCodes: _*)
    } yield res

  override def create(zoneId: ZoneId, rateLimit: CreateRateLimit): Stream[F, RateLimit] = {
    for {
      req ← Stream.eval(POST(BaseUrl / "zones" / zoneId / "rate_limits", rateLimit.asJson))
      resp ← executor.fetch[RateLimitDTO](req).map(Implicits.toModel)
    } yield resp
  }

  override def update(zoneId: ZoneId, rateLimit: RateLimit): Stream[F, RateLimit] = {
    for {
      req ← Stream.eval(PUT(BaseUrl / "zones" / zoneId / "rate_limits" / rateLimit.id, toDto(rateLimit).asJson))
      resp ← executor.fetch[RateLimitDTO](req).map(Implicits.toModel)
    } yield resp
  }

  override def delete(zoneId: ZoneId, rateLimitId: String): Stream[F, RateLimitId] =
  /*_*/
    for {
      req ← Stream.eval(DELETE(BaseUrl / "zones" / zoneId / "rate_limits" / rateLimitId))
      json ← executor.fetch[Json](req).last.adaptError {
        case ex: UnexpectedCloudflareErrorException if ex.errors.flatMap(_.code.toSeq).exists(notFoundCodes.contains) ⇒
          RateLimitDoesNotExistException(zoneId, rateLimitId)
      }
    } yield tagRateLimitId(json.flatMap(deletedRecordLens).getOrElse(rateLimitId))
  /*_*/

  private val deletedRecordLens: Json ⇒ Option[String] = root.id.string.getOption
}

case class RateLimitDoesNotExistException(zoneId: ZoneId, rateLimitId: String) extends RuntimeException(
  s"The rate limit $rateLimitId not found for zone $zoneId."
)
