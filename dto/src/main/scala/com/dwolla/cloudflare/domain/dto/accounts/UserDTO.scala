package com.dwolla.cloudflare.domain.dto.accounts

import com.dwolla.cloudflare.domain.dto.JsonWritable

case class UserDTO (
  id: String,
  first_name: String,
  last_name: String,
  email: String,
  two_factor_authentication_enabled: Boolean
) extends JsonWritable
