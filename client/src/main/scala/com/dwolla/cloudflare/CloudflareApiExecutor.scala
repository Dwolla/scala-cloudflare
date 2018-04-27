package com.dwolla.cloudflare

import java.io.Closeable

import org.apache.http.HttpResponse
import org.apache.http.client.methods.HttpRequestBase
import org.apache.http.impl.client.{CloseableHttpClient, HttpClients}
import resource._

import scala.concurrent.{ExecutionContext, Future}
import scala.language.implicitConversions

class CloudflareApiExecutor(authorization: CloudflareAuthorization)(implicit val ec: ExecutionContext) extends Closeable {
  lazy val httpClient: CloseableHttpClient = HttpClients.createDefault()

  def fetch[T](request: HttpRequestBase)(f: HttpResponse ⇒ T): Future[T] = {
    Future.fromTry {
      request.addHeader("X-Auth-Email", authorization.email)
      request.addHeader("X-Auth-Key", authorization.key)
      request.addHeader("Content-Type", "application/json")

      (for {
        response ← managed(httpClient.execute(request))
      } yield f(response)).tried
    }

  }

  override def close(): Unit = httpClient.close()
}

case class CloudflareAuthorization(email: String, key: String)
