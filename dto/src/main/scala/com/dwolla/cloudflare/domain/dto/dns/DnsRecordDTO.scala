package com.dwolla.cloudflare.domain.dto.dns

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

case class DnsRecordDTO(id: Option[String] = None,
                        name: String,
                        content: String,
                        `type`: String,
                        ttl: Option[Int] = None,
                        proxied: Option[Boolean] = None,
                        priority: Option[Int] = None,
                       )

object DnsRecordDTO {
  implicit val dnsRecordDTOCodec: Codec[DnsRecordDTO] = deriveCodec
}

case class ZoneDTO(id: Option[String],
                   name: String,
                  )

object ZoneDTO {
  implicit val zoneDTOCodec: Codec[ZoneDTO] = deriveCodec
}
