package dwolla.cloudflare

import java.net.URI

import cats.implicits._
import com.dwolla.cloudflare.domain.model.Exceptions.UnexpectedCloudflareErrorException
import com.dwolla.cloudflare.domain.model.ratelimits._
import com.dwolla.cloudflare.{CloudflareAuthorization, FutureCloudflareApiExecutor, RateLimitClient, RateLimitDoesNotExistException}
import dwolla.testutils.httpclient.SimpleHttpRequestMatcher.http
import org.apache.http.HttpVersion.HTTP_1_1
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.{HttpDelete, HttpGet, HttpPost, HttpPut}
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.message.BasicStatusLine
import org.json4s.DefaultFormats
import org.specs2.concurrent.ExecutionEnv
import org.specs2.matcher.JsonMatchers
import org.specs2.mock.Mockito
import org.specs2.mock.mockito.ArgumentCapture
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

import scala.concurrent.Future
import scala.io.Source

class RateLimitClientSpec(implicit ee: ExecutionEnv) extends Specification with Mockito with JsonMatchers with HttpClientHelper {
  trait Setup extends Scope {
    implicit val formats = DefaultFormats
    implicit val mockHttpClient = mock[CloseableHttpClient]
    val fakeExecutor = new FutureCloudflareApiExecutor(CloudflareAuthorization("email", "key")) {
      override lazy val httpClient: CloseableHttpClient = mockHttpClient
    }

    val zoneId = "zone-id"

    val client = new RateLimitClient(fakeExecutor)
  }

  "listRateLimits" should {
    "return configured rate limits" in new Setup {
      mockListRateLimits(zoneId, SampleResponses.Successes.listRateLimits)

      val output: Future[Set[RateLimit]] = client.listRateLimits(zoneId)
      output must be_==(Set(
        RateLimit(
          id = "fake-rate-limit-1",
          disabled = Some(false),
          description = Some("Rate Limit"),
          trafficMatch = RateLimitMatch(
            request = RateLimitMatchRequest(
              methods = Some(List("POST")),
              schemes = Some(List("_ALL_")),
              url = "*.test.com/test/v2/*"
            )
          ),
          threshold = 300,
          period = 60,
          action = RateLimitAction(
            mode = "ban",
            timeout = 60
          )
        ),
        RateLimit(
          id = "fake-rate-limit-2",
          trafficMatch = RateLimitMatch(
            request = RateLimitMatchRequest(
              methods = Some(List("_ALL_")),
              schemes = Some(List("_ALL_")),
              url = "*.anothertest.com/*"
            ),
            response = Some(RateLimitMatchResponse(
              originTraffic = Some(true),
              headers = List(RateLimitMatchResponseHeader(
                name = "Cf-Cache-Status",
                op = "ne",
                value = "HIT"
              ))
            ))
          ),
          bypass = Some(List(RateLimitKeyValue(
            name = "url",
            value = "api.anothertest.com/*"
          ))),
          threshold = 50,
          period = 600,
          action = RateLimitAction(
            mode = "ban",
            timeout = 60,
            response = Some(RateLimitActionResponse(
              contentType = "application/json",
              body = "{\n  \"code\": \"TooManyRequests\",\n  \"message\": \"Your number of requests has exceeded the limit allowed for the last 1 minute. Your client will be allowed to continue making requests after the specified time in the `Retry-After` header.\"\n}"
            ))
          )
        )
      )).await
    }
  }

  "getById" should {
    "return rate limit by id" in new Setup {
      val rateLimitId = "fake-rate-limit-1"

      mockGetRateLimitById(zoneId, rateLimitId, SampleResponses.Successes.rateLimit)

      val output: Future[Option[RateLimit]] = client.getById(zoneId, rateLimitId)
      output must beSome(
        RateLimit(
          id = rateLimitId,
          disabled = Some(false),
          description = Some("Rate Limit"),
          trafficMatch = RateLimitMatch(
            request = RateLimitMatchRequest(
              methods = Some(List("POST")),
              schemes = Some(List("_ALL_")),
              url = "*.test.com/test/v2/*"
            )
          ),
          threshold = 300,
          period = 60,
          action = RateLimitAction(
            mode = "ban",
            timeout = 60
          )
        )
      ).await
    }

    "return None if not found" in new Setup {
      val rateLimitId = "missing-id"

      val failure = SampleResponses.Failures.rateLimitDoesNotExist
      val captor = mockExecuteWithCaptor[HttpGet](fakeResponse(new BasicStatusLine(HTTP_1_1, failure.statusCode, "Not Found"), new StringEntity(failure.json)))

      val output = client.getById(zoneId, rateLimitId)
      output must beNone.await

      val httpGet: HttpGet = captor.value
      httpGet.getMethod must_== "GET"
      httpGet.getURI must_== new URI(s"https://api.cloudflare.com/client/v4/zones/$zoneId/rate_limits/$rateLimitId")
    }
  }

  "createRateLimit" should {
    "create new rate limit" in new Setup {
      val rateLimitId = "fake-rate-limit-1"

      val createRateLimit = CreateRateLimit(
        disabled = Some(false),
        description = Some("Rate Limit"),
        trafficMatch = RateLimitMatch(
          request = RateLimitMatchRequest(
            methods = Some(List("POST")),
            schemes = Some(List("_ALL_")),
            url = "*.test.com/test/v2/*"
          )
        ),
        threshold = 300,
        period = 60,
        action = RateLimitAction(
          mode = "ban",
          timeout = 60
        )
      )

      val captor: ArgumentCapture[HttpPost] = mockExecuteWithCaptor[HttpPost](fakeResponse(new BasicStatusLine(HTTP_1_1, 200, "Ok"), new StringEntity(SampleResponses.Successes.rateLimit)))

      val output: Future[RateLimit] = client.createRateLimit(zoneId, createRateLimit)
      output must be_==(
        RateLimit(
          id = rateLimitId,
          disabled = Some(false),
          description = Some("Rate Limit"),
          trafficMatch = RateLimitMatch(
            request = RateLimitMatchRequest(
              methods = Some(List("POST")),
              schemes = Some(List("_ALL_")),
              url = "*.test.com/test/v2/*"
            )
          ),
          threshold = 300,
          period = 60,
          action = RateLimitAction(
            mode = "ban",
            timeout = 60
          )
        )
      ).await

      val httpPost: HttpPost = captor.value
      httpPost.getMethod must_== "POST"
      httpPost.getURI must_== new URI(s"https://api.cloudflare.com/client/v4/zones/$zoneId/rate_limits")

      private val httpEntity = httpPost.getEntity

      httpEntity.getContentType.getValue must_== "application/json"
      val postedJson: String = Source.fromInputStream(httpEntity.getContent).mkString

      postedJson must /("disabled" → createRateLimit.disabled.get)
      postedJson must /("description" → createRateLimit.description.get)
      postedJson must /("match") /("request") /("url" → createRateLimit.trafficMatch.request.url)
      postedJson must (/("match") / ("request") /("methods")).andHave(exactly[String]("POST"))
      postedJson must (/("match") /("request") /("schemes")).andHave(exactly[String]("_ALL_"))
      postedJson must /("threshold" → 300)
      postedJson must /("period" → 60)
      postedJson must /("action") /("mode" → "ban")
      postedJson must /("action") /("timeout" → 60)

    }

    "throw unexpected exception if error adding new member" in new Setup {
      val createRateLimit = CreateRateLimit(
        disabled = Some(false),
        description = Some("Rate Limit"),
        trafficMatch = RateLimitMatch(
          request = RateLimitMatchRequest(
            methods = Some(List("POST")),
            schemes = Some(List("_ALL_")),
            url = "*.test.com/test/v2/*"
          )
        ),
        threshold = 300,
        period = 60,
        action = RateLimitAction(
          mode = "ban",
          timeout = 60
        )
      )

      val failure = SampleResponses.Failures.rateLimitCreationError
      val captor: ArgumentCapture[HttpPost] = mockExecuteWithCaptor[HttpPost](fakeResponse(new BasicStatusLine(HTTP_1_1, failure.statusCode, "Bad Request"), new StringEntity(failure.json)))

      client.createRateLimit(zoneId, createRateLimit) must throwA[UnexpectedCloudflareErrorException].like {
        case ex ⇒ ex.getMessage must_==
          """An unexpected Cloudflare error occurred. Errors:
            |
            | - Error(1001,ratelimit.api.validation_error:threshold is too low and must be at least 1,sample_rate is too low and must be at least 1 second,'' is not a valid action)
            |     """.stripMargin
      }.await
    }
  }

  "updateRateLimit" should {
    "update existing rate limit" in new Setup {
      val rateLimitId = "fake-rate-limit-1"

      val updatedRateLimit = RateLimit(
        id = rateLimitId,
        disabled = Some(true),
        description = Some("Updated Rate Limit"),
        trafficMatch = RateLimitMatch(
          request = RateLimitMatchRequest(
            methods = Some(List("POST", "GET")),
            schemes = Some(List("_ALL_")),
            url = "*.test.com/test/v3/*"
          )
        ),
        threshold = 600,
        period = 30,
        action = RateLimitAction(
          mode = "challenge",
          timeout = 20
        )
      )

      val captor: ArgumentCapture[HttpPut] = mockExecuteWithCaptor[HttpPut](fakeResponse(new BasicStatusLine(HTTP_1_1, 200, "Ok"), new StringEntity(SampleResponses.Successes.updatedRateLimit)))

      val output: Future[RateLimit] = client.updateRateLimit(zoneId, updatedRateLimit)
      output must be_==(updatedRateLimit).await

      val httpPut: HttpPut = captor.value
      httpPut.getMethod must_== "PUT"
      httpPut.getURI must_== new URI(s"https://api.cloudflare.com/client/v4/zones/$zoneId/rate_limits/$rateLimitId")

      private val httpEntity = httpPut.getEntity

      httpEntity.getContentType.getValue must_== "application/json"
      val putJson: String = Source.fromInputStream(httpEntity.getContent).mkString

      putJson must /("id" → rateLimitId)
      putJson must /("disabled" → updatedRateLimit.disabled.get)
      putJson must /("description" → updatedRateLimit.description.get)
      putJson must /("match") /("request") /("url" → updatedRateLimit.trafficMatch.request.url)
      putJson must (/("match") / ("request") /("methods")).andHave(exactly[String]("POST", "GET"))
      putJson must (/("match") /("request") /("schemes")).andHave(exactly[String]("_ALL_"))
      putJson must /("threshold" → 600)
      putJson must /("period" → 30)
      putJson must /("action") /("mode" → "challenge")
      putJson must /("action") /("timeout" → 20)

    }

    "throw unexpected exception if error updating existing rate limit" in new Setup {
      val rateLimitId = "fake-rate-limit-1"

      val updatedRateLimit = RateLimit(
        id = rateLimitId,
        disabled = Some(true),
        description = Some("Updated Rate Limit"),
        trafficMatch = RateLimitMatch(
          request = RateLimitMatchRequest(
            methods = Some(List("POST", "GET")),
            schemes = Some(List("_ALL_")),
            url = "*.test.com/test/v3/*"
          )
        ),
        threshold = 0,
        period = 30,
        action = RateLimitAction(
          mode = "challenge",
          timeout = 20
        )
      )

      val failure = SampleResponses.Failures.rateLimitUpdateError
      val captor: ArgumentCapture[HttpPut] = mockExecuteWithCaptor[HttpPut](fakeResponse(new BasicStatusLine(HTTP_1_1, failure.statusCode, "Bad Request"), new StringEntity(failure.json)))

      client.updateRateLimit(zoneId, updatedRateLimit) must throwA[UnexpectedCloudflareErrorException].like {
        case ex ⇒ ex.getMessage must_==
          """An unexpected Cloudflare error occurred. Errors:
            |
            | - Error(1001,ratelimit.api.validation_error:threshold is too low and must be at least 1)
            |     """.stripMargin
      }.await
    }
  }

  "deleteRateLimit" should {
    "delete rate limit from zone" in new Setup {
      val rateLimitId = "fake-rate-limit-1"

      val captor: ArgumentCapture[HttpDelete] = mockExecuteWithCaptor[HttpDelete](fakeResponse(new BasicStatusLine(HTTP_1_1, 200, "Ok"), new StringEntity(SampleResponses.Successes.removedRateLimit)))

      val output: Future[String] = client.deleteRateLimit(zoneId, rateLimitId)
      output must be_==(rateLimitId).await

      val httpDelete: HttpDelete = captor.value
      httpDelete.getMethod must_== "DELETE"
      httpDelete.getURI must_== new URI(s"https://api.cloudflare.com/client/v4/zones/$zoneId/rate_limits/$rateLimitId")
    }

    "throw unexpected exception if error deleting rate limit" in new Setup {
      val rateLimitId = "fake-rate-limit-1"

      val failure = SampleResponses.Failures.rateLimitDeleteError
      val captor: ArgumentCapture[HttpDelete] = mockExecuteWithCaptor[HttpDelete](fakeResponse(new BasicStatusLine(HTTP_1_1, failure.statusCode, "Bad Request"), new StringEntity(failure.json)))

      client.deleteRateLimit(zoneId, rateLimitId) must throwA[UnexpectedCloudflareErrorException].like {
        case ex ⇒ ex.getMessage must_==
          """An unexpected Cloudflare error occurred. Errors:
            |
            | - Error(7003,Could not route to /accounts/90940840480ba654a3a5ddcdc5d741f9/rate_limits/8b0bcff938734f359ee12aa788b7ea38, perhaps your object identifier is invalid?)
            | - Error(7000,No route for that URI)
            |     """.stripMargin
      }.await
    }

    "throw not found exception if rate limit not in zone" in new Setup {
      val rateLimitId = "fake-rate-limit-1"

      val failure = SampleResponses.Failures.rateLimitDoesNotExist
      val captor: ArgumentCapture[HttpDelete] = mockExecuteWithCaptor[HttpDelete](fakeResponse(new BasicStatusLine(HTTP_1_1, failure.statusCode, "Not Found"), new StringEntity(failure.json)))

      client.deleteRateLimit(zoneId, rateLimitId) must throwA[RateLimitDoesNotExistException].like {
        case ex ⇒ ex.getMessage must_==
          s"The rate limit $rateLimitId not found for zone $zoneId."
      }.await
    }
  }

  def mockListRateLimits(zoneId: String, responseBody: String)(implicit mockHttpClient: HttpClient): Unit = {
    val response = fakeResponse(new BasicStatusLine(HTTP_1_1, 200, "Ok"), new StringEntity(responseBody))
    mockHttpClient.execute(http(new HttpGet(s"https://api.cloudflare.com/client/v4/zones/$zoneId/rate_limits"))) returns response
  }

  def mockGetRateLimitById(zoneId: String, rateLimitId: String, responseBody: String)(implicit mockHttpClient: HttpClient): Unit = {
    val response = fakeResponse(new BasicStatusLine(HTTP_1_1, 200, "Ok"), new StringEntity(responseBody))
    mockHttpClient.execute(http(new HttpGet(s"https://api.cloudflare.com/client/v4/zones/$zoneId/rate_limits/$rateLimitId"))) returns response
  }

  private object SampleResponses {
    object Successes {
      val listRateLimits =
        """{
          |  "result": [
          |    {
          |      "id": "fake-rate-limit-1",
          |      "disabled": false,
          |      "description": "Rate Limit",
          |      "match":
          |      {
          |        "request":
          |        {
          |          "methods": ["POST"],
          |          "schemes": ["_ALL_"],
          |          "url": "*.test.com/test/v2/*"
          |        }
          |      },
          |      "threshold": 300,
          |      "period": 60,
          |      "action":
          |      {
          |        "mode": "ban",
          |        "timeout": 60
          |      }
          |    },
          |    {
          |      "id": "fake-rate-limit-2",
          |      "match":
          |      {
          |        "request":
          |        {
          |          "methods": ["_ALL_"],
          |          "schemes": ["_ALL_"],
          |          "url": "*.anothertest.com/*"
          |        },
          |        "response":
          |        {
          |          "origin_traffic": true,
          |          "headers":
          |          [
          |            {
          |              "name": "Cf-Cache-Status",
          |              "op": "ne",
          |              "value": "HIT"
          |            }
          |          ]
          |        }
          |      },
          |      "bypass":
          |      [
          |        {
          |          "name": "url",
          |          "value": "api.anothertest.com/*"
          |        }
          |      ],
          |      "threshold": 50,
          |      "period": 600,
          |      "action":
          |      {
          |        "mode": "ban",
          |        "timeout": 60,
          |        "response":
          |        {
          |          "content_type": "application/json",
          |          "body": "{\n  \"code\": \"TooManyRequests\",\n  \"message\": \"Your number of requests has exceeded the limit allowed for the last 1 minute. Your client will be allowed to continue making requests after the specified time in the `Retry-After` header.\"\n}"
          |        }
          |      }
          |    }
          |  ],
          |  "result_info": {
          |    "page": 1,
          |    "per_page": 20,
          |    "total_pages": 1,
          |    "count": 2,
          |    "total_count": 2
          |  },
          |  "success": true,
          |  "errors": [],
          |  "messages": []
          |}
        """.stripMargin

      val rateLimit =
        """{
          |  "success": true,
          |  "errors": [],
          |  "messages": [],
          |  "result": {
          |    "id": "fake-rate-limit-1",
          |    "disabled": false,
          |    "description": "Rate Limit",
          |    "match":
          |    {
          |      "request":
          |      {
          |        "methods": ["POST"],
          |        "schemes": ["_ALL_"],
          |        "url": "*.test.com/test/v2/*"
          |      }
          |    },
          |    "threshold": 300,
          |    "period": 60,
          |    "action":
          |    {
          |      "mode": "ban",
          |      "timeout": 60
          |    }
          |  }
          |}
        """.stripMargin

      val updatedRateLimit =
        """{
          |  "success": true,
          |  "errors": [],
          |  "messages": [],
          |  "result": {
          |    "id": "fake-rate-limit-1",
          |    "disabled": true,
          |    "description": "Updated Rate Limit",
          |    "match":
          |    {
          |      "request":
          |      {
          |        "methods": ["POST", "GET"],
          |        "schemes": ["_ALL_"],
          |        "url": "*.test.com/test/v3/*"
          |      }
          |    },
          |    "threshold": 600,
          |    "period": 30,
          |    "action":
          |    {
          |      "mode": "challenge",
          |      "timeout": 20
          |    }
          |  }
          |}
        """.stripMargin

      val removedRateLimit =
        """
          |{
          |  "result": {
          |    "id": "fake-rate-limit-1"
          |  },
          |  "success": true,
          |  "errors": [],
          |  "messages": []
          |}
        """.stripMargin
    }

    object Failures {
      case class Failure(statusCode: Int, json: String)

      val rateLimitDoesNotExist = Failure(404,
        """{
          |  "result": null,
          |  "success": false,
          |  "errors": [
          |    {
          |      "code": 10001,
          |      "message": "ratelimit.api.not_found"
          |    }
          |  ],
          |  "messages": []
          |}
        """.stripMargin)

      val rateLimitCreationError = Failure(400,
        """{
          |  "success": false,
          |  "errors": [
          |    {
          |      "code": 1001,
          |      "message": "ratelimit.api.validation_error:threshold is too low and must be at least 1,sample_rate is too low and must be at least 1 second,'' is not a valid action"
          |    }
          |  ],
          |  "messages": [],
          |  "result": null
          |}
        """.stripMargin)

      val rateLimitUpdateError = Failure(400,
        """{
          |  "success": false,
          |  "errors": [
          |    {
          |      "code": 1001,
          |      "message": "ratelimit.api.validation_error:threshold is too low and must be at least 1"
          |    }
          |  ],
          |  "messages": [],
          |  "result": null
          |}
        """.stripMargin)

      val rateLimitDeleteError = Failure(400,
        """{
          |  "success": false,
          |  "errors": [
          |    {
          |      "code": 7003,
          |      "message": "Could not route to /accounts/90940840480ba654a3a5ddcdc5d741f9/rate_limits/8b0bcff938734f359ee12aa788b7ea38, perhaps your object identifier is invalid?"
          |    },
          |    {
          |      "code": 7000,
          |      "message": "No route for that URI"
          |    }
          |  ],
          |  "messages": [],
          |  "result": null
          |}
        """.stripMargin)
    }
  }
}
