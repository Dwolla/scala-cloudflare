package com.dwolla.cloudflare

import cats.*
import cats.syntax.all.*
import com.dwolla.cloudflare.domain.model.Exceptions.UnexpectedCloudflareErrorException
import fs2.Stream
import org.http4s.*
import org.http4s.syntax.all.*
import org.http4s.Uri.Path.SegmentEncoder
import monix.newtypes.*

val BaseUrl: Uri = uri"https://api.cloudflare.com" / "client" / "v4"

extension [F[_] : ApplicativeThrow, T](stream: Stream[F, T])
  def returningEmptyOnErrorCodes(codes: Int*): Stream[F, T] =
    stream.recoverWith:
      case ex: UnexpectedCloudflareErrorException if ex.errors.flatMap(_.code.toSeq).exists(codes.contains) => Stream.empty

  def orIfEmpty(fallback: Stream[F, T]): Stream[F, T] =
    stream
      .pull
      .peek1
      .flatMap:
        case Some((_, rest)) => rest.pull.echo
        case None => fallback.pull.echo
      .stream

given [A, B](using HE: HasExtractor.Aux[A, B])
            (using SegmentEncoder[B]): SegmentEncoder[A] =
  SegmentEncoder[B].contramap(HE.extract)
