package com.dwolla.cloudflare.domain.model

object Exceptions {

  case class UnexpectedCloudflareErrorException(errors: List[Error]) extends RuntimeException(
    s"""An unexpected Cloudflare error occurred. Errors:
        |
        | - ${errors.mkString("\n - ")}
     """.stripMargin
  )
}