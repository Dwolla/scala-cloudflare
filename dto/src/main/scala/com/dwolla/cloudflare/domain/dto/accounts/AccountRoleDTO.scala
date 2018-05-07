package com.dwolla.cloudflare.domain.dto.accounts

case class AccountRoleDTO (
  id: String,
  name: String,
  description: String,
  permissions: Map[String, AccountRolePermissionsDTO]
)

case class AccountRolePermissionsDTO (
  read: Boolean,
  edit: Boolean
)