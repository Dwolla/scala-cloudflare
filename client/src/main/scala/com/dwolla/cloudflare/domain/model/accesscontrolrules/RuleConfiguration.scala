package com.dwolla.cloudflare.domain.model.accesscontrolrules

import io.circe._
import io.circe.generic.semiauto._

case class RuleConfiguration(target: String,
                             value: String)

object RuleConfiguration {
  implicit val ruleConfigurationEncoder: Encoder[RuleConfiguration] = deriveEncoder
  implicit val ruleConfigurationDecoder: Decoder[RuleConfiguration] = deriveDecoder
}
