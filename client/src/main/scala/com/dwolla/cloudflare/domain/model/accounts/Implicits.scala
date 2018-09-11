package com.dwolla.cloudflare
package domain
package model
package accounts

import com.dwolla.cloudflare.domain.dto.accounts._

object Implicits {
  implicit def toModel(dto: AccountDTO): Account = {
    Account(
      id = tagAccountId(dto.id),
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
      id = tagAccountMemberId(dto.id),
      user = dto.user,
      status = dto.status,
      roles = dto.roles.map(toModel)
    )
  }

  implicit def toModel(dto: UserDTO): User = {
    User(
      id = tagUserId(dto.id),
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

  implicit def toDto(model: AccountMember): AccountMemberDTO = {
    AccountMemberDTO(
      id = model.id,
      user = model.user,
      status = model.status,
      roles = model.roles.map(toDto)
    )
  }

  implicit def toDto(model: User): UserDTO = {
    UserDTO(
      id = model.id,
      first_name = model.firstName,
      last_name = model.lastName,
      email = model.emailAddress,
      two_factor_authentication_enabled = model.twoFactorEnabled
    )
  }

  implicit def toDto(model: AccountRole): AccountRoleDTO = {
    AccountRoleDTO(
      id = model.id,
      name = model.name,
      description = model.description,
      permissions = model.permissions.map(kv ⇒ (kv._1, toDto(kv._2)))
    )
  }

  implicit def toDto(model: AccountRolePermissions): AccountRolePermissionsDTO = {
    AccountRolePermissionsDTO(
      read = model.read,
      edit = model.edit
    )
  }
}
