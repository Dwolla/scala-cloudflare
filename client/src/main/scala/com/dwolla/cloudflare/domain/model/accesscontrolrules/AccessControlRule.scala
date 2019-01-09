package com.dwolla.cloudflare.domain.model.accesscontrolrules

import com.dwolla.cloudflare.domain.dto.ResponseInfoDTO
import com.dwolla.cloudflare.domain.model.AccessControlRuleId

case class Rule(id: AccessControlRuleId,
                notes: Option[String],
                allowedModes: List[String],
                mode: Option[String],
                configuration: RuleConfiguration,
                createdOn: Option[String],
                modifiedOn: Option[String],
                scope: RuleScope)

case class CreateRule(mode: Option[String],
                      configuration: RuleConfiguration,
                      notes: Option[String] = None)

case class DeletedRule(success: Boolean,
                      errors: Option[Seq[ResponseInfoDTO]],
                      messages: Option[Seq[ResponseInfoDTO]],
                      id: Option[AccessControlRuleId]
                      )