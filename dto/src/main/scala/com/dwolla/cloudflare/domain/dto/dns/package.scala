package com.dwolla.cloudflare.domain.dto

import shapeless.tag.@@

package object dns {
  type DnsRecordAccountId = String @@ DnsRecordAccountIdTag
  type DnsRecordAccountName = String @@ DnsRecordAccountNameTag

  private[cloudflare] val tagDnsRecordAccountId: String => DnsRecordAccountId = shapeless.tag[DnsRecordAccountIdTag][String]
  private[cloudflare] val tagDnsRecordAccountName: String => DnsRecordAccountName = shapeless.tag[DnsRecordAccountNameTag][String]
}

package dns {
  trait DnsRecordAccountIdTag
  trait DnsRecordAccountNameTag
}


