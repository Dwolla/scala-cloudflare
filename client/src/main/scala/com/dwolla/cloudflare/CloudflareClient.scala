package com.dwolla.cloudflare

import cats.Monad
import com.dwolla.cloudflare.clients.{AccountsClient, DnsRecordClient, RateLimitClient}

class CloudflareClient[F[_] : Monad](executor: CloudflareApiExecutor[F]) {
  lazy val Accounts = new AccountsClient(executor)
  lazy val RateLimits = new RateLimitClient(executor)
  lazy val Dns = new DnsRecordClient(executor)
}
