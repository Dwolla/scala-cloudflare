package com.dwolla.cloudflare.domain.model

import com.dwolla.cloudflare.common.JsonWritable

private[cloudflare] case class DnsRecordDTO(id: Option[String] = None,
                        name: String,
                        content: String,
                        `type`: String,
                        ttl: Option[Int] = None,
                        proxied: Option[Boolean] = None) extends JsonWritable
