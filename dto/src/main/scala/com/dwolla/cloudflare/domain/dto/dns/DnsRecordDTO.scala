package com.dwolla.cloudflare.domain.dto.dns

case class DnsRecordDTO(id: Option[String] = None,
                        name: String,
                        content: String,
                        `type`: String,
                        ttl: Option[Int] = None,
                        proxied: Option[Boolean] = None,
                        priority: Option[Int] = None,
                       )

case class ZoneDTO(id: Option[String],
                   name: String,
                  )
