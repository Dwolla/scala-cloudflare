package com.dwolla.cloudflare.domain.dto

import cats.syntax.all.*
import io.circe.*
import io.circe.generic.semiauto.*

sealed trait BaseResponseDTO[T] {
  def success: Boolean
  def errors: Option[Seq[ResponseInfoDTO]]
  def messages: Option[Seq[ResponseInfoDTO]]
}

object BaseResponseDTO {
  def unapply(arg: BaseResponseDTO[?]): Option[(Boolean, Option[Seq[ResponseInfoDTO]], Option[Seq[ResponseInfoDTO]])] =
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

  implicit def responseDTOEncoder[T: Encoder]: Encoder[ResponseDTO[T]] = deriveEncoder[ResponseDTO[T]]
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
  implicit def pagedResponseDTOEncoder[T: Encoder]: Encoder[PagedResponseDTO[T]] = deriveEncoder
  implicit def pagedResponseDTODecoder[T: Decoder]: Decoder[PagedResponseDTO[T]] = deriveDecoder
}

case class DeleteResult(id: String)

object DeleteResult {
  implicit val deleteResultCodec: Codec[DeleteResult] = deriveCodec
}
