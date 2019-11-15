package com.dwolla.cloudflare.domain.model.accesscontrolrules

import io.circe._
import io.circe.generic.semiauto._

case class RuleScope(id: String,
                     name: Option[String],
                     `type`: String)

object RuleScope {
  implicit val ruleScopeEncoder: Encoder[RuleScope] = deriveEncoder
  implicit val ruleScopeDecoder: Decoder[RuleScope] = deriveDecoder
}
