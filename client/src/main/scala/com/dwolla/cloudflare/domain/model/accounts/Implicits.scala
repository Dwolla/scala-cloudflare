package com.dwolla.cloudflare
package domain
package model
package accounts

import com.dwolla.cloudflare.domain.dto.accounts.*

object Implicits {
  def toModel(dto: AccountDTO): Account = {
    Account(
      id = tagAccountId(dto.id),
      name = dto.name,
      settings = toModel(dto.settings)
    )
  }

  def toModel(dto: AccountSettingsDTO): AccountSettings = {
    AccountSettings(
      enforceTwoFactor = dto.enforce_twofactor
    )
  }

  def toModel(dto: AccountMemberDTO): AccountMember = {
    AccountMember(
      id = tagAccountMemberId(dto.id),
      user = toModel(dto.user),
      status = dto.status,
      roles = dto.roles.map(toModel)
    )
  }

  def toModel(dto: UserDTO): User = {
    User(
      id = tagUserId(dto.id),
      firstName = dto.first_name,
      lastName = dto.last_name,
      emailAddress = dto.email,
      twoFactorEnabled = dto.two_factor_authentication_enabled
    )
  }

  def toModel(dto: AccountRoleDTO): AccountRole = {
    AccountRole(
      id = dto.id,
      name = dto.name,
      description = dto.description,
      permissions = dto.permissions.map(kv => (kv._1, toModel(kv._2)))
    )
  }

  def toModel(dto: AccountRolePermissionsDTO): AccountRolePermissions = {
    AccountRolePermissions(
      read = dto.read,
      edit = dto.edit
    )
  }

  def toDto(model: AccountMember): AccountMemberDTO = {
    AccountMemberDTO(
      id = model.id.value,
      user = toDto(model.user),
      status = model.status,
      roles = model.roles.map(toDto)
    )
  }

  def toDto(model: User): UserDTO = {
    UserDTO(
      id = model.id.value,
      first_name = model.firstName,
      last_name = model.lastName,
      email = model.emailAddress,
      two_factor_authentication_enabled = model.twoFactorEnabled
    )
  }

  def toDto(model: AccountRole): AccountRoleDTO = {
    AccountRoleDTO(
      id = model.id,
      name = model.name,
      description = model.description,
      permissions = model.permissions.map(kv => (kv._1, toDto(kv._2)))
    )
  }

  def toDto(model: AccountRolePermissions): AccountRolePermissionsDTO = {
    AccountRolePermissionsDTO(
      read = model.read,
      edit = model.edit
    )
  }
}
