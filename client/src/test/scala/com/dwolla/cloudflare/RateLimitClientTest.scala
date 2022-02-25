package com.dwolla.cloudflare

import java.time.Duration

import cats.effect._
import com.dwolla.cloudflare.domain.model.Exceptions.UnexpectedCloudflareErrorException
import com.dwolla.cloudflare.domain.model._
import com.dwolla.cloudflare.domain.model.ratelimits._
import dwolla.cloudflare.FakeCloudflareService
import org.http4s.HttpRoutes
import org.scalacheck.{Arbitrary, Gen}
import org.specs2.ScalaCheck
import org.specs2.matcher.{ContainWithResult, IOMatchers, Matchers}
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import shapeless.tag.@@

class RateLimitClientTest extends Specification with ScalaCheck with IOMatchers with Matchers {

  trait Setup extends Scope {
    val zoneId: ZoneId = tagZoneId("zone-id")
    val rateLimitId: RateLimitId = tagRateLimitId("ec794f8d14e2407084de98f4a39e6387")

    val authorization = CloudflareAuthorization("email", "key")
    val fakeService = new FakeCloudflareService(authorization)

    protected def buildRateLimitClient(service: HttpRoutes[IO]): RateLimitClient[IO] =
      RateLimitClient(new StreamingCloudflareApiExecutor(fakeService.client(service), authorization))
  }

  "list" should {

    "list the rate limits for the given zone" in new Setup {
      private val client = buildRateLimitClient(fakeService.listRateLimits(zoneId))
      private val output = client.list(zoneId)

      output.compile.toList must returnValue(ContainWithResult(RateLimit(
        id = Option("ec794f8d14e2407084de98f4a39e6387").map(tagRateLimitId),
        disabled = Option(true),
        description = Option("hydragents.xyz/sign-up"),
        `match` = RateLimitMatch(
          RateLimitMatchRequest(
            methods = List(Method.Post),
            schemes = List(Scheme.All),
            url = "hydragents.xyz/sign-up"
          ),
          Option(RateLimitMatchResponse(
            origin_traffic = Option(true),
            headers = List(RateLimitMatchResponseHeader(
              name = "Cf-Cache-Status",
              op = Op.NotEqual,
              value = "HIT"
            ))
          ))
        ),
        threshold = 5,
        period = Duration.ofSeconds(60),
        action = Challenge
      )))
    }

    "get by id" should {

      "return the rate limit with the given id" in new Setup {
        private val client = buildRateLimitClient(fakeService.getRateLimitById(zoneId, rateLimitId))
        private val output = client.getById(zoneId, rateLimitId: String)

        output.compile.toList must returnValue(List(RateLimit(
          id = Option("ec794f8d14e2407084de98f4a39e6387").map(tagRateLimitId),
          disabled = Option(true),
          description = Option("hydragents.xyz/sign-up"),
          `match` = RateLimitMatch(
            RateLimitMatchRequest(
              methods = List(Method.Post),
              schemes = List(Scheme.All),
              url = "hydragents.xyz/sign-up"
            ),
            Option(RateLimitMatchResponse(
              origin_traffic = Option(true),
              headers = List(RateLimitMatchResponseHeader(
                name = "Cf-Cache-Status",
                op = Op.NotEqual,
                value = "HIT"
              ))
            ))
          ),
          threshold = 5,
          period = Duration.ofSeconds(60),
          action = Challenge
        )))
      }

    }

  }

  "create" should {
    val input = RateLimit(
      disabled = Option(false),
      description = Option("hydragents.xyz/sign-up"),
      `match` = RateLimitMatch(
        request = RateLimitMatchRequest(
          methods = List(Method.Post),
          schemes = List(Scheme.All),
          url = "hydragents.xyz/sign-up"
        )
      ),
      threshold = 60,
      period = Duration.ofSeconds(900),
      action = Challenge
    )

    "send the json object and return its value" in new Setup {
      private val client = buildRateLimitClient(fakeService.createRateLimit(zoneId, rateLimitId))
      private val output = client.create(zoneId, input)

      output.compile.toList must returnValue(List(input.copy(
        id = Option(rateLimitId)),
      ))
    }

    "raise a reasonable error if Cloudflare's business rules are violated" in new Setup {
      private val client = buildRateLimitClient(fakeService.createRateLimitFails)
      private val output = client.create(zoneId, input)

      output.attempt.compile.toList must returnValue(List(
        Left(
          UnexpectedCloudflareErrorException(
            List(Error(None, "ratelimit.api.validation_error:ratelimit.api.mitigation_timeout_must_be_greater_than_period")),
            List()
          )
        )
      ))
    }
  }

  "update" should {
    "update the given rate limit" in new Setup {
      private val input = RateLimit(
        id = Option(rateLimitId),
        disabled = Option(false),
        description = Option("hydragents.xyz/sign-up"),
        `match` = RateLimitMatch(
          request = RateLimitMatchRequest(
            methods = List(Method.Post),
            schemes = List(Scheme.All),
            url = "hydragents.xyz/sign-up"
          )
        ),
        threshold = 60,
        period = Duration.ofSeconds(900),
        action = Challenge
      )

      private val client = buildRateLimitClient(fakeService.updateRateLimit(zoneId, rateLimitId))
      private val output = client.update(zoneId, input)

      output.compile.toList must returnValue(List(input))
    }

    "raise an exception when trying to update an unidentified rate limit" in new Setup {
      private val input = RateLimit(
        id = None,
        `match` = RateLimitMatch(
          request = RateLimitMatchRequest(
            url = "hydragents.xyz/sign-up"
          )
        ),
        threshold = 60,
        period = Duration.ofSeconds(900),
        action = Challenge
      )

      private val client = buildRateLimitClient(fakeService.updateRateLimit(zoneId, rateLimitId))
      private val output = client.update(zoneId, input)

      output.attempt.compile.toList must returnValue(List(
        Left(CannotUpdateUnidentifiedRateLimit(input))
      ))
    }
  }

  "delete" should {
    "delete the given rate limit" in new Setup {
      private val client = buildRateLimitClient(fakeService.deleteRateLimit(zoneId, rateLimitId))
      private val output = client.delete(zoneId, rateLimitId)

      output.compile.toList must returnValue(List(rateLimitId))
    }

    "return success if the rate limit id doesn't exist" in new Setup {
      private val client = buildRateLimitClient(fakeService.deleteRateLimitThatDoesNotExist(zoneId, validId = true))
      private val output = client.delete(zoneId, rateLimitId)

      output.compile.toList must returnValue(List(rateLimitId))
    }

    "return success if the rate limit id is invalid" in new Setup {
      private val client = buildRateLimitClient(fakeService.deleteRateLimitThatDoesNotExist(zoneId, validId = false))
      private val output = client.delete(zoneId, rateLimitId)

      output.compile.toList must returnValue(List(rateLimitId))
    }
  }

  "buildUri and parseUri" should {
    val nonEmptyAlphaNumericString = Gen.identifier
    implicit val arbitraryZoneId: Arbitrary[String @@ ZoneIdTag] = Arbitrary(nonEmptyAlphaNumericString.map(shapeless.tag[ZoneIdTag][String]))
    implicit val arbitraryRateLimitId: Arbitrary[String @@ RateLimitIdTag] = Arbitrary(nonEmptyAlphaNumericString.map(shapeless.tag[RateLimitIdTag][String]))

    "be the inverse of each other" >> {
      prop { (zoneId: ZoneId, rateLimitId: RateLimitId) =>
        val client = new RateLimitClient[IO] {
          override def list(zoneId: ZoneId): fs2.Stream[IO, RateLimit] = ???
          override def getById(zoneId: ZoneId, rateLimitId: String): fs2.Stream[IO, RateLimit] = ???
          override def create(zoneId: ZoneId, rateLimitId: RateLimit): fs2.Stream[IO, RateLimit] = ???
          override def update(zoneId: ZoneId, rateLimitId: RateLimit): fs2.Stream[IO, RateLimit] = ???
          override def delete(zoneId: ZoneId, rateLimitId: String): fs2.Stream[IO, RateLimitId] = ???
        }

        client.parseUri(client.buildUri(zoneId, rateLimitId).renderString) must beSome((zoneId, rateLimitId))
      }
    }
  }
}
