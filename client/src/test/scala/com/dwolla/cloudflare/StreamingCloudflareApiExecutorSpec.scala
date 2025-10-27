package com.dwolla.cloudflare

import cats.effect.*
import com.dwolla.cloudflare.domain.dto.*
import com.dwolla.cloudflare.domain.model.Exceptions.*
import dwolla.cloudflare.FakeCloudflareService
import io.circe.syntax.*
import munit.CatsEffectSuite
import natchez.Trace.Implicits.noop
import org.http4s.*
import org.http4s.circe.*
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.dsl.Http4sDsl
import org.http4s.syntax.all.*

class StreamingCloudflareApiExecutorSpec
  extends CatsEffectSuite
    with Http4sDsl[IO]
    with Http4sClientDsl[IO] {

  object PageQueryParamMatcher extends OptionalQueryParamDecoderMatcher[Int]("page")

  private val authorization = CloudflareAuthorization("email", "key")
  private val fakeCloudflareService = new FakeCloudflareService(authorization)

  def responseForPage(page: Int) = PagedResponseDTO[String](
    result = List(s"page-$page"),
    success = true,
    errors = None,
    messages = None,
    result_info = Some(ResultInfoDTO(
      page = page,
      per_page = 1,
      count = 1,
      total_count = 3,
      total_pages = 3,
    )),
  )

  val singleResult = HttpRoutes.of[IO] {
    case GET -> Root =>
      Ok(ResponseDTO("single-result", success = true, errors = None, messages = None).asJson)
  }

  val singlePage = HttpRoutes.of[IO] {
    case GET -> Root =>
      Ok(PagedResponseDTO[String](
        result = List("single-page"),
        success = true,
        errors = None,
        messages = None,
        result_info = None
      ).asJson)
  }

  val multiplePages = HttpRoutes.of[IO] {
    case GET -> Root :? PageQueryParamMatcher(page) if page.contains(1) || page.isEmpty =>
      Ok(responseForPage(1).asJson)
    case GET -> Root :? PageQueryParamMatcher(Some(page)) if page < 4 =>
      Ok(responseForPage(page).asJson)
    case GET -> Root :? PageQueryParamMatcher(Some(page)) if page > 3 =>
      BadRequest(PagedResponseDTO[List[String]](
        result = List.empty,
        success = false,
        errors = None,
        messages = None,
        result_info = Some(ResultInfoDTO(
          page = 1,
          per_page = 1,
          count = 1,
          total_count = 3,
          total_pages = 3,
        )),
      ).asJson)
  }

  val authorizationFailure = HttpRoutes.of[IO] {
    case GET -> Root / "forbidden" =>
      Forbidden()
    case GET -> Root / "invalid-headers" =>
      Ok(ResponseDTO[Unit](
        None,
        success = false,
        errors = Some(List(
          ResponseInfoDTO(Option(6003), "Invalid request headers", Option(List(
            ResponseInfoDTO(Option(6102), "Invalid format for X-Auth-Email header"),
            ResponseInfoDTO(Option(6103), "Invalid format for X-Auth-Key header"),
          )))
        )),
        messages = None).asJson)
  }

  // helper to build the executor client from a service
  private def client(service: HttpRoutes[IO]) =
    new StreamingCloudflareApiExecutor[IO](fakeCloudflareService.client(service), authorization)

  test("fetch retrieves all the pages specified, once, and no more") {
    val output = for {
      res <- client(multiplePages).fetch[String](GET(uri"https://api.cloudflare.com/"))
    } yield res

    assertIO(output.compile.toList, List("page-1", "page-2", "page-3"))
  }

  test("fetch returns a single page if the response is paginated without result_info") {
    val output = for {
      res <- client(singlePage).fetch[String](GET(uri"https://api.cloudflare.com/"))
    } yield res

    assertIO(output.compile.toList, List("single-page"))
  }

  test("fetch returns a single result if the response is not paginated") {
    val output = for {
      res <- client(singleResult).fetch[String](GET(uri"https://api.cloudflare.com/"))
    } yield res

    assertIO(output.compile.toList, List("single-result"))
  }

  test("fetch raises an exception if authorization fails with a 403 response") {
    val output = for {
      res <- client(authorizationFailure).fetch[String](GET(uri"https://api.cloudflare.com/forbidden"))
    } yield res

    interceptIO[AccessDenied](output.compile.toList).map { ex =>
      assertEquals(ex.getMessage, "The given credentials were invalid")
    }
  }

  test("fetch raises an exception if the authorization fails due to invalid headers") {
    val output = for {
      res <- client(authorizationFailure).fetch[String](GET(uri"https://api.cloudflare.com/invalid-headers"))
    } yield res

    interceptIO[AccessDenied](output.compile.toList).map { ex =>
      val AccessDenied(msg) = ex
      assert(msg.contains(ResponseInfoDTO(Option(6102), "Invalid format for X-Auth-Email header")))
      assert(msg.contains(ResponseInfoDTO(Option(6103), "Invalid format for X-Auth-Key header")))
      val expectedPrefix = """The given credentials were invalid
        |
        |  See the following errors:
        |   -""".stripMargin
      assert(ex.getMessage.startsWith(expectedPrefix))
    }
  }
}
