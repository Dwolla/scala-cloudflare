package com.dwolla.cloudflare

import java.io.Closeable

import cats.effect._
import cats.implicits._
import org.apache.http.HttpResponse
import org.apache.http.client.methods.HttpRequestBase
import org.apache.http.impl.client._
import resource._

import scala.concurrent._
import scala.language.higherKinds
import scala.util.Try

trait CloudflareApiExecutor[F[_]] {
  def fetch[T](request: HttpRequestBase)(f: HttpResponse ⇒ T): F[T]
}

object CloudflareApiExecutor {
  private[cloudflare] def blockingFetch[T](authorization: CloudflareAuthorization, httpClient: CloseableHttpClient)(request: HttpRequestBase, f: HttpResponse ⇒ T): Try[T] = {
    request.addHeader("X-Auth-Email", authorization.email)
    request.addHeader("X-Auth-Key", authorization.key)
    request.addHeader("Content-Type", "application/json")

    (for {
      response ← managed(httpClient.execute(request))
    } yield f(response)).tried
  }
}

class FutureCloudflareApiExecutor(authorization: CloudflareAuthorization)(implicit ec: ExecutionContext) extends CloudflareApiExecutor[Future] with Closeable {
  lazy val httpClient: CloseableHttpClient = HttpClients.createDefault()
  private def blockingFetchFunction[T]: (HttpRequestBase, HttpResponse ⇒ T) ⇒ Try[T] = CloudflareApiExecutor.blockingFetch(authorization, httpClient)

  override def fetch[T](request: HttpRequestBase)(f: HttpResponse ⇒ T): Future[T] =
    Future(blocking(blockingFetchFunction(request, f)))
      .flatMap(Future.fromTry)

  override def close(): Unit = httpClient.close()
}

case class CloudflareAuthorization(email: String, key: String)

class AsyncCloudflareApiExecutor[F[_]: Async](authorization: CloudflareAuthorization)(implicit ec: ExecutionContext) extends CloudflareApiExecutor[F] with Closeable {
  lazy val httpClient: CloseableHttpClient = HttpClients.createDefault()
  private def blockingFetchFunction[T]: (HttpRequestBase, HttpResponse ⇒ T) ⇒ Try[T] = CloudflareApiExecutor.blockingFetch(authorization, httpClient)

  override def fetch[T](request: HttpRequestBase)(f: HttpResponse ⇒ T): F[T] =
    for {
      _ ← Async.shift(ec)
      tried ← Sync[F].delay(blockingFetchFunction(request, f))
      output ← Async[F].fromTry(tried)
    } yield output

  override def close(): Unit = httpClient.close()
}
