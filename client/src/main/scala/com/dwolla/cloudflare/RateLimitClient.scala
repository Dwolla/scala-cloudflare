package com.dwolla.cloudflare

import cats._
import cats.syntax.all._
import com.dwolla.cloudflare.domain.model.Exceptions.UnexpectedCloudflareErrorException
import com.dwolla.cloudflare.domain.model.ratelimits._
import com.dwolla.cloudflare.domain.model.{ZoneId, tagZoneId}
import io.circe._
import io.circe.optics.JsonPath._
import io.circe.syntax._
import fs2._
import org.http4s.Method._
import org.http4s.Request
import org.http4s.circe._
import org.http4s.client.dsl.Http4sClientDsl

import scala.util.matching.Regex

trait RateLimitClient[F[_]] {
  def list(zoneId: ZoneId): Stream[F, RateLimit]
  def getById(zoneId: ZoneId, rateLimitId: String): Stream[F, RateLimit]
  def create(zoneId: ZoneId, rateLimit: RateLimit): Stream[F, RateLimit]
  def update(zoneId: ZoneId, rateLimit: RateLimit): Stream[F, RateLimit]
  def delete(zoneId: ZoneId, rateLimitId: String): Stream[F, RateLimitId]

  def getByUri(uri: String): Stream[F, RateLimit] = parseUri(uri).fold(Stream.empty.covaryAll[F, RateLimit]) {
    case (zoneId, rateLimitId) => getById(zoneId, rateLimitId)
  }

  def parseUri(uri: String): Option[(ZoneId, RateLimitId)] = uri match {
    case RateLimitClient.uriRegex(zoneId, rateLimitId) => Option((tagZoneId(zoneId), tagRateLimitId(rateLimitId)))
    case _ => None
  }

  def buildUri(zoneId: ZoneId, rateLimitId: RateLimitId): String =
    s"https://api.cloudflare.com/client/v4/zones/$zoneId/rate_limits/$rateLimitId"
}

object RateLimitClient {
  def apply[F[_] : ApplicativeThrow](executor: StreamingCloudflareApiExecutor[F]): RateLimitClient[F] = new RateLimitClientImpl[F](executor)

  val uriRegex: Regex = """https://api.cloudflare.com/client/v4/zones/(.+?)/rate_limits/(.+)""".r
}

class RateLimitClientImpl[F[_] : ApplicativeThrow](executor: StreamingCloudflareApiExecutor[F]) extends RateLimitClient[F] with Http4sClientDsl[F] {
  private def fetch(req: Request[F]): Stream[F, RateLimit] =
    executor.fetch[RateLimit](req)

  override def list(zoneId: ZoneId): Stream[F, RateLimit] =
    fetch(GET(BaseUrl / "zones" / zoneId / "rate_limits"))

  override def getById(zoneId: ZoneId, rateLimitId: String): Stream[F, RateLimit] =
    fetch(GET(BaseUrl / "zones" / zoneId / "rate_limits" / rateLimitId))

  override def create(zoneId: ZoneId, rateLimit: RateLimit): Stream[F, RateLimit] =
    fetch(POST(rateLimit.asJson, BaseUrl / "zones" / zoneId / "rate_limits"))

  override def update(zoneId: ZoneId, rateLimit: RateLimit): Stream[F, RateLimit] =
  // TODO it would really be better to do this check at compile time by baking the identification question into the types
    if (rateLimit.id.isDefined)
      fetch(PUT(rateLimit.copy(id = None).asJson, BaseUrl / "zones" / zoneId / "rate_limits" / rateLimit.id.get))
    else
      Stream.raiseError[F](CannotUpdateUnidentifiedRateLimit(rateLimit))

  override def delete(zoneId: ZoneId, rateLimitId: String): Stream[F, RateLimitId] =
    for {
      json <- executor.fetch[Json](DELETE(BaseUrl / "zones" / zoneId / "rate_limits" / rateLimitId)).last.recover {
        case ex: UnexpectedCloudflareErrorException if ex.errors.flatMap(_.code.toSeq).exists(notFoundCodes.contains) =>
          None
      }
    } yield tagRateLimitId(json.flatMap(deletedRecordLens).getOrElse(rateLimitId))

  private val deletedRecordLens: Json => Option[String] = root.id.string.getOption
  private val notFoundCodes = List(1000, 7000, 7003)
}

case class CannotUpdateUnidentifiedRateLimit(rateLimit: RateLimit) extends RuntimeException(s"Cannot update unidentified rate limit $rateLimit")
