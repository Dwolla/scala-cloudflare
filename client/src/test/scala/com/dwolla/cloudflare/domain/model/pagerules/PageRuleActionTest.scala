package com.dwolla.cloudflare.domain.model.pagerules

import com.dwolla.cloudflare.domain.model.pagerules.CacheLevelValue.Standard
import io.circe.CursorOp.DownField
import io.circe.DecodingFailure
import io.circe.generic.extras.Configuration
import io.circe.literal.*
import io.circe.syntax.*
import munit.ScalaCheckSuite
import org.http4s.circe.*
import org.http4s.Uri
import org.http4s.laws.discipline.arbitrary.*
import org.http4s.syntax.all.*
import org.scalacheck.*
import org.scalacheck.Prop.forAll

class PageRuleActionTest extends ScalaCheckSuite with ScalacheckShapeless {

  // Arbitrary instances for property testing
  implicit val arbPageRuleActionEnabled: Arbitrary[PageRuleActionEnabled] =
    Arbitrary(Gen.oneOf(PageRuleActionEnabled.On, PageRuleActionEnabled.Off))

  implicit val arbSslSetting: Arbitrary[SslSetting] = Arbitrary(Gen.oneOf(
    SslSetting.Off,
    SslSetting.Flexible,
    SslSetting.Full,
    SslSetting.Strict,
    SslSetting.OriginPull,
  ))

  implicit val arbSecurityLevelValue: Arbitrary[SecurityLevelValue] = Arbitrary(Gen.oneOf(
    SecurityLevelValue.Off,
    SecurityLevelValue.EssentiallyOff,
    SecurityLevelValue.Low,
    SecurityLevelValue.Medium,
    SecurityLevelValue.High,
    SecurityLevelValue.UnderAttack,
  ))

  implicit val arbCacheLevelValue: Arbitrary[CacheLevelValue] = Arbitrary(Gen.oneOf(
    CacheLevelValue.Bypass,
    CacheLevelValue.NoQueryString,
    CacheLevelValue.IgnoreQueryString,
    CacheLevelValue.Standard,
    CacheLevelValue.CacheEverything,
  ))

  implicit val arbForwardingStatusCode: Arbitrary[ForwardingStatusCode] =
    Arbitrary(Gen.oneOf(TemporaryRedirect, PermanentRedirect))

  implicit val arbInt: Arbitrary[Int] = Arbitrary(Gen.chooseNum(Int.MinValue, Int.MaxValue))

  def transformName[T](t: T): String = Configuration.snakeCaseTransformation(String.valueOf(t))

  // PageRule encode/decode
  test("PageRule encode") {
    val output = PageRule(
      id = Option("page-rule-id").map(tagPageRuleId),
      targets = List.empty,
      actions = List(
        ForwardingUrl(uri"http://l@:1", PermanentRedirect),
        DisableSecurity,
        CacheLevel(Standard),
      ),
      priority = 0,
      status = PageRuleStatus.Active,
      modified_on = None,
      created_on = None
    ).asJson

    val expected = json"""{
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

    assertEquals(output, expected)
  }

  test("PageRule encode without ID") {
    val output = PageRule(
      id = None,
      targets = List.empty,
      actions = List(
        ForwardingUrl(uri"http://l@:1", PermanentRedirect),
        DisableSecurity,
        CacheLevel(Standard),
      ),
      priority = 0,
      status = PageRuleStatus.Active,
      modified_on = None,
      created_on = None
    ).asJson

    val expected = json"""{
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

    assertEquals(output, expected)
  }

  test("PageRule decode") {
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

    val expected = Right(PageRule(
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

    assertEquals(output, expected)
  }

  // PageRuleAction encode/decode
  test("PageRuleAction decode disable_security") {
    val output: Either[DecodingFailure, PageRuleAction] =
      json"""{
                 "id": "disable_security"
               }""".as[PageRuleAction]

    assertEquals(output, Right(DisableSecurity: PageRuleAction))
  }

  test("PageRuleAction encode DisableSecurity") {
    val output = (DisableSecurity: PageRuleAction).asJson
    assertEquals(output, json"""{"id": "disable_security"}""")
  }

  property("PageRuleAction decode browser_check") {
    forAll { (enabled: PageRuleActionEnabled) =>
      val output =
        json"""{
                 "id": "browser_check",
                 "value": ${transformName(enabled)}
               }""".as[PageRuleAction]

      assertEquals(output, Right(BrowserCheck(enabled)))
    }
  }

  property("PageRuleAction encode BrowserCheck") {
    forAll { (enabled: PageRuleActionEnabled) =>
      val output = (BrowserCheck(enabled): PageRuleAction).asJson
      assertEquals(output, json"""{"id": "browser_check", "value": ${transformName(enabled)}}""")
    }
  }

  property("PageRuleAction decode ssl") {
    forAll { (ssl: SslSetting) =>
      val output =
        json"""{
                 "id": "ssl",
                 "value": ${transformName(ssl)}
               }""".as[PageRuleAction]

      assertEquals(output, Right(Ssl(ssl)))
    }
  }

  property("PageRuleAction encode ssl") {
    forAll { (ssl: SslSetting) =>
      val output = (Ssl(ssl): PageRuleAction).asJson
      assertEquals(output, json"""{"id": "ssl", "value": ${transformName(ssl)}}""")
    }
  }

  test("PageRuleAction decode minify") {
    import PageRuleActionEnabled.*
    val output =
      json"""{
                 "id": "minify",
                 "value": {
                   "html": "on",
                   "css": "on",
                   "js": "off"
                 }
               }""".as[PageRuleAction]

    assertEquals(output, Right(Minify(On, On, Off)))
  }

  test("PageRuleAction encode minify") {
    import PageRuleActionEnabled.*
    val output = (Minify(On, On, Off): PageRuleAction).asJson

    val expected = json"""{
                 "id": "minify",
                 "value": {
                   "html": "on",
                   "css": "on",
                   "js": "off"
                 }
               }"""

    assertEquals(output, expected)
  }

  property("PageRuleAction decode browser_cache_ttl") {
    forAll { (ttl: Int) =>
      val output =
        json"""{
                 "id": "browser_cache_ttl",
                 "value": $ttl
               }""".as[PageRuleAction]

      assertEquals(output, Right(BrowserCacheTtl(ttl)))
    }
  }

  property("PageRuleAction encode browser_cache_ttl") {
    forAll { (ttl: Int) =>
      val output = (BrowserCacheTtl(ttl): PageRuleAction).asJson

      val expected = json"""{
                 "id": "browser_cache_ttl",
                 "value": $ttl
               }"""

      assertEquals(output, expected)
    }
  }

  property("PageRuleAction decode security_level") {
    forAll { (level: SecurityLevelValue) =>
      val output =
        json"""{
                 "id": "security_level",
                 "value": ${transformName(level)}
               }""".as[PageRuleAction]

      assertEquals(output, Right(SecurityLevel(level)))
    }
  }

  property("PageRuleAction encode security_level") {
    forAll { (level: SecurityLevelValue) =>
      val output = (SecurityLevel(level): PageRuleAction).asJson

      val expected = json"""{
                 "id": "security_level",
                 "value": ${transformName(level)}
               }"""

      assertEquals(output, expected)
    }
  }

  property("PageRuleAction decode cache_level") {
    forAll { (level: CacheLevelValue) =>
      val output =
        json"""{
                 "id": "cache_level",
                 "value": ${cacheLevelToString(level)}
               }""".as[PageRuleAction]

      assertEquals(output, Right(CacheLevel(level)))
    }
  }

  property("PageRuleAction encode cache_level") {
    forAll { (level: CacheLevelValue) =>
      val output = (CacheLevel(level): PageRuleAction).asJson

      val expected = json"""{
                 "id": "cache_level",
                 "value": ${cacheLevelToString(level)}
               }"""

      assertEquals(output, expected)
    }
  }

  property("PageRuleAction decode forwarding url") {
    forAll { (input: ForwardingStatusCode) =>

      val output =
        json"""{
                 "id": "forwarding_url",
                 "value": {
                   "url": "https://hydragents.xyz",
                   "status_code": $input
                 }
               }""".as[PageRuleAction]

      assertEquals(output, Right(ForwardingUrl(uri"https://hydragents.xyz", input)))
    }
  }

  property("PageRuleAction encode forwarding url") {
    forAll { (uri: Uri, code: ForwardingStatusCode) =>

      val output = (ForwardingUrl(uri, code): PageRuleAction).asJson

      val expected = json"""{
                 "id": "forwarding_url",
                 "value": {
                   "url": $uri,
                   "status_code": $code
                 }
               }"""

      assertEquals(output, expected)
    }
  }

  // Custom decoders error cases
  test("Custom decoders fail to decode non-minify JSON") {
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

    assertEquals(output, Left(DecodingFailure("id must be `minify`", List(DownField("id")))))
  }

  test("Custom decoders fail to decode non-forwarding-rule JSON") {
    val input =
      json"""{
                 "id": "not-forwarding_url",
                 "value": {
                   "url": "https://hydragents.xyz",
                   "status_code": 301
                 }
               }"""

    val output = input.as[ForwardingUrl]

    assertEquals(output, Left(DecodingFailure("id must be `forwarding_url`", List(DownField("id")))))
  }

  private def cacheLevelToString(level: CacheLevelValue): String = {
    import CacheLevelValue.*
    level match {
      case Bypass => "bypass"
      case NoQueryString => "basic"
      case IgnoreQueryString => "simplified"
      case Standard => "aggressive"
      case CacheEverything => "cache_everything"
    }
  }
}
