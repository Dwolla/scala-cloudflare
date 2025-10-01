package com.dwolla.cloudflare.domain.model.ratelimits

import io.circe.DecodingFailure
import io.circe.literal.*
import io.circe.syntax.*
import munit.FunSuite

import java.time.Duration

class RateLimitActionTest extends FunSuite {

  // RateLimit encode/decode tests
  test("RateLimit encode") {
    val output = RateLimit(
      id = Option("rate-limit-id").map(tagRateLimitId),
      disabled = Option(false),
      description = Option("my description"),
      `match` = RateLimitMatch(
        RateLimitMatchRequest(
          methods = List(Method.Get),
          schemes = List(Scheme.Http),
          url = "http://l@:1"
        ),
        Option(RateLimitMatchResponse(
          origin_traffic = Option(true),
          headers = List(RateLimitMatchResponseHeader(
            name = "Cf-Cache-Status",
            op = Op.NotEqual,
            value = "HIT"
          ))))),
      bypass = List(RateLimitBypass(name = "url", value = "http://l@:1")),
      threshold = 0,
      period = Duration.ofSeconds(0),
      action = Challenge
    ).asJson

    val expected = json"""{
                 "id": "rate-limit-id",
                 "disabled": false,
                 "description": "my description",
                 "match": {
                   "request": {
                     "methods": ["GET"],
                     "schemes": ["HTTP"],
                     "url": "http://l@:1"
                   },
                   "response": {
                     "origin_traffic": true,
                     "headers": [
                       {
                         "name": "Cf-Cache-Status",
                         "op": "ne",
                         "value": "HIT"
                       }
                     ],
                     "status": []
                   }
                 },
                 "correlate": null,
                 "bypass": [
                   {
                     "name": "url",
                     "value": "http://l@:1"
                   }
                 ],
                 "threshold": 0,
                 "period": 0,
                 "action": {
                   "mode": "challenge"
                 }
               }"""

    assertEquals(output, expected)
  }

  test("RateLimit encode without ID") {
    val output = RateLimit(
      id = None,
      disabled = Option(false),
      description = Option("my description"),
      `match` = RateLimitMatch(
        RateLimitMatchRequest(
          methods = List(Method.Get),
          schemes = List(Scheme.Http),
          url = "http://l@:1"
        ),
        Option(RateLimitMatchResponse(
          origin_traffic = Option(true),
          headers = List(RateLimitMatchResponseHeader(
            name = "Cf-Cache-Status",
            op = Op.NotEqual,
            value = "HIT"
          ))))),
      bypass = List(RateLimitBypass(name = "url", value = "http://l@:1")),
      threshold = 0,
      period = Duration.ofSeconds(0),
      action = Challenge
    ).asJson

    val expected = json"""{
                 "id": null,
                 "disabled": false,
                 "description": "my description",
                 "match": {
                   "request": {
                     "methods": ["GET"],
                     "schemes": ["HTTP"],
                     "url": "http://l@:1"
                   },
                   "response": {
                     "origin_traffic": true,
                     "headers": [
                       {
                         "name": "Cf-Cache-Status",
                         "op": "ne",
                         "value": "HIT"
                       }
                     ],
                     "status": []
                   }
                 },
                 "bypass": [
                   {
                     "name": "url",
                     "value": "http://l@:1"
                   }
                 ],
                 "correlate": null,
                 "threshold": 0,
                 "period": 0,
                 "action": {
                   "mode": "challenge"
                 }
               }"""

    assertEquals(output, expected)
  }

  test("RateLimit decode") {
    val output =
      json"""{
                 "id": "rate-limit-id",
                 "disabled": false,
                 "description": "my description",
                 "match": {
                   "request": {
                     "methods": ["GET"],
                     "schemes": ["HTTP"],
                     "url": "http://l@:1"
                   },
                   "response": {
                     "origin_traffic": true,
                     "headers": [
                       {
                         "name": "Cf-Cache-Status",
                         "op": "ne",
                         "value": "HIT"
                       }
                     ]
                   }
                 },
                 "bypass": [
                   {
                     "name": "url",
                     "value": "http://l@:1"
                   }
                 ],
                 "threshold": 0,
                 "period": 0,
                 "action": {
                   "mode": "challenge"
                 }
               }""".as[RateLimit]

    val expected = Right(RateLimit(
      id = Option("rate-limit-id").map(tagRateLimitId),
      disabled = Option(false),
      description = Option("my description"),
      `match` = RateLimitMatch(
        RateLimitMatchRequest(
          methods = List(Method.Get),
          schemes = List(Scheme.Http),
          url = "http://l@:1"
        ),
        Option(RateLimitMatchResponse(
          origin_traffic = Option(true),
          headers = List(RateLimitMatchResponseHeader(
            name = "Cf-Cache-Status",
            op = Op.NotEqual,
            value = "HIT"
          ))))),
      bypass = List(RateLimitBypass(name = "url", value = "http://l@:1")),
      threshold = 0,
      period = Duration.ofSeconds(0),
      action = Challenge
    ))

    assertEquals(output, expected)
  }

  test("RateLimit decode with correlation and status") {
    val output =
      json"""{
                 "id": "rate-limit-id",
                 "disabled": false,
                 "description": "my description",
                 "match": {
                   "request": {
                     "methods": ["GET"],
                     "schemes": ["HTTP"],
                     "url": "http://l@:1"
                   },
                   "response": {
                     "origin_traffic": true,
                     "headers": [
                       {
                         "name": "Cf-Cache-Status",
                         "op": "ne",
                         "value": "HIT"
                       }
                     ],
                     "status": [ 403 ]
                   }
                 },
                 "bypass": [
                   {
                     "name": "url",
                     "value": "http://l@:1"
                   }
                 ],
                 "correlate": {
                   "by": "nat"
                 },
                 "threshold": 0,
                 "period": 0,
                 "action": {
                   "mode": "challenge"
                 }
               }""".as[RateLimit]

    val expected = Right(RateLimit(
      id = Option("rate-limit-id").map(tagRateLimitId),
      disabled = Option(false),
      description = Option("my description"),
      `match` = RateLimitMatch(
        RateLimitMatchRequest(
          methods = List(Method.Get),
          schemes = List(Scheme.Http),
          url = "http://l@:1"
        ),
        Option(RateLimitMatchResponse(
          origin_traffic = Option(true),
          headers = List(RateLimitMatchResponseHeader(
            name = "Cf-Cache-Status",
            op = Op.NotEqual,
            value = "HIT"
          )),
          status = List(403),
        ))),
      bypass = List(RateLimitBypass(name = "url", value = "http://l@:1")),
      correlate = Option(RateLimitCorrelation("nat")),
      threshold = 0,
      period = Duration.ofSeconds(0),
      action = Challenge
    ))

    assertEquals(output, expected)
  }

  // RateLimitAction encode/decode tests
  test("RateLimitAction decode simulate") {
    val output =
      json"""{
                 "mode": "simulate",
                 "timeout": 60,
                 "response": {
                   "content_type": "text/xml",
                   "body": "<xml></xml>"
                 }
               }""".as[RateLimitAction]

    val expected: Either[DecodingFailure, RateLimitAction] = Right(
      Simulate(
        Duration.ofSeconds(60),
        Option(RateLimitActionResponse(ContentType.Xml, "<xml></xml>")
        )))

    assertEquals(output, expected)
  }

  test("RateLimitAction encode Simulate") {
    val output = (Simulate(
      Duration.ofSeconds(60),
      Option(RateLimitActionResponse(ContentType.Xml, "<xml></xml>")
      )): RateLimitAction).asJson

    val expected = json"""{
                 "mode": "simulate",
                 "timeout": 60,
                 "response": {
                   "content_type": "text/xml",
                   "body": "<xml></xml>"
                 }
               }"""

    assertEquals(output, expected)
  }

  test("RateLimitAction decode ban") {
    val output =
      json"""{
                 "mode": "ban",
                 "timeout": 60,
                 "response": {
                   "content_type": "text/xml",
                   "body": "<xml></xml>"
                 }
               }""".as[RateLimitAction]

    val expected: Either[DecodingFailure, RateLimitAction] = Right(
      Ban(
        Duration.ofSeconds(60),
        Option(RateLimitActionResponse(ContentType.Xml, "<xml></xml>")
        )))

    assertEquals(output, expected)
  }

  test("RateLimitAction encode Ban") {
    val output = (Ban(
      Duration.ofSeconds(60),
      Option(RateLimitActionResponse(ContentType.Xml, "<xml></xml>")
      )): RateLimitAction).asJson

    val expected = json"""{
                 "mode": "ban",
                 "timeout": 60,
                 "response": {
                   "content_type": "text/xml",
                   "body": "<xml></xml>"
                 }
               }"""

    assertEquals(output, expected)
  }

  test("RateLimitAction decode challenge") {
    val output: Either[DecodingFailure, RateLimitAction] =
      json"""{
                 "mode": "challenge"
               }""".as[RateLimitAction]

    val expected: Either[DecodingFailure, RateLimitAction] = Right(Challenge: RateLimitAction)

    assertEquals(output, expected)
  }

  test("RateLimitAction encode Challenge") {
    val output = (Challenge: RateLimitAction).asJson
    val expected = json"""{"mode": "challenge"}"""
    assertEquals(output, expected)
  }

  test("RateLimitAction decode js_challenge") {
    val output: Either[DecodingFailure, RateLimitAction] =
      json"""{
                 "mode": "js_challenge"
               }""".as[RateLimitAction]

    val expected: Either[DecodingFailure, RateLimitAction] = Right(JsChallenge: RateLimitAction)

    assertEquals(output, expected)
  }

  test("RateLimitAction encode JsChallenge") {
    val output = (JsChallenge: RateLimitAction).asJson
    val expected = json"""{"mode": "js_challenge"}"""
    assertEquals(output, expected)
  }

  test("RateLimit decode null list field") {
    val output: Either[DecodingFailure, RateLimit] =
      json"""{
                 "id": "rate-limit-id",
                 "disabled": false,
                 "description": "my description",
                 "match": {
                   "request": {
                     "methods": null,
                     "schemes": ["HTTP"],
                     "url": "http://l@:1"
                   },
                   "response": {
                     "origin_traffic": true,
                     "headers": [
                       {
                         "name": "Cf-Cache-Status",
                         "op": "ne",
                         "value": "HIT"
                       }
                     ]
                   }
                 },
                 "bypass": [
                   {
                     "name": "url",
                     "value": "http://l@:1"
                   }
                 ],
                 "threshold": 0,
                 "period": 0,
                 "action": {
                   "mode": "challenge"
                 }
               }""".as[RateLimit]

    val expected = Right(RateLimit(
      id = Option("rate-limit-id").map(tagRateLimitId),
      disabled = Option(false),
      description = Option("my description"),
      `match` = RateLimitMatch(
        RateLimitMatchRequest(
          methods = List.empty,
          schemes = List(Scheme.Http),
          url = "http://l@:1"
        ),
        Option(RateLimitMatchResponse(
          origin_traffic = Option(true),
          headers = List(RateLimitMatchResponseHeader(
            name = "Cf-Cache-Status",
            op = Op.NotEqual,
            value = "HIT"
          ))))),
      bypass = List(RateLimitBypass(name = "url", value = "http://l@:1")),
      threshold = 0,
      period = Duration.ofSeconds(0),
      action = Challenge
    ))

    assertEquals(output, expected)
  }

  test("RateLimit decode non-existent list field") {
    val output =
      json"""{
                 "id": "rate-limit-id",
                 "disabled": false,
                 "description": "my description",
                 "match": {
                   "request": {
                     "schemes": ["HTTP"],
                     "url": "http://l@:1"
                   },
                   "response": {
                     "origin_traffic": true,
                     "headers": [
                       {
                         "name": "Cf-Cache-Status",
                         "op": "ne",
                         "value": "HIT"
                       }
                     ]
                   }
                 },
                 "bypass": [
                   {
                     "name": "url",
                     "value": "http://l@:1"
                   }
                 ],
                 "threshold": 0,
                 "period": 0,
                 "action": {
                   "mode": "challenge"
                 }
               }""".as[RateLimit]

    val expected = Right(RateLimit(
      id = Option("rate-limit-id").map(tagRateLimitId),
      disabled = Option(false),
      description = Option("my description"),
      `match` = RateLimitMatch(
        RateLimitMatchRequest(
          methods = List.empty,
          schemes = List(Scheme.Http),
          url = "http://l@:1"
        ),
        Option(RateLimitMatchResponse(
          origin_traffic = Option(true),
          headers = List(RateLimitMatchResponseHeader(
            name = "Cf-Cache-Status",
            op = Op.NotEqual,
            value = "HIT"
          ))))),
      bypass = List(RateLimitBypass(name = "url", value = "http://l@:1")),
      threshold = 0,
      period = Duration.ofSeconds(0),
      action = Challenge
    ))

    assertEquals(output, expected)
  }

  test("RateLimit decode booleans sent as strings") {
    val output =
      json"""{
                 "id": "rate-limit-id",
                 "disabled": "false",
                 "description": "my description",
                 "match": {
                   "request": {
                     "methods": null,
                     "schemes": ["HTTP"],
                     "url": "http://l@:1"
                   },
                   "response": {
                     "origin_traffic": "true",
                    "headers": [
                      {
                         "name": "Cf-Cache-Status",
                         "op": "ne",
                         "value": "HIT"
                       }
                     ]
                   }
                 },
                 "bypass": [
                   {
                     "name": "url",
                     "value": "http://l@:1"
                   }
                 ],
                 "threshold": 0,
                 "period": 0,
                 "action": {
                   "mode": "challenge"
                 }
               }""".as[RateLimit]

    val expected = Right(RateLimit(
      id = Option("rate-limit-id").map(tagRateLimitId),
      disabled = Option(false),
      description = Option("my description"),
      `match` = RateLimitMatch(
        RateLimitMatchRequest(
          methods = List.empty,
          schemes = List(Scheme.Http),
          url = "http://l@:1"
        ),
        Option(RateLimitMatchResponse(
          origin_traffic = Option(true),
          headers = List(RateLimitMatchResponseHeader(
            name = "Cf-Cache-Status",
            op = Op.NotEqual,
            value = "HIT"
          ))))),
      bypass = List(RateLimitBypass(name = "url", value = "http://l@:1")),
      threshold = 0,
      period = Duration.ofSeconds(0),
      action = Challenge
    ))

    assertEquals(output, expected)
  }
}
