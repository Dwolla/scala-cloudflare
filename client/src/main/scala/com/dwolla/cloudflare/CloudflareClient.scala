package com.dwolla.cloudflare

import java.io.Closeable

import cats.Monad
import cats.effect.Async
import com.dwolla.cloudflare.clients.{AccountsClient, DnsRecordClient, RateLimitClient}

import scala.concurrent.{ExecutionContext, Future}
import scala.language.higherKinds

trait CloudflareClient[F[_]] {
  def Accounts: AccountsClient[F]
  def RateLimits: RateLimitClient[F]
  def Dns: DnsRecordClient[F]
}

class FutureCloudflareClient(authorization: CloudflareAuthorization)(implicit ec: ExecutionContext, implicit val evidence: Monad[Future]) extends CloudflareClient[Future] with Closeable {
  private val executor = new FutureCloudflareApiExecutor(authorization)

  override lazy val Accounts = new AccountsClient(executor)
  override lazy val RateLimits = new RateLimitClient(executor)
  override lazy val Dns = new DnsRecordClient(executor)

  override def close(): Unit = executor.close()
}

class AsyncCloudflareClient[F[_]: Async](authorization: CloudflareAuthorization)(implicit ec: ExecutionContext) extends CloudflareClient[F] with Closeable {
  private val executor = new AsyncCloudflareApiExecutor(authorization)

  override lazy val Accounts = new AccountsClient(executor)
  override lazy val RateLimits = new RateLimitClient(executor)
  override lazy val Dns = new DnsRecordClient(executor)

  override def close(): Unit = executor.close()
}