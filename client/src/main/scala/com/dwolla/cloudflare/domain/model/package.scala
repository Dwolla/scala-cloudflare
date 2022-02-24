package com.dwolla.cloudflare.domain

import io.circe._
import monix.newtypes.NewtypeWrapped
import org.http4s._
import shapeless.tag._

package object model {

  type ZoneId = String @@ ZoneIdTag
  type ResourceId = String @@ ResourceIdTag
  type AccountId = String @@ AccountIdTag
  type AccountMemberId = String @@ AccountMemberIdTag
  type UserId = String @@ UserIdTag
  type PhysicalResourceId = String @@ PhysicalResourceIdTag
  type LogpushId = Int @@ LogpushIdTag
  type LogpushDestination = String @@ LogpushDestinationTag
  type LogpullOptions = String @@ LogpullOptionsTag
  type WafRulePackageId = String @@ WafRulePackageIdTag
  type WafRuleGroupId = String @@ WafRuleGroupIdTag

  type WafRuleGroupName = WafRuleGroupName.Type
  object WafRuleGroupName extends NewtypeWrapped[String] {
    implicit val queryParam: QueryParam[WafRuleGroupName] = new QueryParam[WafRuleGroupName] {
      override def key: QueryParameterKey = QueryParameterKey("name")
    }
    implicit val queryParamEncoder: QueryParamEncoder[WafRuleGroupName] = value => QueryParameterValue(value.value)
    implicit val jsonEncoder: Encoder[WafRuleGroupName] = Encoder[String].contramap(_.value)
    implicit val jsonDecoder: Decoder[WafRuleGroupName] = Decoder[String].map(WafRuleGroupName(_))
  }

  private[cloudflare] val tagZoneId: String => ZoneId = shapeless.tag[ZoneIdTag][String]
  private[cloudflare] val tagResourceId: String => ResourceId = shapeless.tag[ResourceIdTag][String]
  private[cloudflare] val tagAccountId: String => AccountId = shapeless.tag[AccountIdTag][String]
  private[cloudflare] val tagAccountMemberId: String => AccountMemberId = shapeless.tag[AccountMemberIdTag][String]
  private[cloudflare] val tagUserId: String => UserId = shapeless.tag[UserIdTag][String]
  private[cloudflare] val tagPhysicalResourceId: String => PhysicalResourceId = shapeless.tag[PhysicalResourceIdTag][String]
  private[cloudflare] val tagLogpushId: Int => LogpushId = shapeless.tag[LogpushIdTag][Int]
  private[cloudflare] val tagLogpushDestination: String => LogpushDestination = shapeless.tag[LogpushDestinationTag][String]
  private[cloudflare] val tagLogpullOptions: String => LogpullOptions = shapeless.tag[LogpullOptionsTag][String]
  private[cloudflare] val tagWafRulePackageId: String => WafRulePackageId = shapeless.tag[WafRulePackageIdTag][String]
  private[cloudflare] val tagWafRuleGroupId: String => WafRuleGroupId = shapeless.tag[WafRuleGroupIdTag][String]
}

package model {
  trait ZoneIdTag
  trait ResourceIdTag
  trait AccountIdTag
  trait AccountMemberIdTag
  trait UserIdTag
  trait PhysicalResourceIdTag
  trait LogpushIdTag
  trait LogpushDestinationTag
  trait LogpullOptionsTag
  trait WafRulePackageIdTag
  trait WafRuleGroupIdTag
}
