package com.dwolla.cloudflare

import cats._
import cats.effect._
import cats.implicits._
import com.dwolla.cloudflare.domain.dto._
import com.dwolla.cloudflare.domain.model.Exceptions._
import com.dwolla.fs2utils.Pagination
import fs2.{Segment, Stream}
import io.circe._
import io.circe.generic.auto._
import org.http4s._
import org.http4s.circe._
import org.http4s.client._

import scala.language.higherKinds

case class CloudflareAuthorization(email: String, key: String)

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
    Pagination.offsetUnfoldSegmentEval[F, Int, T] { maybePageNumber: Option[Int] ⇒
      val pagedRequest = maybePageNumber.fold(req) { pageNumber ⇒
        req.withUri(req.uri.withQueryParam("page", pageNumber))
      }

      for {
        pageData ← raw(pagedRequest)(responseToJson[T])
        (segment, nextPage) ← pageData match {
          case BaseResponseDTO(false, Some(errors), _) if errors.exists(_.code == 81057) ⇒
            Sync[F].raiseError(RecordAlreadyExists)
          case BaseResponseDTO(false, Some(errors), _) if errors.exists(cloudflareAuthorizationFormatError) ⇒
            Sync[F].raiseError(AccessDenied(errors.find(cloudflareAuthorizationFormatError).flatMap(_.error_chain).toList.flatten))
          case single: ResponseDTO[T] ⇒
            Applicative[F].pure((Segment.seq(single.result.toSeq), None))
          case paged: PagedResponseDTO[T] ⇒
            Applicative[F].pure((Segment.seq(paged.result), calculateNextPage(paged.result_info.page, paged.result_info.total_pages)))
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
      case Status.Forbidden(_) ⇒ Sync[F].raiseError(AccessDenied())
      case _ ⇒ Sync[F].raiseError(UnexpectedCloudflareResponseStatus(resp.status))
    }

  private def calculateNextPage(currentPage: Int, totalPages: Int): Option[Int] =
    if (currentPage < totalPages) Option(currentPage + 1) else None

  private val cloudflareAuthorizationFormatError: ResponseInfoDTO ⇒ Boolean = resp ⇒ resp.code == 6003 && resp.error_chain.exists(_.exists(x ⇒ Set(6102, 6103).contains(x.code)))

}
