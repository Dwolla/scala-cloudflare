package com.dwolla.cloudflare

import java.io.Closeable

import com.dwolla.cloudflare.CloudflareApiExecutor.tryFuture
import org.apache.http.HttpResponse
import org.apache.http.client.methods.HttpRequestBase
import org.apache.http.impl.client.{CloseableHttpClient, HttpClients}
import resource._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try
import scala.language.implicitConversions

class CloudflareApiExecutor(authorization: CloudflareAuthorization)(implicit val ec: ExecutionContext) extends Closeable {
  lazy val httpClient: CloseableHttpClient = HttpClients.createDefault()

  def fetch[T](request: HttpRequestBase)(f: HttpResponse ⇒ T): Future[T] = {
    val variableToForceImplicitConversion = Future {
      request.addHeader("X-Auth-Email", authorization.email)
      request.addHeader("X-Auth-Key", authorization.key)
      request.addHeader("Content-Type", "application/json")

      (for {
        response ← managed(httpClient.execute(request))
      } yield f(response)).tried
    }
    variableToForceImplicitConversion
  }

  override def close(): Unit = httpClient.close()
}

case class CloudflareAuthorization(email: String, key: String)

object CloudflareApiExecutor {
  implicit def tryFuture[T](f: Future[Try[T]])(implicit ec: ExecutionContext): Future[T] = f.map(_.get)
}
