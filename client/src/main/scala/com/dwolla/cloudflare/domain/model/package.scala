package com.dwolla.cloudflare.domain

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

  private[cloudflare] val tagZoneId: String => ZoneId = shapeless.tag[ZoneIdTag][String]
  private[cloudflare] val tagResourceId: String => ResourceId = shapeless.tag[ResourceIdTag][String]
  private[cloudflare] val tagAccountId: String => AccountId = shapeless.tag[AccountIdTag][String]
  private[cloudflare] val tagAccountMemberId: String => AccountMemberId = shapeless.tag[AccountMemberIdTag][String]
  private[cloudflare] val tagUserId: String => UserId = shapeless.tag[UserIdTag][String]
  private[cloudflare] val tagPhysicalResourceId: String => PhysicalResourceId = shapeless.tag[PhysicalResourceIdTag][String]
  private[cloudflare] val tagLogpushId: Int => LogpushId = shapeless.tag[LogpushIdTag][Int]
  private[cloudflare] val tagLogpushDestination: String => LogpushDestination = shapeless.tag[LogpushDestinationTag][String]
  private[cloudflare] val tagLogpullOptions: String => LogpullOptions = shapeless.tag[LogpullOptionsTag][String]

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
}
