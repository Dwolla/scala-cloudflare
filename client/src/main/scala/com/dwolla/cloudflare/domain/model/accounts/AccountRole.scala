package com.dwolla.cloudflare.domain.model.accounts

case class AccountRole (
  id: String,
  name: String,
  description: String,
  permissions: Map[String, AccountRolePermissions]
)

case class AccountRolePermissions (
  read: Boolean,
  edit: Boolean
)
