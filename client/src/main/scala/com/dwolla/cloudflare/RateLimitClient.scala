package com.dwolla.cloudflare

import cats.*
import cats.effect.{Trace as _, *}
import cats.syntax.all.*
import com.dwolla.cloudflare.domain.model.Exceptions.UnexpectedCloudflareErrorException
import com.dwolla.cloudflare.domain.model.ratelimits.*
import com.dwolla.cloudflare.domain.model.{ZoneId, tagZoneId}
import com.dwolla.tagless.*
import com.dwolla.tracing.syntax.*
import io.circe.*
import io.circe.optics.JsonPath.*
import io.circe.syntax.*
import fs2.*
import org.http4s.Method.*
import org.http4s.{Request, Uri}
import org.http4s.circe.*
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.syntax.all.*

import scala.util.matching.Regex

trait RateLimitClient[F[_]] {
  def list(zoneId: ZoneId): F[RateLimit]
  def getById(zoneId: ZoneId, rateLimitId: String): F[RateLimit]
  def create(zoneId: ZoneId, rateLimit: RateLimit): F[RateLimit]
  def update(zoneId: ZoneId, rateLimit: RateLimit): F[RateLimit]
  def delete(zoneId: ZoneId, rateLimitId: String): F[RateLimitId]
  def getByUri(uri: String): F[RateLimit]

  def parseUri(uri: String): Option[(ZoneId, RateLimitId)] = uri match {
    case RateLimitClient.uriRegex(zoneId, rateLimitId) => Option((tagZoneId(zoneId), tagRateLimitId(rateLimitId)))
    case _ => None
  }

  def buildUri(zoneId: ZoneId, rateLimitId: RateLimitId): Uri =
    uri"https://api.cloudflare.com/client/v4/zones" / zoneId / "rate_limits" / rateLimitId
}

object RateLimitClient extends RateLimitClientInstances {
  def apply[F[_] : MonadCancelThrow : natchez.Trace](executor: StreamingCloudflareApiExecutor[F]): RateLimitClient[Stream[F, *]] =
    apply(executor, _.traceWithInputsAndOutputs)

  def apply[F[_] : ApplicativeThrow](executor: StreamingCloudflareApiExecutor[F],
                                     transform: RateLimitClient[Stream[F, *]] => RateLimitClient[Stream[F, *]]): RateLimitClient[Stream[F, *]] =
    WeaveKnot(knot(executor))(transform)

  private def knot[F[_] : ApplicativeThrow](executor: StreamingCloudflareApiExecutor[F]): Eval[RateLimitClient[Stream[F, *]]] => RateLimitClient[Stream[F, *]] =
    new RateLimitClientImpl[F](executor, _)

  val uriRegex: Regex = """https://api.cloudflare.com/client/v4/zones/(.+?)/rate_limits/(.+)""".r
}

private class RateLimitClientImpl[F[_] : ApplicativeThrow](executor: StreamingCloudflareApiExecutor[F],
                                                          self: Eval[RateLimitClient[Stream[F, *]]])
  extends RateLimitClient[Stream[F, *]] with Http4sClientDsl[F] {

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

  override def getByUri(uri: String): Stream[F, RateLimit] =
    parseUri(uri).fold(MonoidK[Stream[F, *]].empty[RateLimit]) {
      case (zoneId, RateLimitId(rateLimitId)) => self.value.getById(zoneId, rateLimitId)
    }

  private val deletedRecordLens: Json => Option[String] = root.id.string.getOption
  private val notFoundCodes = List(1000, 7000, 7003)
}

case class CannotUpdateUnidentifiedRateLimit(rateLimit: RateLimit) extends RuntimeException(s"Cannot update unidentified rate limit $rateLimit")
