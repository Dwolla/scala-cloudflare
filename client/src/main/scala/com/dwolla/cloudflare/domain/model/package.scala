package com.dwolla.cloudflare.domain

import io.circe.*
import org.http4s.*
import com.dwolla.cloudflare.CloudflareNewtype

package object model {

  type ZoneId = ZoneId.Type
  object ZoneId extends CloudflareNewtype[String]
  type ResourceId = ResourceId.Type
  object ResourceId extends CloudflareNewtype[String]
  type AccountId = AccountId.Type
  object AccountId extends CloudflareNewtype[String]
  type AccountMemberId = AccountMemberId.Type
  object AccountMemberId extends CloudflareNewtype[String]
  type UserId = UserId.Type
  object UserId extends CloudflareNewtype[String]
  type PhysicalResourceId = PhysicalResourceId.Type
  object PhysicalResourceId extends CloudflareNewtype[String]
  type LogpushId = LogpushId.Type
  object LogpushId extends CloudflareNewtype[Int]
  type LogpushDestination = LogpushDestination.Type
  object LogpushDestination extends CloudflareNewtype[String]
  type LogpullOptions = LogpullOptions.Type
  object LogpullOptions extends CloudflareNewtype[String]
  type WafRulePackageId = WafRulePackageId.Type
  object WafRulePackageId extends CloudflareNewtype[String]
  type WafRuleGroupId = WafRuleGroupId.Type
  object WafRuleGroupId extends CloudflareNewtype[String]

  type WafRuleGroupName = WafRuleGroupName.Type
  object WafRuleGroupName extends CloudflareNewtype[String] {
    implicit val queryParam: QueryParam[WafRuleGroupName] = new QueryParam[WafRuleGroupName] {
      override def key: QueryParameterKey = QueryParameterKey("name")
    }
    implicit val queryParamEncoder: QueryParamEncoder[WafRuleGroupName] = value => QueryParameterValue(value.value)
    implicit val jsonEncoder: Encoder[WafRuleGroupName] = Encoder[String].contramap(_.value)
    implicit val jsonDecoder: Decoder[WafRuleGroupName] = Decoder[String].map(WafRuleGroupName(_))
  }

  private[cloudflare] val tagZoneId: String => ZoneId = ZoneId(_)
  private[cloudflare] val tagResourceId: String => ResourceId = ResourceId(_)
  private[cloudflare] val tagAccountId: String => AccountId = AccountId(_)
  private[cloudflare] val tagAccountMemberId: String => AccountMemberId = AccountMemberId(_)
  private[cloudflare] val tagUserId: String => UserId = UserId(_)
  private[cloudflare] val tagPhysicalResourceId: String => PhysicalResourceId = PhysicalResourceId(_)
  private[cloudflare] val tagLogpushId: Int => LogpushId = LogpushId(_)
  private[cloudflare] val tagLogpushDestination: String => LogpushDestination = LogpushDestination(_)
  private[cloudflare] val tagLogpullOptions: String => LogpullOptions = LogpullOptions(_)
  private[cloudflare] val tagWafRulePackageId: String => WafRulePackageId = WafRulePackageId(_)
  private[cloudflare] val tagWafRuleGroupId: String => WafRuleGroupId = WafRuleGroupId(_)
}
