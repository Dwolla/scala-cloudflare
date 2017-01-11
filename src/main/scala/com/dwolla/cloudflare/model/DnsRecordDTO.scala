package com.dwolla.cloudflare.model

import com.dwolla.lambda.cloudflare.record.JsonWritable

case class DnsRecordDTO(id: Option[String] = None,
                        name: String,
                        content: String,
                        `type`: String,
                        ttl: Option[Int] = None,
                        proxied: Option[Boolean] = None) extends JsonWritable
