package com.dwolla

import cats._
import cats.implicits._
import com.dwolla.cloudflare.domain.model.Exceptions.UnexpectedCloudflareErrorException
import fs2.Stream
import org.http4s._
import org.http4s.syntax.all._

package object cloudflare {
  val BaseUrl: Uri = uri"https://api.cloudflare.com" / "client" / "v4"

  implicit class IgnoringCloudflareErrorCodes[F[_] : ApplicativeError[*[_], Throwable], T](stream: Stream[F, T]) {
    def returningEmptyOnErrorCodes(codes: Int*): Stream[F, T] = stream.recoverWith {
      case ex: UnexpectedCloudflareErrorException if ex.errors.flatMap(_.code.toSeq).exists(codes.contains) => Stream.empty
    }
  }

}
