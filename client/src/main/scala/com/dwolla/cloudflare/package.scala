package com.dwolla

import com.dwolla.cloudflare.domain.model.Exceptions.UnexpectedCloudflareErrorException
import cats.implicits._
import fs2.Stream
import org.http4s.Uri

package object cloudflare {
  val BaseUrl: Uri = Uri.uri("https://api.cloudflare.com") / "client" / "v4"

  implicit class IgnoringCloudflareErrorCodes[F[_], T](stream: Stream[F, T]) {
    def returningEmptyOnErrorCodes(codes: Int*): Stream[F, T] = stream.recoverWith {
      case ex: UnexpectedCloudflareErrorException if ex.errors.flatMap(_.code.toSeq).exists(codes.contains) => Stream.empty
    }
  }

}
