package com.dwolla.cloudflare.domain.dto.accounts

import com.dwolla.cloudflare.domain.dto.JsonWritable

case class NewAccountMemberDTO (
  email: String,
  roles: Seq[String],
  status: Option[String] = None
) extends JsonWritable
