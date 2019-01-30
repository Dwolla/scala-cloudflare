package com.dwolla.cloudflare.domain.model.pagerules

import com.dwolla.cloudflare.domain.model.pagerules.CacheLevelValue.Standard
import io.circe.generic.extras.Configuration
import org.specs2.mutable.Specification
import io.circe.literal._
import io.circe.syntax._
import org.scalacheck._
import org.specs2.ScalaCheck
import io.circe.CursorOp.DownField
import io.circe.DecodingFailure
import org.http4s.Uri
import org.http4s.testing.ArbitraryInstances
import org.http4s.circe._

class PageRuleActionTest extends Specification with ScalaCheck with ScalacheckShapeless with ArbitraryInstances {

  def transformName[T](t: T): String = Configuration.snakeCaseTransformation(String.valueOf(t))

  "PageRule" should {
    "encode" >> {
      val output = PageRule(
        id = Option("page-rule-id").map(tagPageRuleId),
        targets = List.empty,
        actions = List(
          ForwardingUrl(Uri.uri("http://l@:1"), PermanentRedirect),
          DisableSecurity,
          CacheLevel(Standard),
        ),
        priority = 0,
        status = PageRuleStatus.Active,
        modified_on = None,
        created_on = None
      ).asJson

      output must_==
        json"""{
                 "id": "page-rule-id",
                 "targets": [],
                 "actions": [
                   {"id": "forwarding_url", "value": {"url": "http://l@:1", "status_code": 301}},
                   {"id": "disable_security"},
                   {"id": "cache_level", "value": "aggressive"}
                 ],
                 "priority": 0,
                 "status": "active",
                 "modified_on": null,
                 "created_on": null
               }"""
    }

    "encode without ID" >> {
      val output = PageRule(
        id = None,
        targets = List.empty,
        actions = List(
          ForwardingUrl(Uri.uri("http://l@:1"), PermanentRedirect),
          DisableSecurity,
          CacheLevel(Standard),
        ),
        priority = 0,
        status = PageRuleStatus.Active,
        modified_on = None,
        created_on = None
      ).asJson

      output must_==
        json"""{
                 "id": null,
                 "targets": [],
                 "actions": [
                   {"id": "forwarding_url", "value": {"url": "http://l@:1", "status_code": 301}},
                   {"id": "disable_security"},
                   {"id": "cache_level", "value": "aggressive"}
                 ],
                 "priority": 0,
                 "status": "active",
                 "modified_on": null,
                 "created_on": null
               }"""
    }

    "decode" >> {
      val output =
        json"""{
                 "id": "page-rule-id",
                 "targets": [],
                 "actions": [
                   {"id": "always_use_https"},
                   {"id": "cache_level", "value": "aggressive"}
                 ],
                 "priority": 0,
                 "status": "active",
                 "modified_on": null,
                 "created_on": null
               }""".as[PageRule]

      output must beRight(PageRule(
        id = Option("page-rule-id").map(tagPageRuleId),
        targets = List.empty,
        actions = List(
          AlwaysUseHttps,
          CacheLevel(Standard),
        ),
        priority = 0,
        status = PageRuleStatus.Active,
        modified_on = None,
        created_on = None
      ))
    }
  }

  "PageRuleAction" should {
    "decode disable_security" >> {
      val output =
        json"""{
                 "id": "disable_security"
               }""".as[PageRuleAction]

      output must beRight(DisableSecurity)
    }

    "encode DisableSecurity" >> {
      val output = (DisableSecurity: PageRuleAction).asJson

      output must_== json"""{"id": "disable_security"}"""
    }

    "decode browser_check" >> { prop { enabled: PageRuleActionEnabled =>
      val output =
        json"""{
                 "id": "browser_check",
                 "value": ${transformName(enabled)}
               }""".as[PageRuleAction]

      output must beRight(BrowserCheck(enabled))
    }}

    "encode BrowserCheck" >> { prop { enabled: PageRuleActionEnabled =>
      val output = (BrowserCheck(enabled): PageRuleAction).asJson

      output must_== json"""{"id": "browser_check", "value": ${transformName(enabled)}}"""
    }}

    "decode ssl" >> { prop { ssl: SslSetting =>
      val output =
        json"""{
                 "id": "ssl",
                 "value": ${transformName(ssl)}
               }""".as[PageRuleAction]

      output must beRight(Ssl(ssl))
    }}

    "encode ssl" >> { prop  { ssl: SslSetting =>
      val output = (Ssl(ssl): PageRuleAction).asJson

      output must_== json"""{"id": "ssl", "value": ${transformName(ssl)}}"""
    }}

    "decode minify" >> {
      import PageRuleActionEnabled._
      val output =
        json"""{
                 "id": "minify",
                 "value": {
                   "html": "on",
                   "css": "on",
                   "js": "off"
                 }
               }""".as[PageRuleAction]

      output must beRight(Minify(On, On, Off))
    }

    "encode minify" >> {
      import PageRuleActionEnabled._
      val output = (Minify(On, On, Off): PageRuleAction).asJson

      output must_==
        json"""{
                 "id": "minify",
                 "value": {
                   "html": "on",
                   "css": "on",
                   "js": "off"
                 }
               }"""
    }

    "decode browser_cache_ttl" >> { prop { ttl: Int =>
      val output =
        json"""{
                 "id": "browser_cache_ttl",
                 "value": $ttl
               }""".as[PageRuleAction]

      output must beRight(BrowserCacheTtl(ttl))
    }}

    "encode browser_cache_ttl" >> { prop { ttl: Int =>
      val output = (BrowserCacheTtl(ttl): PageRuleAction).asJson

      output must_==
        json"""{
                 "id": "browser_cache_ttl",
                 "value": $ttl
               }"""
    }}

    "decode security_level" >> { prop { level: SecurityLevelValue =>
      val output =
        json"""{
                 "id": "security_level",
                 "value": ${transformName(level)}
               }""".as[PageRuleAction]

      output must beRight(SecurityLevel(level))
    } }

    "encode security_level" >> { prop { level: SecurityLevelValue =>
      val output = (SecurityLevel(level): PageRuleAction).asJson

      output must_==
        json"""{
                 "id": "security_level",
                 "value": ${transformName(level)}
               }"""
    }}

    "decode cache_level" >> { prop { level: CacheLevelValue =>
      val output =
        json"""{
                 "id": "cache_level",
                 "value": ${cacheLevelToString(level)}
               }""".as[PageRuleAction]

      output must beRight(CacheLevel(level))
    } }

    "encode cache_level" >> { prop { level: CacheLevelValue =>
      val output = (CacheLevel(level): PageRuleAction).asJson

      output must_==
        json"""{
                 "id": "cache_level",
                 "value": ${cacheLevelToString(level)}
               }"""
    }}

    "decode forwarding url" >> { prop { input: ForwardingStatusCode =>
      import PageRuleAction.redirectEncoder
      val output =
        json"""{
                 "id": "forwarding_url",
                 "value": {
                   "url": "https://hydragents.xyz",
                   "status_code": $input
                 }
               }""".as[PageRuleAction]

      output must beRight(ForwardingUrl(Uri.uri("https://hydragents.xyz"), input))
    }}

    "encode forwarding url" >> { prop { input: (Uri, ForwardingStatusCode) =>
      import PageRuleAction.redirectEncoder
      val output = (ForwardingUrl(input._1, input._2): PageRuleAction).asJson

      output must_==
        json"""{
                 "id": "forwarding_url",
                 "value": {
                   "url": ${input._1},
                   "status_code": ${input._2}
                 }
               }"""
    }}

  }

  "Custom decoders" should {
    "fail to decode non-minify JSON" >> {
      val input =
        json"""{
                 "id": "not-minify",
                 "value": {
                   "html": "on",
                   "css": "on",
                   "js": "on"
                 }
               }"""

      val output = input.as[Minify]

      output must beLeft(DecodingFailure("id must be `minify`", List(DownField("id"))))
    }

    "fail to decode non-forwarding-rule JSON" >> {
      val input =
        json"""{
                 "id": "not-forwarding_url",
                 "value": {
                   "url": "https://hydragents.xyz",
                   "status_code": 301
                 }
               }"""

      val output = input.as[ForwardingUrl]

      output must beLeft(DecodingFailure("id must be `forwarding_url`", List(DownField("id"))))
    }
  }

  private def cacheLevelToString(level: CacheLevelValue): String = {
    import CacheLevelValue._
    level match {
      case Bypass => "bypass"
      case NoQueryString => "basic"
      case IgnoreQueryString => "simplified"
      case Standard => "aggressive"
      case CacheEverything => "cache_everything"
    }
  }


}
