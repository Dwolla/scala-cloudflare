package com.dwolla

import cats.*
import cats.syntax.all.*
import com.dwolla.cloudflare.domain.model.Exceptions.UnexpectedCloudflareErrorException
import fs2.Stream
import org.http4s.*
import org.http4s.syntax.all.*
import org.http4s.Uri.Path.SegmentEncoder
import monix.newtypes.*

package object cloudflare {
  val BaseUrl: Uri = uri"https://api.cloudflare.com" / "client" / "v4"

  implicit class IgnoringCloudflareErrorCodes[F[_] : ApplicativeError[*[_], Throwable], T](stream: Stream[F, T]) {
    def returningEmptyOnErrorCodes(codes: Int*): Stream[F, T] = stream.recoverWith {
      case ex: UnexpectedCloudflareErrorException if ex.errors.flatMap(_.code.toSeq).exists(codes.contains) => Stream.empty
    }
  }

  implicit def segmentEncoder[A, B](implicit extractor: HasExtractor.Aux[A, B],
                                    encoder: SegmentEncoder[B]): SegmentEncoder[A] =
    encoder.contramap(extractor.extract)

}
