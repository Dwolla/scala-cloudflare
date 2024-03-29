package com.dwolla.cloudflare.domain.dto

import cats.implicits._
import io.circe._
import io.circe.generic.semiauto._

import scala.annotation.nowarn

sealed trait BaseResponseDTO[T] {
  def success: Boolean
  def errors: Option[Seq[ResponseInfoDTO]]
  def messages: Option[Seq[ResponseInfoDTO]]
}

object BaseResponseDTO {
  def unapply(arg: BaseResponseDTO[_]): Option[(Boolean, Option[Seq[ResponseInfoDTO]], Option[Seq[ResponseInfoDTO]])] =
    Option((arg.success, arg.errors, arg.messages))

  implicit def baseResponseDTODecoder[T: Decoder]: Decoder[BaseResponseDTO[T]] =
    Decoder[PagedResponseDTO[T]].widen or Decoder[ResponseDTO[T]].widen
}

case class ResponseInfoDTO (
  code: Option[Int],
  message: String,
  error_chain: Option[List[ResponseInfoDTO]] = None,
)

object ResponseInfoDTO {
  implicit val responseInfoDTOCodec: Codec[ResponseInfoDTO] = deriveCodec
}

case class ResultInfoDTO (
  page: Int,
  per_page: Int,
  count: Int,
  total_count: Int,
  total_pages: Int
)

object ResultInfoDTO {
  implicit val resultInfoDTOCodec: Codec[ResultInfoDTO] = deriveCodec
}

case class ResponseDTO[T] (
  result: Option[T],
  success: Boolean,
  errors: Option[List[ResponseInfoDTO]],
  messages: Option[List[ResponseInfoDTO]]
) extends BaseResponseDTO[T]

object ResponseDTO {
  def apply[T](result: T,
               success: Boolean,
               errors: Option[List[ResponseInfoDTO]],
               messages: Option[List[ResponseInfoDTO]]
              ): ResponseDTO[T] = ResponseDTO(
    result = Option(result),
    success = success,
    errors = errors,
    messages = messages
  )

  @nowarn("msg=parameter .* is never used")
  implicit def responseDTOEncoder[T: Encoder]: Encoder[ResponseDTO[T]] = deriveEncoder[ResponseDTO[T]]
  @nowarn("msg=parameter .* is never used")
  implicit def responseDTODecoder[T: Decoder]: Decoder[ResponseDTO[T]] = deriveDecoder[ResponseDTO[T]]
}

case class PagedResponseDTO[T] (
  result: List[T],
  success: Boolean,
  errors: Option[Seq[ResponseInfoDTO]],
  messages: Option[Seq[ResponseInfoDTO]],
  result_info: Option[ResultInfoDTO],
) extends BaseResponseDTO[T]

object PagedResponseDTO {
  @nowarn("msg=parameter .* is never used")
  implicit def pagedResponseDTOEncoder[T: Encoder]: Encoder[PagedResponseDTO[T]] = deriveEncoder
  @nowarn("msg=parameter .* is never used")
  implicit def pagedResponseDTODecoder[T: Decoder]: Decoder[PagedResponseDTO[T]] = deriveDecoder
}

case class DeleteResult(id: String)

object DeleteResult {
  implicit val deleteResultCodec: Codec[DeleteResult] = deriveCodec
}
