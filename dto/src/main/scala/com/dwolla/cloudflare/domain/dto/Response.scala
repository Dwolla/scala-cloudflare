package com.dwolla.cloudflare.domain.dto

trait BaseResponseDTO[T] {
  def result: T
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
  result: T,
  success: Boolean,
  errors: Option[Seq[ResponseInfoDTO]],
  messages: Option[Seq[ResponseInfoDTO]]
) extends BaseResponseDTO[T]

case class PagedResponseDTO[T] (
  result: T,
  success: Boolean,
  errors: Option[Seq[ResponseInfoDTO]],
  messages: Option[Seq[ResponseInfoDTO]],
  result_info: ResultInfoDTO
) extends BaseResponseDTO[T]
