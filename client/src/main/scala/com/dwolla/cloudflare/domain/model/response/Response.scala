package com.dwolla.cloudflare.domain.model.response

case class PagingInfo (
  page: Int,
  perPage: Int,
  count: Int,
  total: Int,
  totalPages: Int
)

case class PagedResponse[T] (
  result: T,
  paging: PagingInfo
)
