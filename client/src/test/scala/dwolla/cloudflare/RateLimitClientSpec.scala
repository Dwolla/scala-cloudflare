package dwolla.cloudflare

import cats.effect.Sync
import com.dwolla.cloudflare._
import com.dwolla.cloudflare.domain.model.Exceptions.UnexpectedCloudflareErrorException
import com.dwolla.cloudflare.domain.model.ratelimits._
import org.http4s.Status
import org.http4s.client.Client
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

class RateLimitClientSpec extends Specification {
  trait Setup extends Scope {
    val zoneId = "zone-id"

    val authorization = CloudflareAuthorization("email", "key")
    val fakeService = new FakeCloudflareService(authorization)
  }

  "list" should {
    "list rate limits" in new Setup {
      val http4sClient = fakeService.client(fakeService.listRateLimits(Map(1 → SampleResponses.Successes.listRateLimitsPage1, 2 → SampleResponses.Successes.listRateLimitsPage2, 3 → SampleResponses.Successes.listRateLimitsPage3), zoneId))
      val client = buildRateLimitClient(http4sClient, authorization)

      val output: List[RateLimit] = client.list(zoneId).compile.toList.unsafeRunSync()
      output must be_==(
        List(
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
          ),
          RateLimit(
            id = "fake-rate-limit-3",
            description = Some("Third Rate Limit"),
            trafficMatch = RateLimitMatch(
              request = RateLimitMatchRequest(
                methods = Some(List("PUT")),
                schemes = Some(List("_ALL_")),
                url = "*.thirdtest.com/*"
              )
            ),
            threshold = 20,
            period = 6000,
            action = RateLimitAction(
              mode = "ban",
              timeout = 120
            )
          )
        )
      )
    }

    "list rate limits across pages doesn't fetch eagerly" in new Setup {
      val http4sClient = fakeService.client(fakeService.listRateLimits(Map(1 → SampleResponses.Successes.listRateLimitsPage1), zoneId))
      val client = buildRateLimitClient(http4sClient, authorization)

      val output: List[RateLimit] = client.list(zoneId).take(1).compile.toList.unsafeRunSync()
      output must be_==(
        List(
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
          )
        )
      )
    }
  }

  "getById" should {
    "get rate limit by id" in new Setup {
      val rateLimitId = "fake-rate-limit-1"

      val http4sClient = fakeService.client(fakeService.rateLimitById(SampleResponses.Successes.rateLimit, zoneId, rateLimitId))
      val client = buildRateLimitClient(http4sClient, authorization)

      val output: Option[RateLimit] = client.getById(zoneId, rateLimitId)
        .compile.toList.map(_.headOption).unsafeRunSync()

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
      )
    }

    "return None if not found" in new Setup {
      val rateLimitId = "missing-id"

      val failure = SampleResponses.Failures.rateLimitDoesNotExist
      val http4sClient = fakeService.client(fakeService.rateLimitById(failure.json, zoneId, rateLimitId, failure.status))
      val client = buildRateLimitClient(http4sClient, authorization)

      val output: Option[RateLimit] = client.getById(zoneId, rateLimitId)
        .compile.toList.map(_.headOption).unsafeRunSync()

      output must beNone
    }
  }

  "create" should {
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

      val http4sClient = fakeService.client(fakeService.createRateLimit(SampleResponses.Successes.rateLimit, zoneId))
      val client = buildRateLimitClient(http4sClient, authorization)

      val output = client.create(zoneId, createRateLimit)
        .compile.toList.unsafeRunSync()

      output must be_==(
        List(RateLimit(
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
        ))
      )
    }

    "throw unexpected exception if error creating rate limit" in new Setup {
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
      val http4sClient = fakeService.client(fakeService.createRateLimit(failure.json, zoneId, failure.status))
      val client = buildRateLimitClient(http4sClient, authorization)

      val output = client.create(zoneId, createRateLimit)
        .compile
        .toList
        .attempt
        .unsafeRunSync()

      output must beLeft[Throwable].like {
        case ex: UnexpectedCloudflareErrorException ⇒ ex.getMessage must_==
          """An unexpected Cloudflare error occurred. Errors:
            |
            | - Error(1001,ratelimit.api.validation_error:threshold is too low and must be at least 1,sample_rate is too low and must be at least 1 second,'' is not a valid action)
            |     """.stripMargin
      }
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

      val http4sClient = fakeService.client(fakeService.updateRateLimit(SampleResponses.Successes.updatedRateLimit, zoneId, updatedRateLimit.id))
      val client = buildRateLimitClient(http4sClient, authorization)

      val output = client.update(zoneId, updatedRateLimit)
        .compile.toList.unsafeRunSync()

      output must be_==(List(updatedRateLimit))
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
      val http4sClient = fakeService.client(fakeService.updateRateLimit(failure.json, zoneId, updatedRateLimit.id, failure.status))
      val client = buildRateLimitClient(http4sClient, authorization)

      val output = client.update(zoneId, updatedRateLimit)
        .compile
        .toList
        .attempt
        .unsafeRunSync()

      output must beLeft[Throwable].like {
        case ex: UnexpectedCloudflareErrorException ⇒ ex.getMessage must_==
          """An unexpected Cloudflare error occurred. Errors:
            |
            | - Error(1001,ratelimit.api.validation_error:threshold is too low and must be at least 1)
            |     """.stripMargin
      }
    }
  }

  "deleteRateLimit" should {
    "delete rate limit from zone" in new Setup {
      val rateLimitId = "fake-rate-limit-1"

      val http4sClient = fakeService.client(fakeService.deleteRateLimit(SampleResponses.Successes.removedRateLimit, zoneId, rateLimitId))
      val client = buildRateLimitClient(http4sClient, authorization)

      val output = client.delete(zoneId, rateLimitId)
        .compile.toList.unsafeRunSync()

      output must be_==(List(rateLimitId))
    }

    "throw unexpected exception if error deleting rate limit" in new Setup {
      val rateLimitId = "fake-rate-limit-1"

      val failure = SampleResponses.Failures.rateLimitDeleteError
      val http4sClient = fakeService.client(fakeService.deleteRateLimit(failure.json, zoneId, rateLimitId, failure.status))
      val client = buildRateLimitClient(http4sClient, authorization)

      val output = client.delete(zoneId, rateLimitId)
        .compile
        .toList
        .attempt
        .unsafeRunSync()

      output must beLeft[Throwable].like {
        case ex: UnexpectedCloudflareErrorException ⇒ ex.getMessage must_==
          """An unexpected Cloudflare error occurred. Errors:
            |
            | - Error(7003,Could not route to /accounts/90940840480ba654a3a5ddcdc5d741f9/rate_limits/8b0bcff938734f359ee12aa788b7ea38, perhaps your object identifier is invalid?)
            | - Error(7000,No route for that URI)
            |     """.stripMargin
      }
    }

    "throw not found exception if rate limit not in zone" in new Setup {
      val rateLimitId = "fake-rate-limit-1"

      val failure = SampleResponses.Failures.rateLimitDoesNotExist
      val http4sClient = fakeService.client(fakeService.deleteRateLimit(failure.json, zoneId, rateLimitId, failure.status))
      val client = buildRateLimitClient(http4sClient, authorization)

      val output = client.delete(zoneId, rateLimitId)
        .compile
        .toList
        .attempt
        .unsafeRunSync()

      output must beLeft[Throwable].like {
        case ex: RateLimitDoesNotExistException ⇒ ex.getMessage must_==
          s"The rate limit $rateLimitId not found for zone $zoneId."
      }
    }
  }

  def buildRateLimitClient[F[_]: Sync](http4sClient: Client[F], authorization: CloudflareAuthorization): RateLimitClient[F] = {
    val fakeHttp4sExecutor = new StreamingCloudflareApiExecutor(http4sClient, authorization)
    RateLimitClient(fakeHttp4sExecutor)
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

      val listRateLimitsPage1 =
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
          |    }
          |  ],
          |  "result_info": {
          |    "page": 1,
          |    "per_page": 1,
          |    "total_pages": 3,
          |    "count": 1,
          |    "total_count": 3
          |  },
          |  "success": true,
          |  "errors": [],
          |  "messages": []
          |}
        """.stripMargin

      val listRateLimitsPage2 =
        """{
          |  "result": [
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
          |    "page": 2,
          |    "per_page": 1,
          |    "total_pages": 3,
          |    "count": 1,
          |    "total_count": 3
          |  },
          |  "success": true,
          |  "errors": [],
          |  "messages": []
          |}
        """.stripMargin

      val listRateLimitsPage3 =
        """{
          |  "result": [
          |    {
          |      "id": "fake-rate-limit-3",
          |      "description": "Third Rate Limit",
          |      "match":
          |      {
          |        "request":
          |        {
          |          "methods": ["PUT"],
          |          "schemes": ["_ALL_"],
          |          "url": "*.thirdtest.com/*"
          |        }
          |      },
          |      "threshold": 20,
          |      "period": 6000,
          |      "action":
          |      {
          |        "mode": "ban",
          |        "timeout": 120
          |      }
          |    }
          |  ],
          |  "result_info": {
          |    "page": 3,
          |    "per_page": 1,
          |    "total_pages": 3,
          |    "count": 1,
          |    "total_count": 3
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
          |  "result": null,
          |  "success": true,
          |  "errors": null,
          |  "messages": null
          |}
        """.stripMargin
    }

    object Failures {
      case class Failure(status: Status, json: String)

      val rateLimitDoesNotExist = Failure(Status.NotFound,
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

      val rateLimitCreationError = Failure(Status.BadRequest,
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

      val rateLimitUpdateError = Failure(Status.BadRequest,
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

      val rateLimitDeleteError = Failure(Status.BadRequest,
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
