package com.dwolla.cloudflare

import cats.Monad
import com.dwolla.cloudflare.common.JsonEntity._
import com.dwolla.cloudflare.domain.dto.ratelimits.RateLimitDTO
import com.dwolla.cloudflare.domain.model.Error
import com.dwolla.cloudflare.domain.model.Exceptions.UnexpectedCloudflareErrorException
import com.dwolla.cloudflare.domain.model.ratelimits.Implicits._
import com.dwolla.cloudflare.domain.model.ratelimits.{CreateRateLimit, RateLimit}
import org.apache.http.client.methods._
import org.json4s.native.parseJson
import org.json4s.{DefaultFormats, Formats}

import scala.language.higherKinds

class RateLimitClient[F[_] : Monad](executor: CloudflareApiExecutor[F]) {
  protected implicit val formats: Formats = DefaultFormats

  def listRateLimits(zoneId: String): F[Set[RateLimit]] = {
    val request: HttpGet = new HttpGet(s"https://api.cloudflare.com/client/v4/zones/$zoneId/rate_limits")

    executor.fetch(request) { response ⇒
      (parseJson(response.getEntity.getContent) \ "result").extract[Set[RateLimitDTO]].map(toModel)
    }
  }

  def getById(zoneId: String, rateLimitId: String): F[Option[RateLimit]] = {
    val request: HttpGet = new HttpGet(s"https://api.cloudflare.com/client/v4/zones/$zoneId/rate_limits/$rateLimitId")

    executor.fetch(request) { response ⇒
      (parseJson(response.getEntity.getContent) \ "result").extract[Option[RateLimitDTO]].map(toModel)
    }
  }

  def createRateLimit(zoneId: String, rateLimit: CreateRateLimit): F[RateLimit] = {
    val request: HttpPost = new HttpPost(s"https://api.cloudflare.com/client/v4/zones/$zoneId/rate_limits")
    request.setEntity(rateLimit)

    createOrUpdate(request)
  }

  def updateRateLimit(zoneId: String, rateLimit: RateLimit): F[RateLimit] = {
    val request: HttpPut = new HttpPut(s"https://api.cloudflare.com/client/v4/zones/$zoneId/rate_limits/${rateLimit.id}")
    request.setEntity(rateLimit)

    createOrUpdate(request)
  }

  def deleteRateLimit(zoneId: String, rateLimitId: String): F[String] = {
    val request: HttpDelete = new HttpDelete(s"https://api.cloudflare.com/client/v4/zones/$zoneId/rate_limits/$rateLimitId")
    executor.fetch(request) { response ⇒
      val parsedJson = parseJson(response.getEntity.getContent)

      response.getStatusLine.getStatusCode match {
        case 200 ⇒
          // This endpoint returns the "result" field as null currently, so just return the supplied rate limit id.
          rateLimitId
        case 404 ⇒
          throw RateLimitDoesNotExistException(zoneId, rateLimitId)
        case _ ⇒
          throw UnexpectedCloudflareErrorException((parsedJson \ "errors").extract[List[Error]])
      }
    }
  }

  private def createOrUpdate(request: HttpRequestBase): F[RateLimit] = {
    executor.fetch(request) { response ⇒
      val parsedJson = parseJson(response.getEntity.getContent)

      response.getStatusLine.getStatusCode match {
        case 200 ⇒
          (parsedJson \ "result").extract[RateLimitDTO]
        case _ ⇒
          throw UnexpectedCloudflareErrorException((parsedJson \ "errors").extract[List[Error]])
      }
    }
  }
}

case class RateLimitDoesNotExistException(zoneId: String, rateLimitId: String) extends RuntimeException(
  s"The rate limit $rateLimitId not found for zone $zoneId."
)