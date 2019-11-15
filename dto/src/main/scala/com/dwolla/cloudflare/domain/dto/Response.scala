package com.dwolla.cloudflare.domain.dto

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto._

trait BaseResponseDTO[T] {
  def success: Boolean
  def errors: Option[Seq[ResponseInfoDTO]]
  def messages: Option[Seq[ResponseInfoDTO]]
}

object BaseResponseDTO {
  def unapply(arg: BaseResponseDTO[_]): Option[(Boolean, Option[Seq[ResponseInfoDTO]], Option[Seq[ResponseInfoDTO]])] =
    Option((arg.success, arg.errors, arg.messages))
}

case class ResponseInfoDTO (
  code: Option[Int],
  message: String,
  error_chain: Option[List[ResponseInfoDTO]] = None,
)

object ResponseInfoDTO {
  implicit val responseInfoDTOEncoder: Encoder[ResponseInfoDTO] = deriveEncoder
  implicit val responseInfoDTODecoder: Decoder[ResponseInfoDTO] = deriveDecoder
}

case class ResultInfoDTO (
  page: Int,
  per_page: Int,
  count: Int,
  total_count: Int,
  total_pages: Int
)

object ResultInfoDTO {
  implicit val resultInfoDTOEncoder: Encoder[ResultInfoDTO] = deriveEncoder
  implicit val resultInfoDTODecoder: Decoder[ResultInfoDTO] = deriveDecoder
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

  implicit def responseDTOEncoder[T: Encoder]: Encoder[ResponseDTO[T]] = deriveEncoder
  implicit def responseDTODecoder[T: Decoder]: Decoder[ResponseDTO[T]] = deriveDecoder
}

case class PagedResponseDTO[T] (
  result: List[T],
  success: Boolean,
  errors: Option[Seq[ResponseInfoDTO]],
  messages: Option[Seq[ResponseInfoDTO]],
  result_info: Option[ResultInfoDTO],
) extends BaseResponseDTO[T]

object PagedResponseDTO {
  implicit def pagedResponseDTOEncoder[T: Encoder]: Encoder[PagedResponseDTO[T]] = deriveEncoder
  implicit def pagedResponseDTODecoder[T: Decoder]: Decoder[PagedResponseDTO[T]] = deriveDecoder
}

case class DeleteResult(id: String)

object DeleteResult {
  implicit val deleteResultEncoder: Encoder[DeleteResult] = deriveEncoder
  implicit val deleteResultDecoder: Decoder[DeleteResult] = deriveDecoder
}
