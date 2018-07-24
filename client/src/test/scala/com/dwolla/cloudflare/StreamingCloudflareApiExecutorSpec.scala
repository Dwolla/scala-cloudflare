package com.dwolla.cloudflare

import cats.effect._
import com.dwolla.cloudflare.domain.dto._
import dwolla.cloudflare.FakeCloudflareService
import io.circe.generic.auto._
import io.circe.syntax._
import fs2._
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import org.http4s.client.dsl.Http4sClientDsl

class StreamingCloudflareApiExecutorSpec(implicit ee: ExecutionEnv) extends Specification with Http4sDsl[IO] with Http4sClientDsl[IO] {

  object PageQueryParamMatcher extends OptionalQueryParamDecoderMatcher[Int]("page")

  private val authorization = CloudflareAuthorization("email", "key")
  private val fakeCloudflareService = new FakeCloudflareService(authorization)

  def responseForPage(page: Int) = PagedResponseDTO[String](
    result = List(s"page-$page"),
    success = true,
    errors = None,
    messages = None,
    result_info = ResultInfoDTO(
      page = page,
      per_page = 1,
      count = 1,
      total_count = 3,
      total_pages = 3,
    ),
  )

  val singleResult = HttpService[IO] {
    case GET -> Root ⇒
      Ok(ResponseDTO("single-result", success = true, errors = None, messages = None).asJson)
  }

  val multiplePages = HttpService[IO] {
    case GET -> Root :? PageQueryParamMatcher(page) if page.contains(1) || page.isEmpty ⇒
      Ok(responseForPage(1).asJson)
    case GET -> Root :? PageQueryParamMatcher(Some(page)) if page < 4 ⇒
      Ok(responseForPage(page).asJson)
    case GET -> Root :? PageQueryParamMatcher(Some(page)) if page > 3 ⇒
      BadRequest(PagedResponseDTO[List[String]](
        result = List.empty,
        success = false,
        errors = None,
        messages = None,
        result_info = ResultInfoDTO(
          page = 1,
          per_page = 1,
          count = 1,
          total_count = 3,
          total_pages = 3,
        ),
      ).asJson)
  }

  trait Setup extends Scope {
    def client(service: HttpService[IO]) = new StreamingCloudflareApiExecutor[IO](fakeCloudflareService.client(service), authorization)
  }

  "fetch" should {
    "retrieve all the pages specified, once, and no more" in new Setup {
      private val output = for {
        req ← Stream.eval(GET(Uri.uri("https://api.cloudflare.com/")))
        res ← client(multiplePages).fetch[String](req)
      } yield res

      output.compile.toList.unsafeToFuture() must be_==(List("page-1", "page-2", "page-3")).await
    }

    "return a single result if the response is not paginated" in new Setup {
      private val output = for {
        req ← Stream.eval(GET(Uri.uri("https://api.cloudflare.com/")))
        res ← client(singleResult).fetch[String](req)
      } yield res

      output.compile.toList.unsafeToFuture() must be_==(List("single-result")).await
    }
  }
}
