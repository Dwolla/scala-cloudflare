package com.dwolla.cloudflare.domain.model.accounts

case class Account (
  id: String,
  name: String,
  settings: AccountSettings
)
