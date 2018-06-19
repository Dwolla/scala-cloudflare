package dwolla.cloudflare

import org.apache.http.{HttpEntity, HttpResponse, StatusLine}
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.{CloseableHttpResponse, HttpUriRequest}
import org.apache.http.message.BasicHttpResponse
import org.specs2.mock.Mockito
import org.specs2.mock.mockito.ArgumentCapture

import scala.concurrent.Promise
import scala.reflect.ClassTag

trait HttpClientHelper { self: Mockito ⇒
  def mockExecuteWithCaptor[T <: HttpUriRequest : ClassTag](response: HttpResponse)(implicit mockHttpClient: HttpClient): ArgumentCapture[T] = {
    val captor = capture[T]
    mockHttpClient.execute(captor) returns response

    captor
  }

  def fakeResponse(statusLine: StatusLine, entity: HttpEntity) = {
    val res = new BasicHttpResponse(statusLine) with CloseableHttpResponse {
      val promisedClose = Promise[Unit]

      override def close(): Unit = promisedClose.success(Unit)

      def isClosed: Boolean = promisedClose.isCompleted
    }

    res.setEntity(entity)

    res
  }
}
