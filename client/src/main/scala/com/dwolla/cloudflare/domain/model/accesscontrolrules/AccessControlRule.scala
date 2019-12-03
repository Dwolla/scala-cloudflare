package com.dwolla.cloudflare.domain.model.accesscontrolrules

import com.dwolla.cloudflare.domain.model.AccessControlRuleId
import io.circe._
import io.circe.generic.semiauto._
import com.dwolla.circe._

case class Rule(id: AccessControlRuleId,
                notes: Option[String],
                allowedModes: List[String],
                mode: Option[String],
                configuration: RuleConfiguration,
                createdOn: Option[String],
                modifiedOn: Option[String],
                scope: RuleScope)

object Rule {
  implicit val ruleCodec: Codec[Rule] = deriveCodec
}

case class CreateRule(mode: Option[String],
                      configuration: RuleConfiguration,
                      notes: Option[String] = None)

object CreateRule {
  implicit val createRuleCodec: Codec[CreateRule] = deriveCodec
}