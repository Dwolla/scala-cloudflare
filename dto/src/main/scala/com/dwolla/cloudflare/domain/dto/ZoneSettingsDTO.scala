package com.dwolla.cloudflare.domain.dto

case class ZoneSettingsDTO(id: String,
                           value: String,
                           editable: Option[Boolean],
                           modified_on: Option[String],
                          )
