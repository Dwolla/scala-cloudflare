package com.dwolla.cloudflare.domain.dto.accounts

import com.dwolla.cloudflare.domain.dto.JsonWritable

case class AccountRoleDTO (
  id: String,
  name: String,
  description: String,
  permissions: Map[String, AccountRolePermissionsDTO]
) extends JsonWritable

case class AccountRolePermissionsDTO (
  read: Boolean,
  edit: Boolean
) extends JsonWritable