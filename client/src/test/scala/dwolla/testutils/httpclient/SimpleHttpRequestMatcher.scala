package dwolla.testutils.httpclient

import org.apache.http.client.methods.HttpRequestBase
import org.specs2.matcher.{Expectable, MatchResult, Matcher}

case class SimpleHttpRequestMatcher(req: HttpRequestBase) extends Matcher[HttpRequestBase] {
  override def apply[S <: HttpRequestBase](t: Expectable[S]): MatchResult[S] = {
    val actualValue = t.value
    val test = actualValue != null && req.getMethod == actualValue.getMethod && req.getURI == actualValue.getURI
    result(test, s"${t.description} is the same as ${req.toString}", s"${t.description} is not the same as ${req.toString}", t)
  }
}

object SimpleHttpRequestMatcher {
  def http(req: HttpRequestBase) = SimpleHttpRequestMatcher(req)
}
