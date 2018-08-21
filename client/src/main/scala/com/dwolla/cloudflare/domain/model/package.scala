package com.dwolla.cloudflare.domain

import shapeless.tag._

package object model {

  type ZoneId = String @@ ZoneIdTag

  private[cloudflare] val tagZoneId: String ⇒ ZoneId = shapeless.tag[ZoneIdTag][String]

}

package model {
  trait ZoneIdTag
}
