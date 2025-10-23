package com.dwolla.cloudflare

import cats.effect.{Trace as _, *}
import cats.syntax.all.*
import com.dwolla.cloudflare.StreamingCloudflareApiExecutor.*
import com.dwolla.cloudflare.domain.dto.*
import com.dwolla.cloudflare.domain.model.Exceptions.*
import com.dwolla.cloudflare.domain.model.response.Implicits
import com.dwolla.fs2utils.Pagination
import fs2.{Chunk, Stream}
import io.circe.*
import org.http4s.*
import org.http4s.Header.Single
import org.http4s.circe.*
import org.http4s.client.*
import org.http4s.headers.{Authorization, `Content-Type`}
import org.http4s.syntax.all.*
import org.typelevel.ci.*
import natchez.Trace
import com.dwolla.tracing.LowPriorityTraceableValueInstances.*

sealed trait CloudflareAuthorization
object CloudflareAuthorization {
  def apply(token: String): CloudflareAuthorization = ApiToken(token)
  def apply(email: String, key: String): CloudflareAuthorization = ApiKey(email, key)
}

case class ApiToken(token: String) extends CloudflareAuthorization
case class ApiKey(email: String, key: String) extends CloudflareAuthorization

object StreamingCloudflareApiExecutor {
  type `X-Auth-Email` = `X-Auth-Email`.Type
  object `X-Auth-Email` extends CloudflareNewtype[String] {
    implicit val header: Header[`X-Auth-Email`, Single] = Header.create[`X-Auth-Email`, Single](ci"X-Auth-Email", _.value, `X-Auth-Email`(_).asRight)
  }

  type `X-Auth-Key` = `X-Auth-Key`.Type
  object `X-Auth-Key` extends CloudflareNewtype[String] {
    implicit val header: Header[`X-Auth-Key`, Single] = Header.create[`X-Auth-Key`, Single](ci"X-Auth-Key", _.value, `X-Auth-Key`(_).asRight)
  }
}

class StreamingCloudflareApiExecutor[F[_] : Concurrent : Trace](client: Client[F], authorization: CloudflareAuthorization) {

  def raw[T](request: Request[F])(f: Response[F] => F[T]): F[T] =
    client.run(setupRequest(request)).use(f)

  def fetch[T: Decoder](req: Request[F]): Stream[F, T] =
    Trace[Stream[F, *]].span("StreamingCloudflareApiExecutor.fetch") {
      Pagination.offsetUnfoldChunkEval[F, Int, T] { (maybePageNumber: Option[Int]) =>
        val pagedRequest = maybePageNumber.fold(req) { pageNumber =>
          req.withUri(req.uri.withQueryParam("page", pageNumber))
        }

        Trace[F].span("StreamingCloudflareApiExecutor.fetch.page") {
          for {
            _ <- Trace[F].put("maybePageNumber" -> maybePageNumber)
            pageData <- raw(pagedRequest)(responseToJson[T])
            tuple <- pageData match {
              case BaseResponseDTO(false, Some(errors), _) if errors.exists(_.code == Option(81057)) =>
                RecordAlreadyExists.raiseError
              case BaseResponseDTO(false, Some(errors), _) if errors.exists(cloudflareAuthorizationFormatError) =>
                AccessDenied(errors.find(cloudflareAuthorizationFormatError).flatMap(_.error_chain).toList.flatten).raiseError
              case single: ResponseDTO[T] if single.success =>
                (Chunk.from(single.result.toSeq), None).pure[F]
              case PagedResponseDTO(result, true, _, _, Some(result_info)) =>
                (Chunk.from(result), calculateNextPage(result_info.page, result_info.total_pages)).pure[F]
              case PagedResponseDTO(result, true, _, _, None) =>
                (Chunk.from(result), None).pure[F]
              case e =>
                val errors = e.errors.toList.flatten.map(Implicits.toError)
                val messages = e.messages.toList.flatten.map(r => com.dwolla.cloudflare.domain.model.Message(r.code, r.message, None))

                UnexpectedCloudflareErrorException(errors, messages).raiseError
            }
            (chunk, nextPage) = tuple
            _ <- Trace[F].put("nextPage" -> nextPage)
          } yield (chunk, nextPage)
        }
      }
    }

  private def addAuthorization(request: Request[F]): Request[F] =
    authorization match {
      case ApiToken(token) =>
        request.putHeaders(Authorization(Credentials.Token(AuthScheme.Bearer, token)))
      case ApiKey(email, key) =>
        request.putHeaders(
          `X-Auth-Email`(email),
          `X-Auth-Key`(key),
        )
    }

  private def setupRequest: Request[F] => Request[F] =
    addAuthorization(_).putHeaders(`Content-Type`(mediaType"application/json"))

  private def responseToJson[T: Decoder](resp: Response[F]): F[BaseResponseDTO[T]] =
    resp match {
      case Status.Successful(_) | Status.NotFound(_) | Status.BadRequest(_) =>
        resp.as[Json]
          .flatMap {
            _.asAccumulating[BaseResponseDTO[T]]
              .toEither
              .leftMap(io.circe.Errors(_))
              .liftTo[F]
          }
      case Status.Forbidden(_) => AccessDenied().raiseError
      case _ => UnexpectedCloudflareResponseStatus(resp.status).raiseError
    }

  private def calculateNextPage(currentPage: Int, totalPages: Int): Option[Int] =
    if (currentPage < totalPages) Option(currentPage + 1) else None

  private val cloudflareAuthorizationFormatError: ResponseInfoDTO => Boolean = resp => resp.code == Option(6003) && resp.error_chain.exists(_.exists(x => Set(6102, 6103).map(Option(_)).contains(x.code)))

}
