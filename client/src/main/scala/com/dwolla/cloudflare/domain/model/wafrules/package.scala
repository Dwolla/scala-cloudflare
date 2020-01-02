package com.dwolla.cloudflare.domain.model

import com.dwolla.circe._
import io.circe._
import io.circe.generic.semiauto
import shapeless.tag.@@

package object wafrules {
  type WafRuleId = String @@ WafRuleIdTag
  type WafRulePriority = String @@ WafPriorityTag

  private[cloudflare] val tagWafRuleId: String => WafRuleId = shapeless.tag[WafRuleIdTag][String]
  private[cloudflare] val tagWafRulePriority: String => WafRulePriority = shapeless.tag[WafPriorityTag][String]
}

package wafrules {
  trait WafRuleIdTag
  trait WafPriorityTag
  
  case class WafRule(id: WafRuleId,
                     description: String,
                     priority: WafRulePriority,
                     group: WafRuleGroup,
                     package_id: WafRulePackageId,
                     allowed_modes: List[Mode],
                     mode: Mode)

  case class WafRuleGroup(id: WafRuleGroupId, name: WafRuleGroupName)

  object WafRuleGroup {
    implicit val wafRuleGroupCodec: Codec[WafRuleGroup] = semiauto.deriveCodec
  }

  sealed trait Mode
  object Mode {
    case object Default extends Mode
    case object Disable extends Mode
    case object Simulate extends Mode
    case object Block extends Mode
    case object Challenge extends Mode
    case object On extends Mode
    case object Off extends Mode

    implicit val modeEncoder: Encoder[Mode] = Encoder[String].contramap {
      case Mode.Default => "default"
      case Mode.Disable => "disable"
      case Mode.Simulate => "simulate"
      case Mode.Block => "block"
      case Mode.Challenge => "challenge"
      case Mode.On => "on"
      case Mode.Off => "off"
    }

    implicit val modeDecoder: Decoder[Mode] = Decoder[String].map {
      case "default" => Mode.Default
      case "disable" => Mode.Disable
      case "simulate" => Mode.Simulate
      case "block" => Mode.Block
      case "challenge" => Mode.Challenge
      case "on" => Mode.On
      case "off" => Mode.Off
    }
  }

  object WafRule {
    implicit val wafRuleCodec: Codec[WafRule] = semiauto.deriveCodec
  }
}
