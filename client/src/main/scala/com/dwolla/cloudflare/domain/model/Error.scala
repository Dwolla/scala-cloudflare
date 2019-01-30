package com.dwolla.cloudflare.domain.model

private[cloudflare] case class Error(code: Option[Int], message: String)
private[cloudflare] case class Message(code: Option[Int], message: String, `type`: Option[String])
