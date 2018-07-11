package com.dwolla.cloudflare.domain.model.accounts

import com.dwolla.cloudflare.domain.dto.PagedResponseDTO
import com.dwolla.cloudflare.domain.dto.accounts._
import com.dwolla.cloudflare.domain.model.response.PagedResponse

object Implicits {
  import com.dwolla.cloudflare.domain.model.response.Implicits._

  implicit def toPagedAccountModel(dto: PagedResponseDTO[Set[AccountDTO]]): PagedResponse[Set[Account]] = {
    PagedResponse(
      result = dto.result.map(toModel),
      paging = dto.result_info
    )
  }

  implicit def toPagedAccountRoleModel(dto: PagedResponseDTO[Set[AccountRoleDTO]]): PagedResponse[Set[AccountRole]] = {
    PagedResponse(
      result = dto.result.map(toModel),
      paging = dto.result_info
    )
  }

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
      user = dto.user,
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

  implicit def toDTO(model: AccountMember): AccountMemberDTO = {
    AccountMemberDTO(
      id = model.id,
      user = model.user,
      status = model.status,
      roles = model.roles.map(toDTO)
    )
  }

  implicit def toDTO(model: User): UserDTO = {
    UserDTO(
      id = model.id,
      first_name = model.firstName,
      last_name = model.lastName,
      email = model.emailAddress,
      two_factor_authentication_enabled = model.twoFactorEnabled
    )
  }

  implicit def toDTO(model: AccountRole): AccountRoleDTO = {
    AccountRoleDTO(
      id = model.id,
      name = model.name,
      description = model.description,
      permissions = model.permissions.map(kv ⇒ (kv._1, toDTO(kv._2)))
    )
  }

  implicit def toDTO(model: AccountRolePermissions): AccountRolePermissionsDTO = {
    AccountRolePermissionsDTO(
      read = model.read,
      edit = model.edit
    )
  }
}
