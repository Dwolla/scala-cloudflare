package com.dwolla.cloudflare

import cats.effect.*
import com.dwolla.cloudflare.domain.model.*
import com.dwolla.cloudflare.domain.model.Exceptions.UnexpectedCloudflareErrorException
import com.dwolla.cloudflare.domain.model.ratelimits.*
import dwolla.cloudflare.FakeCloudflareService
import munit.{CatsEffectSuite, ScalaCheckSuite}
import natchez.Trace.Implicits.noop
import org.http4s.HttpRoutes
import org.scalacheck.{Arbitrary, Gen}

import java.time.Duration

class RateLimitClientTest extends CatsEffectSuite with ScalaCheckSuite {

  // Common setup values and helper
  val zoneId: ZoneId = tagZoneId("zone-id")
  val rateLimitId: RateLimitId = tagRateLimitId("ec794f8d14e2407084de98f4a39e6387")

  val authorization = CloudflareAuthorization("email", "key")

  private def buildRateLimitClient(service: HttpRoutes[IO]): RateLimitClient[fs2.Stream[IO, *]] = {
    val fakeService = new FakeCloudflareService(authorization)
    RateLimitClient[IO](new StreamingCloudflareApiExecutor(fakeService.client(service), authorization))
  }

  test("list should list the rate limits for the given zone") {
    val fakeService = new FakeCloudflareService(authorization)
    val client = buildRateLimitClient(fakeService.listRateLimits(zoneId))
    val output = client.list(zoneId)

    val expectedItem = RateLimit(
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
    )

    assertIO(output.compile.toList.map(_.contains(expectedItem)), true)
  }

  test("get by id should return the rate limit with the given id") {
    val fakeService = new FakeCloudflareService(authorization)
    val client = buildRateLimitClient(fakeService.getRateLimitById(zoneId, rateLimitId))
    val output = client.getById(zoneId, rateLimitId.value)

    val expected = List(RateLimit(
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
    ))

    assertIO(output.compile.toList, expected)
  }

  private val createInput = RateLimit(
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

  test("create should send the json object and return its value") {
    val fakeService = new FakeCloudflareService(authorization)
    val client = buildRateLimitClient(fakeService.createRateLimit(zoneId, rateLimitId))
    val output = client.create(zoneId, createInput)

    val expected = List(createInput.copy(
      id = Option(rateLimitId)
    ))

    assertIO(output.compile.toList, expected)
  }

  test("create should raise a reasonable error if Cloudflare's business rules are violated") {
    val fakeService = new FakeCloudflareService(authorization)
    val client = buildRateLimitClient(fakeService.createRateLimitFails)
    val output = client.create(zoneId, createInput)

    val expected = List(
      Left(
        UnexpectedCloudflareErrorException(
          List(Error(None, "ratelimit.api.validation_error:ratelimit.api.mitigation_timeout_must_be_greater_than_period")),
          List()
        )
      )
    )

    assertIO(output.attempt.compile.toList, expected)
  }

  test("update should update the given rate limit") {
    val input = RateLimit(
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

    val fakeService = new FakeCloudflareService(authorization)
    val client = buildRateLimitClient(fakeService.updateRateLimit(zoneId, rateLimitId))
    val output = client.update(zoneId, input)

    val expected = List(input)

    assertIO(output.compile.toList, expected)
  }

  test("update should raise an exception when trying to update an unidentified rate limit") {
    val input = RateLimit(
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

    val fakeService = new FakeCloudflareService(authorization)
    val client = buildRateLimitClient(fakeService.updateRateLimit(zoneId, rateLimitId))
    val output = client.update(zoneId, input)

    val expected = List(Left(CannotUpdateUnidentifiedRateLimit(input)))

    assertIO(output.attempt.compile.toList, expected)
  }

  test("delete should delete the given rate limit") {
    val fakeService = new FakeCloudflareService(authorization)
    val client = buildRateLimitClient(fakeService.deleteRateLimit(zoneId, rateLimitId))
    val output = client.delete(zoneId, rateLimitId.value)

    val expected = List(rateLimitId)
    assertIO(output.compile.toList, expected)
  }

  test("delete should return success if the rate limit id doesn't exist") {
    val fakeService = new FakeCloudflareService(authorization)
    val client = buildRateLimitClient(fakeService.deleteRateLimitThatDoesNotExist(zoneId, validId = true))
    val output = client.delete(zoneId, rateLimitId.value)

    val expected = List(rateLimitId)
    assertIO(output.compile.toList, expected)
  }

  test("delete should return success if the rate limit id is invalid") {
    val fakeService = new FakeCloudflareService(authorization)
    val client = buildRateLimitClient(fakeService.deleteRateLimitThatDoesNotExist(zoneId, validId = false))
    val output = client.delete(zoneId, rateLimitId.value)

    val expected = List(rateLimitId)
    assertIO(output.compile.toList, expected)
  }

  // property-based: buildUri and parseUri are inverses
  private val nonEmptyAlphaNumericString = Gen.identifier
  implicit private val arbitraryZoneId: Arbitrary[ZoneId] = Arbitrary(nonEmptyAlphaNumericString.map(ZoneId(_)))
  implicit private val arbitraryRateLimitId: Arbitrary[RateLimitId] = Arbitrary(nonEmptyAlphaNumericString.map(RateLimitId(_)))

  property("buildUri and parseUri should be inverses") {
    import org.scalacheck.Prop.forAll

    forAll { (zoneId: ZoneId, rateLimitId: RateLimitId) =>
      val client = new RateLimitClient[IO] {
        override def list(zoneId: ZoneId): IO[RateLimit] = ???
        override def getById(zoneId: ZoneId, rateLimitId: String): IO[RateLimit] = ???
        override def create(zoneId: ZoneId, rateLimit: RateLimit): IO[RateLimit] = ???
        override def update(zoneId: ZoneId, rateLimit: RateLimit): IO[RateLimit] = ???
        override def delete(zoneId: ZoneId, rateLimitId: String): IO[RateLimitId] = ???
        override def getByUri(uri: String): IO[RateLimit] = ???
      }

      assertEquals(client.parseUri(client.buildUri(zoneId, rateLimitId).renderString), Some((zoneId, rateLimitId)))
    }
  }
}
