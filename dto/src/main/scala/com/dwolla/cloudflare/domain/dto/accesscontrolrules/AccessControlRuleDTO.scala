package com.dwolla.cloudflare.domain.dto.accesscontrolrules

import io.circe.Codec
import io.circe.generic.semiauto._

case class AccessControlRuleDTO(id: String,
                                notes: Option[String],
                                allowed_modes: List[String],
                                mode: Option[String],
                                configuration: AccessControlRuleConfigurationDTO,
                                created_on: Option[String],
                                modified_on: Option[String],
                                scope: AccessControlRuleScopeDTO)

object AccessControlRuleDTO {
  implicit val accessControlRuleDTOCodec: Codec[AccessControlRuleDTO] = deriveCodec
}

case class AccessControlRuleConfigurationDTO(target: String,
                                             value: String)

object AccessControlRuleConfigurationDTO {
  implicit val accessControlRuleConfigurationDTOCodec: Codec[AccessControlRuleConfigurationDTO] = deriveCodec
}

case class AccessControlRuleScopeDTO(id: String,
                                     name: Option[String],
                                     `type`: String)

object AccessControlRuleScopeDTO {
  implicit val accessControlRuleScopeDTOCodec: Codec[AccessControlRuleScopeDTO] = deriveCodec
}
