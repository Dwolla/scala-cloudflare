package com.dwolla.cloudflare

import cats._
import cats.effect._
import cats.implicits._
import com.dwolla.cloudflare.domain.dto.ratelimits._
import com.dwolla.cloudflare.domain.model
import com.dwolla.cloudflare.domain.model.Error
import com.dwolla.cloudflare.domain.model.Exceptions.UnexpectedCloudflareErrorException
import com.dwolla.cloudflare.domain.model.ratelimits.Implicits._
import com.dwolla.cloudflare.domain.model.ratelimits._
import io.circe.generic.auto._
import io.circe.optics.JsonPath._
import io.circe.syntax._
import io.circe.{Decoder, DecodingFailure, HCursor, Json}
import fs2._
import org.http4s.Method._
import org.http4s._
import org.http4s.circe._
import org.http4s.client.dsl.Http4sClientDsl

import scala.language.higherKinds

trait RateLimitClient[F[_]] {
  def list(zoneId: String): Stream[F, RateLimit]
  def getById(zoneId: String, rateLimitId: String): Stream[F, RateLimit]
  def create(zoneId: String, rateLimit: CreateRateLimit): Stream[F, RateLimit]
  def update(zoneId: String, rateLimit: RateLimit): Stream[F, RateLimit]
  def delete(zoneId: String, rateLimitId: String): Stream[F, String]
}

object RateLimitClient {
  def apply[F[_] : Sync](executor: StreamingCloudflareApiExecutor[F]): RateLimitClient[F] = new RateLimitClientImpl[F](executor)
}

class RateLimitClientImpl[F[_] : Sync](executor: StreamingCloudflareApiExecutor[F]) extends RateLimitClient[F] with Http4sClientDsl[F] {
  implicit val rateLimitActionDtoDecoder: Decoder[RateLimitActionDTO] = new Decoder[RateLimitActionDTO] {
    override def apply(c: HCursor): Decoder.Result[RateLimitActionDTO] =
      for {
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
  }

  def list(zoneId: String): Stream[F, RateLimit] = {
    for {
      req ← Stream.eval(GET(BaseUrl / "zones" / zoneId / "rate_limits"))
      record ← executor.fetch[RateLimitDTO](req)
    } yield record
  }

  def getById(zoneId: String, rateLimitId: String): Stream[F, RateLimit] =
    for {
      req ← Stream.eval(GET(BaseUrl / "zones" / zoneId / "rate_limits" / rateLimitId))
      res ← executor.fetch[RateLimitDTO](req)
    } yield res

  def create(zoneId: String, rateLimit: CreateRateLimit): Stream[F, RateLimit] = {
    for {
      req ← Stream.eval(POST(BaseUrl / "zones" / zoneId / "rate_limits", rateLimit.asJson))
      resp ← createOrUpdate(req)
    } yield resp
  }

  def update(zoneId: String, rateLimit: RateLimit): Stream[F, RateLimit] = {
    for {
      req ← Stream.eval(PUT(BaseUrl / "zones" / zoneId / "rate_limits" / rateLimit.id, toDto(rateLimit).asJson))
      resp ← createOrUpdate(req)
    } yield resp
  }

  def delete(zoneId: String, rateLimitId: String): Stream[F, String] = Stream.eval(deleteF(zoneId, rateLimitId))

  private def deleteF(zoneId: String, rateLimitId: String): F[String] =
    for {
      req ← DELETE(BaseUrl / "zones" / zoneId / "rate_limits" / rateLimitId)
      id ← executor.raw(req) { res ⇒
        for {
          json ← res.decodeJson[Json]
          output ← handleDeleteResponseJson(json, res.status, zoneId, rateLimitId)
        } yield output
      }
    } yield id

  private def handleDeleteResponseJson(json: Json, status: Status, zoneId: String, rateLimitId: String): F[String] =
    if (status.isSuccess)
      deletedRecordLens(json).fold(Applicative[F].pure(rateLimitId))(Applicative[F].pure)
    else {
      if (status == Status.NotFound)
        Sync[F].raiseError(RateLimitDoesNotExistException(zoneId, rateLimitId))
      else
        Sync[F].raiseError(UnexpectedCloudflareErrorException(errorsLens(json)))
    }

  private def createOrUpdate(request: Request[F]): Stream[F, RateLimit] = Stream.eval(createOrUpdateF(request))

  private def createOrUpdateF(request: Request[F]): F[RateLimit] = {
    executor.raw(request) { res ⇒
      for {
        json ← res.decodeJson[Json]
        output ← handleCreateUpdateResponseJson(json, res.status)
      } yield output
    }
  }

  private def handleCreateUpdateResponseJson(json: Json, status: Status): F[RateLimit] =
    if (status.isSuccess)
      Applicative[F].pure(rateLimitLens(json))
    else {
      Sync[F].raiseError(UnexpectedCloudflareErrorException(errorsLens(json)))
    }

  private val deletedRecordLens: Json ⇒ Option[String] = root.result.id.string.getOption
  private val rateLimitLens: Json ⇒ RateLimit = root.result.as[RateLimitDTO].getOption(_).get
  private val errorsLens: Json ⇒ List[Error] = root.errors.each.as[model.Error].getAll
}

case class RateLimitDoesNotExistException(zoneId: String, rateLimitId: String) extends RuntimeException(
  s"The rate limit $rateLimitId not found for zone $zoneId."
)