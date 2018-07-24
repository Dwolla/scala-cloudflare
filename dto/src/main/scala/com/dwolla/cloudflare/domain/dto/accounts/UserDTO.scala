package com.dwolla.cloudflare.domain.dto.accounts

import com.dwolla.cloudflare.domain.dto.JsonWritable

case class UserDTO (
  id: String,
  first_name: Option[String],
  last_name: Option[String],
  email: String,
  two_factor_authentication_enabled: Boolean
) extends JsonWritable
