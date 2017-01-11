package com.dwolla.cloudflare.model

import scala.language.implicitConversions
import scala.util.matching.Regex

trait DnsRecord {
  val name: String
  val content: String
  val recordType: String
  val ttl: Option[Int]
  val proxied: Option[Boolean]
}

case class UnidentifiedDnsRecord(name: String,
                                 content: String,
                                 recordType: String,
                                 ttl: Option[Int] = None,
                                 proxied: Option[Boolean] = None) extends DnsRecord {

  import IdentifiedDnsRecord._

  def identifyAs(physicalResourceId: String): IdentifiedDnsRecord = physicalResourceId match {
    case dnsRecordIdUrlRegex(zoneId, recordId) ⇒
      IdentifiedDnsRecord(
        physicalResourceId = s"https://api.cloudflare.com/client/v4/zones/$zoneId/dns_records/$recordId",
        zoneId = zoneId,
        resourceId = recordId,
        name = this.name,
        content = this.content,
        recordType = this.recordType,
        ttl = this.ttl,
        proxied = this.proxied
      )
    case _ ⇒ throw new RuntimeException("Passed string does not match URL pattern for Cloudflare DNS record")
  }

  def identifyAs(zoneId: String, recordId: String): IdentifiedDnsRecord = IdentifiedDnsRecord(
    physicalResourceId = s"https://api.cloudflare.com/client/v4/zones/$zoneId/dns_records/$recordId",
    zoneId = zoneId,
    resourceId = recordId,
    name = this.name,
    content = this.content,
    recordType = this.recordType,
    ttl = this.ttl,
    proxied = this.proxied
  )

}

case class IdentifiedDnsRecord(physicalResourceId: String,
                               zoneId: String,
                               resourceId: String,
                               name: String,
                               content: String,
                               recordType: String,
                               ttl: Option[Int] = None,
                               proxied: Option[Boolean] = None) extends DnsRecord {
  def unidentify: UnidentifiedDnsRecord = UnidentifiedDnsRecord(
    name = this.name,
    content = this.content,
    recordType = this.recordType,
    ttl = this.ttl,
    proxied = this.proxied
  )

}

object IdentifiedDnsRecord {
  val dnsRecordIdUrlRegex: Regex = "https://api.cloudflare.com/client/v4/zones/([^/]+)/dns_records/([^/]+)".r("zoneId", "recordId")
}

object Implicits {
  implicit def toDto(dnsRecord: DnsRecord): DnsRecordDTO = DnsRecordDTO(
    id = dnsRecord match {
      case identified: IdentifiedDnsRecord ⇒ Option(identified.physicalResourceId)
      case _: UnidentifiedDnsRecord ⇒ None
    },
    name = dnsRecord.name,
    content = dnsRecord.content,
    `type` = dnsRecord.recordType,
    ttl = dnsRecord.ttl,
    proxied = dnsRecord.proxied
  )

  implicit def fromDto(dnsRecordDto: DnsRecordDTO): UnidentifiedDnsRecord = UnidentifiedDnsRecord(
    name = dnsRecordDto.name,
    content = dnsRecordDto.content,
    recordType = dnsRecordDto.`type`,
    ttl = dnsRecordDto.ttl,
    proxied = dnsRecordDto.proxied
  )

  implicit def fromDtoZoneIdTuple(tuple: (DnsRecordDTO, String))(implicit ev: DnsRecordDTO ⇒ UnidentifiedDnsRecord): IdentifiedDnsRecord = tuple match {
    case (dnsRecordDTO: DnsRecordDTO, zoneId: String) ⇒ dnsRecordDTO.id.fold(throw new RuntimeException) { recordId ⇒
      dnsRecordDTO.identifyAs(zoneId, recordId)
    }
  }
}
