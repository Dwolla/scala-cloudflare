package com.dwolla.cloudflare.domain.model

import com.dwolla.cloudflare.domain.dto.ZoneSettingsDTO
import com.dwolla.cloudflare.domain.model.firewallrules.FirewallRuleFilter
import com.dwolla.cloudflare.domain.model.ratelimits.RateLimit
import io.circe.parser._
import munit.FunSuite

class MoreBooleanDecodingSpec extends FunSuite {

  test("ZoneSettingsDTO should decode boolean editable") {
    val json = """{"id":"test","value":"val","editable":true,"modified_on":"2021-01-01T00:00:00Z"}"""
    val expected = ZoneSettingsDTO("test", "val", Some(true), Some("2021-01-01T00:00:00Z"))
    assertEquals(decode[ZoneSettingsDTO](json), Right(expected))
  }

  test("ZoneSettingsDTO should decode string boolean editable") {
    val json = """{"id":"test","value":"val","editable":"true","modified_on":"2021-01-01T00:00:00Z"}"""
    val expected = ZoneSettingsDTO("test", "val", Some(true), Some("2021-01-01T00:00:00Z"))
    assertEquals(decode[ZoneSettingsDTO](json), Right(expected))
  }

  test("FirewallRuleFilter should decode boolean paused") {
    val json = """{"paused":true}"""
    val result = decode[FirewallRuleFilter](json)
    assert(result.isRight, s"Failed to decode: $result")
    assertEquals(result.toOption.flatMap(_.paused), Some(true))
  }

  test("FirewallRuleFilter should decode string boolean paused") {
    val json = """{"paused":"true"}"""
    val result = decode[FirewallRuleFilter](json)
    assert(result.isRight, s"Failed to decode: $result")
    assertEquals(result.toOption.flatMap(_.paused), Some(true))
  }

  test("RateLimit should decode boolean disabled") {
    val json = """{"disabled":true, "match": {"request": {"url": "test"}, "response": {}}, "threshold": 10, "period": 60, "action": {"mode": "simulate", "timeout": 60}}"""
    val result = decode[RateLimit](json)
    assert(result.isRight, s"Failed to decode: $result")
    assertEquals(result.toOption.flatMap(_.disabled), Some(true))
  }

  test("RateLimit should decode string boolean disabled") {
    val json = """{"disabled":"true", "match": {"request": {"url": "test"}, "response": {}}, "threshold": 10, "period": 60, "action": {"mode": "simulate", "timeout": 60}}"""
    val result = decode[RateLimit](json)
    assert(result.isRight, s"Failed to decode: $result")
    assertEquals(result.toOption.flatMap(_.disabled), Some(true))
  }
}
