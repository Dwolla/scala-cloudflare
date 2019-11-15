package com.dwolla.cloudflare
package domain
package model

import com.dwolla.cloudflare.domain.dto.dns.DnsRecordDTO

import scala.language.implicitConversions
import scala.util.matching.Regex

trait DnsRecord {
  val name: String
  val content: String
  val recordType: String
  val ttl: Option[Int]
  val proxied: Option[Boolean]
  val priority: Option[Int]
}

case class UnidentifiedDnsRecord(name: String,
                                 content: String,
                                 recordType: String,
                                 ttl: Option[Int] = None,
                                 proxied: Option[Boolean] = None,
                                 priority: Option[Int] = None,
                                ) extends DnsRecord {

  import IdentifiedDnsRecord._

  def identifyAs(physicalResourceId: String): IdentifiedDnsRecord = physicalResourceId match {
    case dnsRecordIdUrlRegex(zoneId, recordId) => identifyAs(zoneId, recordId)
    case _ => throw new RuntimeException("Passed string does not match URL pattern for Cloudflare DNS record")
  }

  def identifyAs(zoneId: String, recordId: String): IdentifiedDnsRecord = IdentifiedDnsRecord(
    physicalResourceId = tagPhysicalResourceId(s"https://api.cloudflare.com/client/v4/zones/$zoneId/dns_records/$recordId"),
    zoneId = tagZoneId(zoneId),
    resourceId = tagResourceId(recordId),
    name = this.name,
    content = this.content,
    recordType = this.recordType,
    ttl = this.ttl,
    proxied = this.proxied,
    priority = this.priority,
  )

}

case class IdentifiedDnsRecord(physicalResourceId: PhysicalResourceId,
                               zoneId: ZoneId,
                               resourceId: ResourceId,
                               name: String,
                               content: String,
                               recordType: String,
                               ttl: Option[Int] = None,
                               proxied: Option[Boolean] = None,
                               priority: Option[Int] = None,
                              ) extends DnsRecord {
  def unidentify: UnidentifiedDnsRecord = UnidentifiedDnsRecord(
    name = this.name,
    content = this.content,
    recordType = this.recordType,
    ttl = this.ttl,
    proxied = this.proxied,
    priority = this.priority,
  )

}

object IdentifiedDnsRecord {
  val dnsRecordIdUrlRegex: Regex = "https://api.cloudflare.com/client/v4/zones/([^/]+)/dns_records/([^/]+)".r("zoneId", "recordId")
}

object Implicits {
  implicit def toDto(dnsRecord: DnsRecord): DnsRecordDTO = DnsRecordDTO(
    id = dnsRecord match {
      case identified: IdentifiedDnsRecord => Option(identified.resourceId)
      case _: UnidentifiedDnsRecord => None
    },
    name = dnsRecord.name,
    content = dnsRecord.content,
    `type` = dnsRecord.recordType,
    ttl = dnsRecord.ttl,
    proxied = dnsRecord.proxied,
    priority = dnsRecord.priority,
  )

  implicit def fromDto(dnsRecordDto: DnsRecordDTO): UnidentifiedDnsRecord = UnidentifiedDnsRecord(
    name = dnsRecordDto.name,
    content = dnsRecordDto.content,
    recordType = dnsRecordDto.`type`,
    ttl = dnsRecordDto.ttl,
    proxied = dnsRecordDto.proxied,
    priority = dnsRecordDto.priority,
  )

  implicit def fromDtoZoneIdTuple(tuple: (DnsRecordDTO, String))(implicit ev: DnsRecordDTO => UnidentifiedDnsRecord): IdentifiedDnsRecord = tuple match {
    case (dnsRecordDTO: DnsRecordDTO, zoneId: String) => dnsRecordDTO.id.fold(throw new RuntimeException) { recordId =>
      dnsRecordDTO.identifyAs(zoneId, recordId)
    }
  }

  implicit class DnsRecordToDto(dnsRecord: DnsRecord) {
    def toDto: DnsRecordDTO = Implicits.toDto(dnsRecord)
  }
}
