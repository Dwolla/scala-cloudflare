package com.dwolla.cloudflare.domain.dto.dns

import io.circe.{Encoder, Decoder}
import io.circe.generic.semiauto.{deriveEncoder, deriveDecoder}

case class DnsRecordDTO(id: Option[String] = None,
                        name: String,
                        content: String,
                        `type`: String,
                        ttl: Option[Int] = None,
                        proxied: Option[Boolean] = None,
                        priority: Option[Int] = None,
                       )

object DnsRecordDTO {
  implicit val dnsRecordDTOEncoder: Encoder[DnsRecordDTO] = deriveEncoder
  implicit val dnsRecordDTODecoder: Decoder[DnsRecordDTO] = deriveDecoder
}

case class ZoneDTO(id: Option[String],
                   name: String,
                  )

object ZoneDTO {
  implicit val zoneDTOEncoder: Encoder[ZoneDTO] = deriveEncoder
  implicit val zoneDTODecoder: Decoder[ZoneDTO] = deriveDecoder
}
