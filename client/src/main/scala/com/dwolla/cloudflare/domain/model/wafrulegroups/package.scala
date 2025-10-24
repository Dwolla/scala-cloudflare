package com.dwolla.cloudflare.domain.model

import io.circe.*
import io.circe.generic.semiauto

package wafrulegroups {
  trait WafRuleGroupNameTag

  case class WafRuleGroup(id: WafRuleGroupId,
                          name: WafRuleGroupName,
                          description: String,
                          rules_count: Int,
                          modified_rules_count: Int,
                          package_id: WafRulePackageId,
                          mode: Mode)

  sealed trait Mode
  object Mode {
    case object On extends Mode
    case object Off extends Mode

    implicit val modeEncoder: Encoder[Mode] = Encoder[String].contramap {
      case Mode.On => "on"
      case Mode.Off => "off"
    }

    implicit val modeDecoder: Decoder[Mode] = Decoder[String].map {
      case "on" => Mode.On
      case "off" => Mode.Off
    }
  }

  object WafRuleGroup {
    implicit val wafRuleGroupCodec: Codec[WafRuleGroup] = semiauto.deriveCodec
  }
}
