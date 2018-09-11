package com.dwolla.cloudflare.domain.model.accounts

import com.dwolla.cloudflare.domain.model.AccountId

case class Account(id: AccountId,
                   name: String,
                   settings: AccountSettings
                  )
