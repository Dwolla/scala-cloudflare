package com.dwolla.cloudflare.domain.dto.accounts

case class AccountDTO (
  id: String,
  name: String,
  settings: AccountSettingsDTO
)
