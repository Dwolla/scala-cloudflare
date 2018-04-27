package com.dwolla.cloudflare.domain.model.accounts

case class User (
  id: String,
  firstName: String,
  lastName: String,
  emailAddress: String,
  twoFactorEnabled: Boolean
)
