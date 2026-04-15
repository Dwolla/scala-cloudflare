package com.dwolla.cloudflare.domain.model

import com.dwolla.cloudflare.domain.dto.dns.DnsRecordDTO
import io.circe.literal._
import munit.FunSuite

class DnsRecordBooleanDecodingSpec extends FunSuite {

  test("DnsRecordDTO should decode boolean proxied") {
    val json = json"""{"name":"test","content":"1.2.3.4","type":"A","proxied":true}"""
    val expected = DnsRecordDTO(None, "test", "1.2.3.4", "A", proxied = Some(true))
    assertEquals(json.as[DnsRecordDTO], Right(expected))
  }

  test("DnsRecordDTO should decode string boolean proxied") {
    val json = json"""{"name":"test","content":"1.2.3.4","type":"A","proxied":"true"}"""
    val expected = DnsRecordDTO(None, "test", "1.2.3.4", "A", proxied = Some(true))
    assertEquals(json.as[DnsRecordDTO], Right(expected))
  }

  test("UnidentifiedDnsRecord should decode boolean proxied") {
    val json = json"""{"name":"test","content":"1.2.3.4","recordType":"A","proxied":true}"""
    val expected = UnidentifiedDnsRecord("test", "1.2.3.4", "A", proxied = Some(true))
    assertEquals(json.as[UnidentifiedDnsRecord], Right(expected))
  }

  test("UnidentifiedDnsRecord should decode string boolean proxied") {
    val json = json"""{"name":"test","content":"1.2.3.4","recordType":"A","proxied":"true"}"""
    val expected = UnidentifiedDnsRecord("test", "1.2.3.4", "A", proxied = Some(true))
    assertEquals(json.as[UnidentifiedDnsRecord], Right(expected))
  }

  test("IdentifiedDnsRecord should decode boolean proxied") {
    val json = json"""{"physicalResourceId":"https://api.cloudflare.com/client/v4/zones/zone-id/dns_records/record-id","zoneId":"zone-id","resourceId":"record-id","name":"test","content":"1.2.3.4","recordType":"A","proxied":true}"""
    val expected = IdentifiedDnsRecord(
      physicalResourceId = tagPhysicalResourceId("https://api.cloudflare.com/client/v4/zones/zone-id/dns_records/record-id"),
      zoneId = ZoneId("zone-id"),
      resourceId = ResourceId("record-id"),
      name = "test",
      content = "1.2.3.4",
      recordType = "A",
      proxied = Some(true)
    )
    assertEquals(json.as[IdentifiedDnsRecord], Right(expected))
  }

  test("IdentifiedDnsRecord should decode string boolean proxied") {
    val json = json"""{"physicalResourceId":"https://api.cloudflare.com/client/v4/zones/zone-id/dns_records/record-id","zoneId":"zone-id","resourceId":"record-id","name":"test","content":"1.2.3.4","recordType":"A","proxied":"true"}"""
    val expected = IdentifiedDnsRecord(
      physicalResourceId = tagPhysicalResourceId("https://api.cloudflare.com/client/v4/zones/zone-id/dns_records/record-id"),
      zoneId = ZoneId("zone-id"),
      resourceId = ResourceId("record-id"),
      name = "test",
      content = "1.2.3.4",
      recordType = "A",
      proxied = Some(true)
    )
    assertEquals(json.as[IdentifiedDnsRecord], Right(expected))
  }
}
