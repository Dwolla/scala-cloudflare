package com.dwolla.cloudflare.clients

import cats._
import cats.implicits._
import com.dwolla.cloudflare.CloudflareApiExecutor
import com.dwolla.cloudflare.common.JsonEntity._
import com.dwolla.cloudflare.common.UriHelper
import com.dwolla.cloudflare.domain.dto.ratelimits._
import com.dwolla.cloudflare.domain.dto.{PagedResponseDTO, ResponseDTO}
import com.dwolla.cloudflare.domain.model.DeletedRecord
import com.dwolla.cloudflare.domain.model.Exceptions.UnexpectedCloudflareErrorException
import com.dwolla.cloudflare.domain.model.ratelimits.Implicits._
import com.dwolla.cloudflare.domain.model.ratelimits._
import com.dwolla.cloudflare.domain.model.response.Implicits._
import com.dwolla.cloudflare.domain.model.response.PagedResponse
import org.apache.http.client.methods._
import org.json4s.native.parseJson
import org.json4s.{DefaultFormats, Formats}

import scala.language.higherKinds

class RateLimitClient[F[_] : Monad](executor: CloudflareApiExecutor[F]) {
  protected implicit val formats: Formats = DefaultFormats

  def list(zoneId: String, page: Int = 1, perPage: Int = 25): F[PagedResponse[Set[RateLimit]]] = {
    val parameters = UriHelper.buildParameterString(Seq(Option("page" → page), Option("per_page" → perPage)))
    val request: HttpGet = new HttpGet(UriHelper.buildApiUri(s"zones/$zoneId/rate_limits", Some(parameters)))

    executor.fetch(request) { response ⇒
      parseJson(response.getEntity.getContent).extract[PagedResponseDTO[Set[RateLimitDTO]]]
    }
  }

  def listAll(zoneId: String): F[Set[RateLimit]] = {
    list(zoneId, 1).flatMap { pagedResponse ⇒
      val totalPages = pagedResponse.paging.totalPages
      val firstLimits = pagedResponse.result

      (2 to totalPages).toList.traverse(page ⇒ list(zoneId, page))
        .map(responses ⇒ responses.map(_.result))
        .map(limits ⇒ limits.foldLeft(firstLimits) {
          (accumulated, a) ⇒ accumulated ++ a
        })
    }
  }

  def getById(zoneId: String, rateLimitId: String): F[Option[RateLimit]] = {
    val request: HttpGet = new HttpGet(buildRateLimitUri(zoneId, rateLimitId))

    executor.fetch(request) { response ⇒
      parseJson(response.getEntity.getContent).extract[ResponseDTO[Option[RateLimitDTO]]].result.map(toModel)
    }
  }

  def create(zoneId: String, rateLimit: CreateRateLimit): F[RateLimit] = {
    val request: HttpPost = new HttpPost(UriHelper.buildApiUri(s"zones/$zoneId/rate_limits"))
    request.setEntity(rateLimit)

    createOrUpdate(request)
  }

  def update(zoneId: String, rateLimit: RateLimit): F[RateLimit] = {
    val request: HttpPut = new HttpPut(buildRateLimitUri(zoneId, rateLimit.id))
    request.setEntity(rateLimit)

    createOrUpdate(request)
  }

  def delete(zoneId: String, rateLimitId: String): F[String] = {
    val request: HttpDelete = new HttpDelete(buildRateLimitUri(zoneId, rateLimitId))
    executor.fetch(request) { response ⇒
      val r = parseJson(response.getEntity.getContent).extract[ResponseDTO[Option[DeletedRecord]]]

      response.getStatusLine.getStatusCode match {
        case 200 ⇒
          // This endpoint returns the "result" field as null currently, so just return the supplied rate limit id if that happens.
          r.result.fold(rateLimitId) { del ⇒ del.id }
        case 404 ⇒
          throw RateLimitDoesNotExistException(zoneId, rateLimitId)
        case _ ⇒
          throw UnexpectedCloudflareErrorException(r.errors.get)
      }
    }
  }

  private def createOrUpdate(request: HttpRequestBase): F[RateLimit] = {
    executor.fetch(request) { response ⇒
      val r = parseJson(response.getEntity.getContent).extract[ResponseDTO[RateLimitDTO]]

      response.getStatusLine.getStatusCode match {
        case 200 ⇒
          r.result
        case _ ⇒
          throw UnexpectedCloudflareErrorException(r.errors.get)
      }
    }
  }

  private def buildRateLimitUri(zoneId: String, rateLimitId: String): String = {
    UriHelper.buildApiUri(s"zones/$zoneId/rate_limits/$rateLimitId")
  }
}

case class RateLimitDoesNotExistException(zoneId: String, rateLimitId: String) extends RuntimeException(
  s"The rate limit $rateLimitId not found for zone $zoneId."
)