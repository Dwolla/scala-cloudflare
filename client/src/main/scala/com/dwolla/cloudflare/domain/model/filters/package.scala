package com.dwolla.cloudflare.domain.model

import com.dwolla.circe._
import io.circe._
import io.circe.generic.semiauto
import shapeless.tag.@@

package object filters {
  type FilterId = String @@ FilterIdTag
  type FilterExpression = String @@ FilterExpressionTag
  type FilterRef = String @@ FilterRefTag

  private[cloudflare] val tagFilterId: String => FilterId = shapeless.tag[FilterIdTag][String]
  private[cloudflare] val tagFilterExpression: String => FilterExpression = shapeless.tag[FilterExpressionTag][String]
  private[cloudflare] val tagFilterRef: String => FilterRef = shapeless.tag[FilterRefTag][String]
}


package filters {
  trait FilterIdTag
  trait FilterExpressionTag
  trait FilterRefTag

  case class Filter(id: Option[FilterId] = None,
                    expression: FilterExpression,
                    paused: Boolean,
                    description: Option[String] = None,
                    ref: Option[FilterRef] = None)

  object Filter extends StringAsBooleanCodec {
    implicit val FilterCodec: Codec[Filter] = semiauto.deriveCodec
  }
}
