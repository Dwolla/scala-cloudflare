package com.dwolla.cloudflare.domain.model

import org.http4s.Status

object Exceptions {

  case class UnexpectedCloudflareErrorException(errors: List[Error]) extends RuntimeException(
    s"""An unexpected Cloudflare error occurred. Errors:
        |
        | - ${errors.mkString("\n - ")}
     """.stripMargin
  )

  case class UnexpectedCloudflareResponseStatus(status: Status) extends RuntimeException(s"Received $status response from Cloudflare, but don't know how to handle it")

  case object RecordAlreadyExists extends RuntimeException("Cloudflare already has a matching record, and refuses to create a new one.")
}
