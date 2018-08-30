package com.dwolla.cloudflare.domain.model.accesscontrolrules

import com.dwolla.cloudflare.domain.dto.accesscontrolrules._

private[cloudflare] object Implicits {
  implicit def toModel(dto: AccessControlRuleDTO): Rule = {
    Rule(
      id = dto.id,
      notes = dto.notes,
      allowedModes = dto.allowed_modes,
      mode = dto.mode,
      configuration = dto.configuration,
      createdOn = dto.created_on,
      modifiedOn = dto.modified_on,
      scope = dto.scope
    )
  }

  implicit def toModel(dto: AccessControlRuleConfigurationDTO): RuleConfiguration = {
    RuleConfiguration(
      target = dto.target,
      value = dto.value
    )
  }

  implicit def toModel(dto: AccessControlRuleScopeDTO): RuleScope = {
    RuleScope(
      id = dto.id,
      name = dto.name,
      `type` = dto.`type`
    )
  }
}
