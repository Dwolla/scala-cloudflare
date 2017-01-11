package com.dwolla.cloudflare

import org.apache.http.HttpResponse
import org.apache.http.client.methods.{CloseableHttpResponse, HttpRequestBase}
import org.apache.http.impl.client.CloseableHttpClient
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

class CloudflareApiExecutorSpec(implicit ee: ExecutionEnv) extends Specification with Mockito {

  trait Setup extends Scope {
    val authorization = CloudflareAuthorization("email", "key")
    val mockHttpClient = mock[CloseableHttpClient]

    val executor = new CloudflareApiExecutor(authorization) {
      override lazy val httpClient = mockHttpClient
    }
  }

  "Cloudflare API Executor" should {
    "add required headers to requests" in new Setup {
      val request = mock[HttpRequestBase]
      private val response = mock[CloseableHttpResponse]

      mockHttpClient.execute(request) returns response

      val output = executor.fetch(request)(res ⇒ Some(res))

      output must beSome(response.asInstanceOf[HttpResponse]).await

      there was one(request).addHeader("X-Auth-Email", authorization.email)
      there was one(request).addHeader("X-Auth-Key", authorization.key)
      there was one(request).addHeader("Content-Type", "application/json")
      there was one(response).close()
    }

    "close the HttpClient on close" in new Setup {
      executor.close()

      there was one(mockHttpClient).close()
    }
  }

}
