package com.dwolla.cloudflare.domain.model

import com.dwolla.circe._
import io.circe._
import io.circe.generic.semiauto
import com.dwolla.cloudflare.CloudflareNewtype

package object filters {
  type FilterId = FilterId.Type
  object FilterId extends CloudflareNewtype[String]
  type FilterExpression = FilterExpression.Type
  object FilterExpression extends CloudflareNewtype[String]
  type FilterRef = FilterRef.Type
  object FilterRef extends CloudflareNewtype[String]

  private[cloudflare] val tagFilterId: String => FilterId = FilterId(_)
  private[cloudflare] val tagFilterExpression: String => FilterExpression = FilterExpression(_)
  private[cloudflare] val tagFilterRef: String => FilterRef = FilterRef(_)
}


package filters {

  case class Filter(id: Option[FilterId] = None,
                    expression: FilterExpression,
                    paused: Boolean,
                    description: Option[String] = None,
                    ref: Option[FilterRef] = None)

  object Filter extends StringAsBooleanCodec {
    implicit val FilterCodec: Codec[Filter] = semiauto.deriveCodec
  }
}
