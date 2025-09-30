package com.dwolla.cloudflare.domain.model

import java.time.Instant

import com.dwolla.circe._
import io.circe._
import io.circe.generic.semiauto
import io.circe.literal._
import com.dwolla.cloudflare.CloudflareNewtype

package object firewallrules {

  type FirewallRuleId = FirewallRuleId.Type
  object FirewallRuleId extends CloudflareNewtype[String]
  type FirewallRulePriority = FirewallRulePriority.Type
  object FirewallRulePriority extends CloudflareNewtype[Int]
  type FirewallRuleRef = FirewallRuleRef.Type
  object FirewallRuleRef extends CloudflareNewtype[String]

  private[cloudflare] val tagFirewallRuleId: String => FirewallRuleId = FirewallRuleId(_)
  private[cloudflare] val tagFirewallRulePriority: Int => FirewallRulePriority = FirewallRulePriority(_)
  private[cloudflare] val tagFirewallRuleRef: String => FirewallRuleRef = FirewallRuleRef(_)
}

//TODO: Can you add support for dependencies withing dwolla-cloudformation. For filter -> rule relationship.
package firewallrules {
  import com.dwolla.cloudflare.domain.model.filters._

  case class FirewallRule(id: Option[FirewallRuleId] = None,
                          filter: FirewallRuleFilter,
                          action: Action,
                          products: List[Product] = List.empty,
                          priority: FirewallRulePriority,
                          paused: Boolean,
                          description: Option[String] = None,
                          ref: Option[FirewallRuleRef] = None,
                          created_on: Option[Instant] = None,
                          modified_on: Option[Instant] = None)

  object FirewallRule extends DurationAsSecondsCodec with NullAsEmptyListCodec with StringAsBooleanCodec {

    // Cloudflare throws an error if the products key is present and the 'bypass'
    // action is not specified. As a result we, filter out the products key when its value is
    // empty. In the long-term, it would probably be best to bake this constraint
    // into the type.
    implicit val firewallRuleEncoder: Encoder[FirewallRule] = semiauto.deriveEncoder[FirewallRule].mapJsonObject(
      _.filter {
        case ("products", value) => value != json"[]"
        case _ => true
      }
    )
    implicit val firewallRuleDecoder: Decoder[FirewallRule] = semiauto.deriveDecoder
  }

  case class FirewallRuleFilter(id: Option[FilterId] = None,
                                expression: Option[FilterExpression],
                                paused: Option[Boolean],
                                description: Option[String] = None,
                                ref: Option[FilterRef] = None)

  object FirewallRuleFilter extends StringAsBooleanCodec {
    implicit val firewallRuleFilterCodec: Codec[FirewallRuleFilter] = semiauto.deriveCodec
  }

  sealed trait Action
  object Action {
    case object Block extends Action
    case object Challenge extends Action
    case object JsChallenge extends Action
    case object Allow extends Action
    case object Log extends Action
    case object Bypass extends Action

    implicit val actionEncoder: Encoder[Action] = Encoder[String].contramap {
      case Action.Block => "block"
      case Action.Challenge => "challenge"
      case Action.JsChallenge => "js_challenge"
      case Action.Allow => "allow"
      case Action.Log => "log"
      case Action.Bypass => "bypass"
    }

    implicit val actionDecoder: Decoder[Action] = Decoder[String].map {
      case "block" => Action.Block
      case "challenge" => Action.Challenge
      case "js_challenge" => Action.JsChallenge
      case "allow" => Action.Allow
      case "log" => Action.Log
      case "bypass" => Action.Bypass
    }
  }

  sealed trait Product
  object Product {
    case object ZoneLockdown extends Product
    case object UaBlock extends Product
    case object Bic extends Product
    case object Hot extends Product
    case object SecurityLevel extends Product
    case object RateLimit extends Product
    case object Waf extends Product

    implicit val productEncoder: Encoder[Product] = Encoder[String].contramap {
      case Product.ZoneLockdown => "zoneLockdown"
      case Product.UaBlock => "uaBlock"
      case Product.Bic => "bic"
      case Product.Hot => "hot"
      case Product.SecurityLevel => "securityLevel"
      case Product.RateLimit => "rateLimit"
      case Product.Waf => "waf"
    }

    implicit val productDecoder: Decoder[Product] = Decoder[String].map {
      case "zoneLockdown" => Product.ZoneLockdown
      case "uaBlock" => Product.UaBlock
      case "bic" => Product.Bic
      case "hot" => Product.Hot
      case "securityLevel" => Product.SecurityLevel
      case "rateLimit" => Product.RateLimit
      case "waf" => Product.Waf
    }
  }
}
