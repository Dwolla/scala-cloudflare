package com.dwolla.cloudflare

import cats._
import cats.effect._
import cats.syntax.all._
import com.dwolla.cloudflare.StreamingCloudflareApiExecutor._
import com.dwolla.cloudflare.domain.dto._
import com.dwolla.cloudflare.domain.model.Exceptions._
import com.dwolla.cloudflare.domain.model.response.Implicits
import com.dwolla.fs2utils.Pagination
import fs2.{Chunk, Stream}
import io.circe._
import monix.newtypes.NewtypeWrapped
import org.http4s.Header.Single
import org.http4s._
import org.http4s.circe._
import org.http4s.client._
import org.http4s.headers.`Content-Type`
import org.http4s.syntax.all._
import org.typelevel.ci._

case class CloudflareAuthorization(email: String, key: String)

object StreamingCloudflareApiExecutor {
  type `X-Auth-Email` = `X-Auth-Email`.Type
  object `X-Auth-Email` extends NewtypeWrapped[String] {
    implicit val header: Header[`X-Auth-Email`, Single] = Header.create[`X-Auth-Email`, Single](ci"X-Auth-Email", _.value, `X-Auth-Email`(_).asRight)
  }

  type `X-Auth-Key` = `X-Auth-Key`.Type
  object `X-Auth-Key` extends NewtypeWrapped[String] {
    implicit val header: Header[`X-Auth-Key`, Single] = Header.create[`X-Auth-Key`, Single](ci"X-Auth-Key", _.value, `X-Auth-Key`(_).asRight)
  }
}

class StreamingCloudflareApiExecutor[F[_] : Concurrent](client: Client[F], authorization: CloudflareAuthorization) {

  def raw[T](request: Request[F])(f: Response[F] => F[T]): F[T] =
    client.run(setupRequest(request)).use(f)

  def fetch[T: Decoder](req: Request[F]): Stream[F, T] =
    Pagination.offsetUnfoldChunkEval[F, Int, T] { maybePageNumber: Option[Int] =>
      val pagedRequest = maybePageNumber.fold(req) { pageNumber =>
        req.withUri(req.uri.withQueryParam("page", pageNumber))
      }

      for {
        pageData <- raw(pagedRequest)(responseToJson[T])
        (chunk, nextPage) <- pageData match {
          case BaseResponseDTO(false, Some(errors), _) if errors.exists(_.code == Option(81057)) =>
            Concurrent[F].raiseError(RecordAlreadyExists)
          case BaseResponseDTO(false, Some(errors), _) if errors.exists(cloudflareAuthorizationFormatError) =>
            Concurrent[F].raiseError(AccessDenied(errors.find(cloudflareAuthorizationFormatError).flatMap(_.error_chain).toList.flatten))
          case single: ResponseDTO[T] if single.success =>
            Applicative[F].pure((Chunk.seq(single.result.toSeq), None))
          case PagedResponseDTO(result, true, _, _, Some(result_info)) =>
            Applicative[F].pure((Chunk.seq(result), calculateNextPage(result_info.page, result_info.total_pages)))
          case PagedResponseDTO(result, true, _, _, None) =>
            Applicative[F].pure((Chunk.seq(result), None))
          case e =>
            val errors = e.errors.toList.flatten.map(Implicits.toError)
            val messages = e.messages.toList.flatten.map(r => com.dwolla.cloudflare.domain.model.Message(r.code, r.message, None))

            Concurrent[F].raiseError(UnexpectedCloudflareErrorException(errors, messages))
        }
      } yield (chunk, nextPage)
    }

  private def setupRequest(request: Request[F]) =
    request.withHeaders(Headers(
      `X-Auth-Email`(authorization.email),
      `X-Auth-Key`(authorization.key),
      `Content-Type`(mediaType"application/json"),
    ))

  private def responseToJson[T: Decoder](resp: Response[F]): F[BaseResponseDTO[T]] =
    resp match {
      case Status.Successful(_) | Status.NotFound(_) | Status.BadRequest(_) => resp.decodeJson[BaseResponseDTO[T]]
      case Status.Forbidden(_) => Concurrent[F].raiseError(AccessDenied())
      case _ => Concurrent[F].raiseError(UnexpectedCloudflareResponseStatus(resp.status))
    }

  private def calculateNextPage(currentPage: Int, totalPages: Int): Option[Int] =
    if (currentPage < totalPages) Option(currentPage + 1) else None

  private val cloudflareAuthorizationFormatError: ResponseInfoDTO => Boolean = resp => resp.code == Option(6003) && resp.error_chain.exists(_.exists(x => Set(6102, 6103).map(Option(_)).contains(x.code)))

}
