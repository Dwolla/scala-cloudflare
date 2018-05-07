package com.dwolla.cloudflare.domain.dto.accounts

case class UserDTO (
  id: String,
  first_name: String,
  last_name: String,
  email: String,
  two_factor_authentication_enabled: Boolean
)
