package com.dwolla.cloudflare

import java.io.Closeable

import org.apache.http.HttpResponse
import org.apache.http.client.methods.HttpRequestBase
import org.apache.http.impl.client.{CloseableHttpClient, HttpClients}
import resource._

import scala.concurrent._
import scala.language.implicitConversions
import scala.util.Try

class CloudflareApiExecutor(authorization: CloudflareAuthorization)(implicit val ec: ExecutionContext) extends Closeable {
  lazy val httpClient: CloseableHttpClient = HttpClients.createDefault()
  private def blockingFetch[T]: (HttpRequestBase, HttpResponse ⇒ T) ⇒ Try[T] = CloudflareApiExecutor.blockingFetch[T](authorization, httpClient)

  def fetch[T](request: HttpRequestBase)(f: HttpResponse ⇒ T): Future[T] = Future(blocking(blockingFetch(request, f))).flatMap(Future.fromTry)

  override def close(): Unit = httpClient.close()
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


case class CloudflareAuthorization(email: String, key: String)
