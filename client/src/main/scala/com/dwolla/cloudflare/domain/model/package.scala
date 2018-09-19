package com.dwolla.cloudflare.domain

import shapeless.tag._

package object model {

  type ZoneId = String @@ ZoneIdTag
  type ResourceId = String @@ ResourceIdTag
  type RateLimitId = String @@ RateLimitIdTag
  type AccountId = String @@ AccountIdTag
  type AccountMemberId = String @@ AccountMemberIdTag
  type UserId = String @@ UserIdTag
  type AccessControlRuleId = String @@ AccessControlRuleIdTag
  type PhysicalResourceId = String @@ PhysicalResourceIdTag

  private[cloudflare] val tagZoneId: String ⇒ ZoneId = shapeless.tag[ZoneIdTag][String]
  private[cloudflare] val tagResourceId: String ⇒ ResourceId = shapeless.tag[ResourceIdTag][String]
  private[cloudflare] val tagRateLimitId: String ⇒ RateLimitId = shapeless.tag[RateLimitIdTag][String]
  private[cloudflare] val tagAccountId: String ⇒ AccountId = shapeless.tag[AccountIdTag][String]
  private[cloudflare] val tagAccountMemberId: String ⇒ AccountMemberId = shapeless.tag[AccountMemberIdTag][String]
  private[cloudflare] val tagUserId: String ⇒ UserId = shapeless.tag[UserIdTag][String]
  private[cloudflare] val tagAccessControlRuleId: String ⇒ AccessControlRuleId = shapeless.tag[AccessControlRuleIdTag][String]
  private[cloudflare] val tagPhysicalResourceId: String ⇒ PhysicalResourceId = shapeless.tag[PhysicalResourceIdTag][String]

}

package model {
  trait ZoneIdTag
  trait ResourceIdTag
  trait RateLimitIdTag
  trait AccountIdTag
  trait AccountMemberIdTag
  trait UserIdTag
  trait AccessControlRuleIdTag
  trait PhysicalResourceIdTag
}
