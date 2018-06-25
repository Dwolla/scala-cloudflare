package com.dwolla.cloudflare.domain.model.dns

import com.dwolla.cloudflare.domain.dto.dns.DnsRecordDTO

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