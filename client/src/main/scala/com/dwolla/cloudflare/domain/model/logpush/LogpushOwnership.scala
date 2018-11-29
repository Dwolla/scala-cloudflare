package com.dwolla.cloudflare.domain.model.logpush

case class LogpushOwnership(
  filename: String,
  message: String,
  valid: Boolean
)
