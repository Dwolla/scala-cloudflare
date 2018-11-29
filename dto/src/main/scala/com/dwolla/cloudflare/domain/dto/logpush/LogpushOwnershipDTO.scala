package com.dwolla.cloudflare.domain.dto.logpush

case class LogpushOwnershipDTO(
  filename: String,
  message: String,
  valid: Boolean
)

case class CreateOwnershipDTO(destination_conf: String)
