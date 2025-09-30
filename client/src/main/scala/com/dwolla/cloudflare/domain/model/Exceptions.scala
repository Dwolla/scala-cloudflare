package com.dwolla.cloudflare.domain.model

import com.dwolla.cloudflare.domain.dto.ResponseInfoDTO
import org.http4s.Status

object Exceptions {

  private def unexpectedCloudflareErrorExceptionMessage(errors: List[Error], messages: List[Message]): String = {
    val base =
      s"""An unexpected Cloudflare error occurred. Errors:
         |
         | - ${errors.mkString("\n - ")}
         |     """.stripMargin

    if (messages.nonEmpty)
      base ++
        s"""
           | Messages:
           |
           | - ${messages.mkString("\n - ")}
           |""".stripMargin
    else base
  }

  case class UnexpectedCloudflareErrorException(errors: List[Error], messages: List[Message] = List.empty) extends RuntimeException(unexpectedCloudflareErrorExceptionMessage(errors, messages))

  case class UnexpectedCloudflareResponseStatus(status: Status) extends RuntimeException(s"Received $status response from Cloudflare, but don't know how to handle it")

  case object RecordAlreadyExists extends RuntimeException("Cloudflare already has a matching record, and refuses to create a new one.")

  case class AccessDenied(errorChain: List[ResponseInfoDTO] = List.empty)
    extends RuntimeException(s"The given credentials were invalid${
      if (errorChain.nonEmpty)
        errorChain
          .map(ResponseInfoDTO.unapply)
          .map(e => s"   - $e")
          .mkString("\n\n  See the following errors:\n", "\n", "\n")
      else ""
    }")
}
