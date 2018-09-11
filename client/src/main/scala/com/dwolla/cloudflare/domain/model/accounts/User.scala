package com.dwolla.cloudflare.domain.model.accounts

import com.dwolla.cloudflare.domain.model._

case class User(id: UserId,
                firstName: Option[String],
                lastName: Option[String],
                emailAddress: String,
                twoFactorEnabled: Boolean
               )
