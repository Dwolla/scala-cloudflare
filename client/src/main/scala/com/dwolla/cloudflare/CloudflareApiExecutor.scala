package com.dwolla.cloudflare

import java.io.Closeable

import cats.effect._
import cats.implicits._
import com.dwolla.cloudflare.domain.dto._
import com.dwolla.cloudflare.domain.model.Exceptions._
import com.dwolla.fs2utils.Pagination
import fs2.{Segment, Stream}
import io.circe._
import io.circe.generic.auto._
import org.apache.http.HttpResponse
import org.apache.http.client.methods.HttpRequestBase
import org.apache.http.impl.client._
import org.http4s._
import org.http4s.circe._
import org.http4s.client._
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

object StreamingCloudflareApiExecutor {
  implicit def baseResponseDtoDecoder[T: Decoder]: Decoder[BaseResponseDTO[T]] = Decoder[ResponseDTO[T]].widen.or(Decoder[PagedResponseDTO[T]].widen)
}

class StreamingCloudflareApiExecutor[F[_]: Sync](client: Client[F], authorization: CloudflareAuthorization) {
  import StreamingCloudflareApiExecutor._

  def raw[T](request: Request[F])(f: Response[F] => F[T]): F[T] = {
    client.fetch(setupRequest(request))(f)
  }

  def fetch[T](req: Request[F])
              (implicit decoder: Decoder[T]): Stream[F, T] =
    Pagination.offsetUnfoldSegmentEval { maybePageNumber: Option[Int] ⇒
      val pagedRequest = maybePageNumber.fold(req) { pageNumber ⇒
        req.withUri(req.uri.withQueryParam("page", pageNumber))
      }

      for {
        pageData ← raw(pagedRequest)(responseToJson[T])
        (segment, nextPage) = pageData match {
          case single: ResponseDTO[T] ⇒
            (Segment.seq(single.result.toSeq), None)
          case paged: PagedResponseDTO[T] ⇒
            (Segment.seq(paged.result), calculateNextPage(paged.result_info.page, paged.result_info.total_pages))
        }
      } yield (segment, nextPage)
    }

  private def setupRequest(request: Request[F]) = {
    val authEmailHeader = Header("X-Auth-Email", authorization.email)
    val authKeyHeader = Header("X-Auth-Key", authorization.key)
    val contentTypeHeader = Header("Content-Type", "application/json")

    request
      .withHeaders(Headers(authEmailHeader, authKeyHeader, contentTypeHeader))
  }

  private def responseToJson[T: Decoder](resp: Response[F]): F[BaseResponseDTO[T]] =
    resp match {
      case Status.Successful(_) | Status.NotFound(_) | Status.BadRequest(_) ⇒ resp.decodeJson
      case _ ⇒ Sync[F].raiseError(UnexpectedCloudflareResponseStatus(resp.status))
    }

  private def calculateNextPage(currentPage: Int, totalPages: Int): Option[Int] =
    if (currentPage < totalPages) Option(currentPage + 1) else None

}
