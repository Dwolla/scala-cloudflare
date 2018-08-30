package com.dwolla.cloudflare.domain

import shapeless.tag._

package object model {

  type ZoneId = String @@ ZoneIdTag
  type RateLimitId = String @@ RateLimitIdTag
  type AccountId = String @@ AccountIdTag

  private[cloudflare] val tagZoneId: String ⇒ ZoneId = shapeless.tag[ZoneIdTag][String]
  private[cloudflare] val tagRateLimitId: String ⇒ RateLimitId = shapeless.tag[RateLimitIdTag][String]
  private[cloudflare] val tagAccountId: String ⇒ AccountId = shapeless.tag[AccountIdTag][String]

}

package model {
  trait ZoneIdTag
  trait RateLimitIdTag
  trait AccountIdTag
}
