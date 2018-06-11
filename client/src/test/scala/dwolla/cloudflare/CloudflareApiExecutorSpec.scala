package dwolla.cloudflare

import cats.effect.IO
import com.dwolla.cloudflare._
import org.apache.http.HttpResponse
import org.apache.http.client.methods._
import org.apache.http.impl.client.CloseableHttpClient
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

class CloudflareApiExecutorSpec(implicit ee: ExecutionEnv) extends Specification with Mockito {

  trait Setup extends Scope {
    val authorization = CloudflareAuthorization("email", "key")
    val mockHttpClient = mock[CloseableHttpClient]
  }

  trait FutureSetup extends Setup {
    val executor = new FutureCloudflareApiExecutor(authorization) {
      override lazy val httpClient = mockHttpClient
    }
  }

  trait AsyncSetup extends Setup {
    val executor = new AsyncCloudflareApiExecutor[IO](authorization) {
      override lazy val httpClient = mockHttpClient
    }
  }

  "Future-based Cloudflare API Executor" should {
    "add required headers to requests" in new FutureSetup {
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

    "close the HttpClient on close" in new FutureSetup {
      executor.close()

      there was one(mockHttpClient).close()
    }
  }

  "Async-based Cloudflare API Executor" should {
    "add required headers to requests" in new AsyncSetup {
      val request = mock[HttpRequestBase]
      private val response = mock[CloseableHttpResponse]

      mockHttpClient.execute(request) returns response

      val output = executor.fetch(request)(res ⇒ Option(res)).unsafeToFuture()

      output must beSome(response.asInstanceOf[HttpResponse]).await

      there was one(request).addHeader("X-Auth-Email", authorization.email)
      there was one(request).addHeader("X-Auth-Key", authorization.key)
      there was one(request).addHeader("Content-Type", "application/json")
      there was one(response).close()
    }

    "close the HttpClient on close" in new AsyncSetup {
      executor.close()

      there was one(mockHttpClient).close()
    }
  }

}
