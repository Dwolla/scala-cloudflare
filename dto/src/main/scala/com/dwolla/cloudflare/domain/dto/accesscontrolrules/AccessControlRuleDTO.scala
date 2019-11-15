package com.dwolla.cloudflare.domain.dto.accesscontrolrules

import io.circe.{Encoder, Decoder}
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
  implicit val accessControlRuleDTOEncoder: Encoder[AccessControlRuleDTO] = deriveEncoder
  implicit val accessControlRuleDTODecoder: Decoder[AccessControlRuleDTO] = deriveDecoder
}

case class AccessControlRuleConfigurationDTO(target: String,
                                             value: String)

object AccessControlRuleConfigurationDTO {
  implicit val accessControlRuleConfigurationDTOEncoder: Encoder[AccessControlRuleConfigurationDTO] = deriveEncoder
  implicit val accessControlRuleConfigurationDTODecoder: Decoder[AccessControlRuleConfigurationDTO] = deriveDecoder
}

case class AccessControlRuleScopeDTO(id: String,
                                     name: Option[String],
                                     `type`: String)

object AccessControlRuleScopeDTO {
  implicit val accessControlRuleScopeDTOEncoder: Encoder[AccessControlRuleScopeDTO] = deriveEncoder
  implicit val accessControlRuleScopeDTODecoder: Decoder[AccessControlRuleScopeDTO] = deriveDecoder
}
