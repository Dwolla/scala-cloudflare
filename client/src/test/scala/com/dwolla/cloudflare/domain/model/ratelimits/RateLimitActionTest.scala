package com.dwolla.cloudflare.domain.model.ratelimits

import java.time.Duration

import org.specs2.mutable.Specification
import io.circe.literal._
import io.circe.syntax._
import org.scalacheck._
import org.specs2.ScalaCheck

class RateLimitActionTest extends Specification with ScalaCheck with ScalacheckShapeless {

  "RateLimit" should {
    "encode" >> {
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
            ))
          ))),
        bypass = List(RateLimitBypass(name = "url", value = "http://l@:1")),
        threshold = 0,
        period = Duration.ofSeconds(0),
        action = Challenge
      ).asJson

      output must_==
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
               }"""
    }

    "encode without ID" >> {
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
            ))
          ))),
        bypass = List(RateLimitBypass(name = "url", value = "http://l@:1")),
        threshold = 0,
        period = Duration.ofSeconds(0),
        action = Challenge
      ).asJson

      output must_==
        json"""{
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
               }"""
    }

    "decode" >> {
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

      output must beRight(RateLimit(
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
            ))
          ))),
        bypass = List(RateLimitBypass(name = "url", value = "http://l@:1")),
        threshold = 0,
        period = Duration.ofSeconds(0),
        action = Challenge
      ))
    }

    "decode with correlation and status" >> {
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

      output must beRight(RateLimit(
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
    }
  }

  "RateLimitAction" should {
    "decode simulate" >> {
      val output =
        json"""{
                 "mode": "simulate",
                 "timeout": 60,
                 "response": {
                   "content_type": "text/xml",
                   "body": "<xml></xml>"
                 }
               }""".as[RateLimitAction]

      output must beRight(
        Simulate(
          Duration.ofSeconds(60),
          Option(RateLimitActionResponse(ContentType.Xml, "<xml></xml>")
          )))
    }

    "encode Simulate" >> {
      val output = (Simulate(
        Duration.ofSeconds(60),
        Option(RateLimitActionResponse(ContentType.Xml, "<xml></xml>")
        )): RateLimitAction).asJson

      output must_== json"""{
                 "mode": "simulate",
                 "timeout": 60,
                 "response": {
                   "content_type": "text/xml",
                   "body": "<xml></xml>"
                 }
               }"""
    }

    "decode ban" >> {
      val output =
        json"""{
                 "mode": "ban",
                 "timeout": 60,
                 "response": {
                   "content_type": "text/xml",
                   "body": "<xml></xml>"
                 }
               }""".as[RateLimitAction]

      output must beRight(
        Ban(
          Duration.ofSeconds(60),
          Option(RateLimitActionResponse(ContentType.Xml, "<xml></xml>")
          )))
    }

    "encode Ban" >> {
      val output = (Ban(
        Duration.ofSeconds(60),
        Option(RateLimitActionResponse(ContentType.Xml, "<xml></xml>")
        )): RateLimitAction).asJson

      output must_== json"""{
                 "mode": "ban",
                 "timeout": 60,
                 "response": {
                   "content_type": "text/xml",
                   "body": "<xml></xml>"
                 }
               }"""
    }

    "decode challenge" >> {
      val output =
        json"""{
                 "mode": "challenge"
               }""".as[RateLimitAction]

      output must beRight(Challenge)
    }

    "encode Challenge" >> {
      val output = (Challenge: RateLimitAction).asJson

      output must_== json"""{"mode": "challenge"}"""
    }

    "decode js_challenge" >> {
      val output =
        json"""{
                 "mode": "js_challenge"
               }""".as[RateLimitAction]

      output must beRight(JsChallenge)
    }

    "encode JsChallenge" >> {
      val output = (JsChallenge: RateLimitAction).asJson

      output must_== json"""{"mode": "js_challenge"}"""
    }

    "decode null list field" >> {
      val output =
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

      output must beRight(RateLimit(
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
            ))
          ))),
        bypass = List(RateLimitBypass(name = "url", value = "http://l@:1")),
        threshold = 0,
        period = Duration.ofSeconds(0),
        action = Challenge
      ))
    }

    "decode non-existent list field" >> {
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

      output must beRight(RateLimit(
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
            ))
          ))),
        bypass = List(RateLimitBypass(name = "url", value = "http://l@:1")),
        threshold = 0,
        period = Duration.ofSeconds(0),
        action = Challenge
      ))
    }

    "decode booleans sent as strings" >> {
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

      output must beRight(RateLimit(
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
            ))
          ))),
        bypass = List(RateLimitBypass(name = "url", value = "http://l@:1")),
        threshold = 0,
        period = Duration.ofSeconds(0),
        action = Challenge
      ))
    }
  }
}
