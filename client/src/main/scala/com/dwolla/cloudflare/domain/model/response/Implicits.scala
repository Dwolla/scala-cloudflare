package com.dwolla.cloudflare.domain.model.response

import com.dwolla.cloudflare.domain.dto.{ResponseInfoDTO, ResultInfoDTO}
import com.dwolla.cloudflare.domain.model.Error

object Implicits {
  implicit def toErrorList(dto: Seq[ResponseInfoDTO]): List[Error] = {
    dto.map(toError).toList
  }

  implicit def toError(dto: ResponseInfoDTO): Error = {
    Error(
      code = dto.code,
      message = dto.message
    )
  }

  implicit def toPagingInfo(dto: ResultInfoDTO): PagingInfo = {
    PagingInfo(
      page = dto.page,
      perPage = dto.per_page,
      count = dto.count,
      total = dto.total_count,
      totalPages = dto.total_pages
    )
  }
}
