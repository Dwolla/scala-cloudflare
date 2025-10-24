package com.dwolla.cloudflare.domain.dto

import com.dwolla.cloudflare.CloudflareNewtype

package object dns {
  type DnsRecordAccountId = DnsRecordAccountId.Type
  object DnsRecordAccountId extends CloudflareNewtype[String]

  type DnsRecordAccountName = DnsRecordAccountName.Type
  object DnsRecordAccountName extends CloudflareNewtype[String]
}
