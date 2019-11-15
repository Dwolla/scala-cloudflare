package com.dwolla.cloudflare

import cats.implicits._
import com.dwolla.cloudflare.domain.model.{ZoneId, tagZoneId}
import com.dwolla.cloudflare.domain.model.ratelimits._
import io.circe.syntax._
import io.circe._
import io.circe.optics.JsonPath._
import fs2._
import cats.effect.Sync
import com.dwolla.cloudflare.domain.model.Exceptions.UnexpectedCloudflareErrorException
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
  def apply[F[_] : Sync](executor: StreamingCloudflareApiExecutor[F]): RateLimitClient[F] = new RateLimitClientImpl[F](executor)

  val uriRegex: Regex = """https://api.cloudflare.com/client/v4/zones/(.+?)/rate_limits/(.+)""".r
}

class RateLimitClientImpl[F[_] : Sync](executor: StreamingCloudflareApiExecutor[F]) extends RateLimitClient[F] with Http4sClientDsl[F] {
  private def fetch(reqF: F[Request[F]]): Stream[F, RateLimit] =
    for {
      req <- Stream.eval(reqF)
      res <- executor.fetch[RateLimit](req)
    } yield res

  override def list(zoneId: ZoneId): Stream[F, RateLimit] =
    fetch(GET(BaseUrl / "zones" / zoneId / "rate_limits"))

  override def getById(zoneId: ZoneId, rateLimitId: String): Stream[F, RateLimit] =
    fetch(GET(BaseUrl / "zones" / zoneId / "rate_limits" / rateLimitId))

  override def create(zoneId: ZoneId, rateLimit: RateLimit): Stream[F, RateLimit] =
    fetch(POST(BaseUrl / "zones" / zoneId / "rate_limits", rateLimit.asJson))

  override def update(zoneId: ZoneId, rateLimit: RateLimit): Stream[F, RateLimit] =
  // TODO it would really be better to do this check at compile time by baking the identification question into the types
    if (rateLimit.id.isDefined)
      fetch(PUT(BaseUrl / "zones" / zoneId / "rate_limits" / rateLimit.id.get, rateLimit.copy(id = None).asJson))
    else
      Stream.raiseError(CannotUpdateUnidentifiedRateLimit(rateLimit))

  override def delete(zoneId: ZoneId, rateLimitId: String): Stream[F, RateLimitId] =
    for {
      req <- Stream.eval(DELETE(BaseUrl / "zones" / zoneId / "rate_limits" / rateLimitId))
      json <- executor.fetch[Json](req).last.recover {
        case ex: UnexpectedCloudflareErrorException if ex.errors.flatMap(_.code.toSeq).exists(notFoundCodes.contains) =>
          None
      }
    } yield tagRateLimitId(json.flatMap(deletedRecordLens).getOrElse(rateLimitId))

  private val deletedRecordLens: Json => Option[String] = root.id.string.getOption
  private val notFoundCodes = List(1000, 7000, 7003)
}

case class CannotUpdateUnidentifiedRateLimit(rateLimit: RateLimit) extends RuntimeException(s"Cannot update unidentified rate limit $rateLimit")

