package com.dwolla.cloudflare.domain.model.accounts

import com.dwolla.cloudflare.domain.dto.accounts._

private[cloudflare] object Implicits {
  implicit def toModel(dto: AccountDTO): Account = {
    Account(
      id = dto.id,
      name = dto.name,
      settings = toModel(dto.settings)
    )
  }

  implicit def toModel(dto: AccountSettingsDTO): AccountSettings = {
    AccountSettings(
      enforceTwoFactor = dto.enforce_twofactor
    )
  }

  implicit def toModel(dto: AccountMemberDTO): AccountMember = {
    AccountMember(
      id = dto.id,
      user = toModel(dto.user),
      status = dto.status,
      roles = dto.roles.map(toModel)
    )
  }

  implicit def toModel(dto: UserDTO): User = {
    User(
      id = dto.id,
      firstName = dto.first_name,
      lastName = dto.last_name,
      emailAddress = dto.email,
      twoFactorEnabled = dto.two_factor_authentication_enabled
    )
  }

  implicit def toModel(dto: AccountRoleDTO): AccountRole = {
    AccountRole(
      id = dto.id,
      name = dto.name,
      description = dto.description,
      permissions = dto.permissions.map(kv ⇒ (kv._1, toModel(kv._2)))
    )
  }

  implicit def toModel(dto: AccountRolePermissionsDTO): AccountRolePermissions = {
    AccountRolePermissions(
      read = dto.read,
      edit = dto.edit
    )
  }
}
