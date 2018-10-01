package com.dwolla.cloudflare.domain.dto.accesscontrolrules

case class AccessControlRuleDTO(id: String,
                                notes: Option[String],
                                allowed_modes: List[String],
                                mode: Option[String],
                                configuration: AccessControlRuleConfigurationDTO,
                                created_on: Option[String],
                                modified_on: Option[String],
                                scope: AccessControlRuleScopeDTO)

case class AccessControlRuleConfigurationDTO(target: String,
                                             value: String)

case class AccessControlRuleScopeDTO(id: String,
                                     name: Option[String],
                                     `type`: String)
