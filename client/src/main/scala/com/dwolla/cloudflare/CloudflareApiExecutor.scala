package com.dwolla.cloudflare

import cats._
import cats.effect._
import cats.implicits._
import com.dwolla.cloudflare.domain.dto._
import com.dwolla.cloudflare.domain.model.Exceptions._
import com.dwolla.cloudflare.domain.model.response.Implicits
import com.dwolla.fs2utils.Pagination
import fs2.{Chunk, Stream}
import io.circe._
import org.http4s._
import org.http4s.circe._
import org.http4s.client._

case class CloudflareAuthorization(email: String, key: String)

class StreamingCloudflareApiExecutor[F[_] : Sync](client: Client[F], authorization: CloudflareAuthorization) {

  def raw[T](request: Request[F])(f: Response[F] => F[T]): F[T] = {
    client.fetch(setupRequest(request))(f)
  }

  def fetch[T](req: Request[F])
              (implicit decoder: Decoder[T]): Stream[F, T] =
    Pagination.offsetUnfoldChunkEval[F, Int, T] { maybePageNumber: Option[Int] =>
      val pagedRequest = maybePageNumber.fold(req) { pageNumber =>
        req.withUri(req.uri.withQueryParam("page", pageNumber))
      }

      for {
        pageData <- raw(pagedRequest)(responseToJson[T])
        (chunk, nextPage) <- pageData match {
          case BaseResponseDTO(false, Some(errors), _) if errors.exists(_.code == Option(81057)) =>
            Sync[F].raiseError(RecordAlreadyExists)
          case BaseResponseDTO(false, Some(errors), _) if errors.exists(cloudflareAuthorizationFormatError) =>
            Sync[F].raiseError(AccessDenied(errors.find(cloudflareAuthorizationFormatError).flatMap(_.error_chain).toList.flatten))
          case single: ResponseDTO[T] if single.success =>
            Applicative[F].pure((Chunk.seq(single.result.toSeq), None))
          case PagedResponseDTO(result, true, _, _, Some(result_info)) =>
            Applicative[F].pure((Chunk.seq(result), calculateNextPage(result_info.page, result_info.total_pages)))
          case PagedResponseDTO(result, true, _, _, None) =>
            Applicative[F].pure((Chunk.seq(result), None))
          case e =>
            val errors = e.errors.toList.flatten.map(Implicits.toError)
            val messages = e.messages.toList.flatten.map(r => com.dwolla.cloudflare.domain.model.Message(r.code, r.message, None))

            Sync[F].raiseError(UnexpectedCloudflareErrorException(errors, messages))
        }
      } yield (chunk, nextPage)
    }

  private def setupRequest(request: Request[F]) = {
    val authEmailHeader = Header("X-Auth-Email", authorization.email)
    val authKeyHeader = Header("X-Auth-Key", authorization.key)
    val contentTypeHeader = Header("Content-Type", "application/json")

    request
      .withHeaders(Headers.of(authEmailHeader, authKeyHeader, contentTypeHeader))
  }

  private def responseToJson[T: Decoder](resp: Response[F]): F[BaseResponseDTO[T]] =
    resp match {
      case Status.Successful(_) | Status.NotFound(_) | Status.BadRequest(_) => resp.decodeJson[BaseResponseDTO[T]]
      case Status.Forbidden(_) => Sync[F].raiseError(AccessDenied())
      case _ => Sync[F].raiseError(UnexpectedCloudflareResponseStatus(resp.status))
    }

  private def calculateNextPage(currentPage: Int, totalPages: Int): Option[Int] =
    if (currentPage < totalPages) Option(currentPage + 1) else None

  private val cloudflareAuthorizationFormatError: ResponseInfoDTO => Boolean = resp => resp.code == Option(6003) && resp.error_chain.exists(_.exists(x => Set(6102, 6103).map(Option(_)).contains(x.code)))

}
