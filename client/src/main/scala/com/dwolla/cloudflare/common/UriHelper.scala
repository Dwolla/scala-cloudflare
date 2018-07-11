package com.dwolla.cloudflare.common

import java.net.URLEncoder

object UriHelper {
  val BaseUrl = "https://api.cloudflare.com/client/v4"

  def buildApiUri(path: String, parameters: Option[String] = None): String = {
    val paramString = parameters.fold("") { p ⇒ s"?$p" }
    s"$BaseUrl/${path.stripPrefix("/")}$paramString"
  }

  def buildParameterString(params: Seq[Option[(String, Any)]]): String = {
    params
      .collect {
        case Some((key, value)) ⇒ s"$key=${URLEncoder.encode(value.toString, "UTF-8")}"
      }
      .mkString("&")
  }
}
