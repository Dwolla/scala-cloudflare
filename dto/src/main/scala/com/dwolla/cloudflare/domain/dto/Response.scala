package com.dwolla.cloudflare.domain.dto

trait BaseResponseDTO[T] {
  def success: Boolean
  def errors: Option[Seq[ResponseInfoDTO]]
  def messages: Option[Seq[ResponseInfoDTO]]
}

case class ResponseInfoDTO (
  code: Int,
  message: String
)

case class ResultInfoDTO (
  page: Int,
  per_page: Int,
  count: Int,
  total_count: Int,
  total_pages: Int
)

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
}

case class PagedResponseDTO[T] (
  result: List[T],
  success: Boolean,
  errors: Option[Seq[ResponseInfoDTO]],
  messages: Option[Seq[ResponseInfoDTO]],
  result_info: ResultInfoDTO
) extends BaseResponseDTO[T]

case class DeleteResult(id: String)
