package com.dwolla

import org.http4s.Uri

package object cloudflare {
  val BaseUrl: Uri = Uri.uri("https://api.cloudflare.com") / "client" / "v4"
}
