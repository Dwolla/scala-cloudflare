package com.dwolla.cloudflare.domain.model.accounts

case class User (
  id: String,
  firstName: Option[String],
  lastName: Option[String],
  emailAddress: String,
  twoFactorEnabled: Boolean
)
