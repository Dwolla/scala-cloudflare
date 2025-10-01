package com.dwolla.cloudflare

import cats.effect.*
import com.dwolla.cloudflare.domain.model.*
import com.dwolla.cloudflare.domain.model.Exceptions.UnexpectedCloudflareErrorException
import com.dwolla.cloudflare.domain.model.pagerules.*
import com.dwolla.cloudflare.domain.model.pagerules.PageRuleStatus.{Active, Disabled}
import dwolla.cloudflare.FakeCloudflareService
import munit.{CatsEffectSuite, ScalaCheckSuite}
import org.http4s.HttpRoutes
import org.http4s.syntax.all.*
import org.scalacheck.{Arbitrary, Gen}

import java.time.Instant

class PageRuleClientTest extends CatsEffectSuite with ScalaCheckSuite {

  // Common setup values and helper
  val zoneId: ZoneId = tagZoneId("zone-id")
  val pageRuleId: PageRuleId = tagPageRuleId("50fdc2d542e0f6c6246963277d1dc140")

  val authorization = CloudflareAuthorization("email", "key")

  private def buildPageRuleClient(service: HttpRoutes[IO]): PageRuleClient[IO] = {
    val fakeService = new FakeCloudflareService(authorization)
    PageRuleClient(new StreamingCloudflareApiExecutor(fakeService.client(service), authorization))
  }

  test("list should list the page rules for the given zone") {
    val fakeService = new FakeCloudflareService(authorization)
    val client = buildPageRuleClient(fakeService.listPageRules(zoneId))
    val output = client.list(zoneId)

    val expected = List(
      PageRule(
        id = Option("50fdc2d542e0f6c6246963277d1dc140").map(tagPageRuleId),
        targets = List(
          PageRuleTarget("url", PageRuleConstraint("matches", "http://hydragents.xyz/"))
        ),
        actions = List(
          ForwardingUrl(uri"http://hydragents.xyz/home", PermanentRedirect)
        ),
        priority = 2,
        status = Disabled,
        created_on = Option("2019-01-18T21:33:59.000000Z").map(Instant.parse),
        modified_on = Option("2019-01-23T01:03:53.000000Z").map(Instant.parse)
      ),
      PageRule(
        id = Option("b7cc3152e872cf6e02384706fbabcc7f").map(tagPageRuleId),
        targets = List(
          PageRuleTarget("url", PageRuleConstraint("matches", "http://*.hydragents.xyz/*"))
        ),
        actions = List(AlwaysUseHttps),
        priority = 1,
        status = Disabled,
        created_on = Option("2017-03-27T17:28:36.000000Z").map(Instant.parse),
        modified_on = Option("2017-03-27T18:33:11.000000Z").map(Instant.parse)
      )
    )

    assertIO(output.compile.toList.map(_.toSet), expected.toSet)
  }

  test("get by id should return the page rule with the given id") {
    val fakeService = new FakeCloudflareService(authorization)
    val client = buildPageRuleClient(fakeService.getPageRuleById(zoneId, pageRuleId))
    val output = client.getById(zoneId, pageRuleId: String)

    val expected = List(PageRule(
      id = Option(pageRuleId),
      targets = List(
        PageRuleTarget("url", PageRuleConstraint("matches", "http://hydragents.xyz/"))
      ),
      actions = List(
        ForwardingUrl(uri"http://hydragents.xyz/home", PermanentRedirect)
      ),
      priority = 2,
      status = Disabled,
      created_on = Option("2019-01-18T21:33:59.000000Z").map(Instant.parse),
      modified_on = Option("2019-01-23T01:03:53.000000Z").map(Instant.parse)
    ))

    assertIO(output.compile.toList, expected)
  }

  private val createInput = PageRule(
    targets = List(
      PageRuleTarget("url", PageRuleConstraint("matches", "http://hydragents.xyz/")),
    ),
    actions = List(
      AlwaysUseHttps,
    ),
    priority = 42,
    status = Active,
  )

  test("create should send the json object and return its value") {
    val fakeService = new FakeCloudflareService(authorization)
    val client = buildPageRuleClient(fakeService.createPageRule(zoneId, pageRuleId))
    val output = client.create(zoneId, createInput)

    val expected = List(createInput.copy(
      id = Option(pageRuleId),
      created_on = Option("1983-09-10T21:33:59.000000Z").map(Instant.parse),
      modified_on = Option("2019-01-24T11:09:11.000000Z").map(Instant.parse),
    ))

    assertIO(output.compile.toList, expected)
  }

  test("raise a reasonable error if Cloudflare's business rules are violated") {
    val fakeService = new FakeCloudflareService(authorization)
    val client = buildPageRuleClient(fakeService.createPageRuleFails)
    val output = client.create(zoneId, createInput)

    val expected = List(
      Left(
        // TODO error code 1004 seems to be a validation error; we may want to create a more specific exception for that
        UnexpectedCloudflareErrorException(
          List(Error(Option(1004), "Page Rule validation failed: See messages for details.")),
          List(Message(Option(1), ".distinctTargetUrl: Your zone already has an existing page rule with that URL. If you are modifying only page rule settings use the Edit Page Rule option instead", None))
        )
      )
    )

    assertIO(output.attempt.compile.toList, expected)
  }

  test("update should update the given page rule") {
    val input = PageRule(
      id = Option(pageRuleId),
      targets = List(
        PageRuleTarget("url", PageRuleConstraint("matches", "http://hydragents.xyz/")),
      ),
      actions = List(
        AlwaysUseHttps,
      ),
      priority = 42,
      status = Active,
    )

    val fakeService = new FakeCloudflareService(authorization)
    val client = buildPageRuleClient(fakeService.updatePageRule(zoneId, pageRuleId))
    val output = client.update(zoneId, input)

    val expected = List(input.copy(modified_on = Option("2019-01-24T11:09:11.000000Z").map(Instant.parse)))

    assertIO(output.compile.toList, expected)
  }

  test("update should raise an exception when trying to update an unidentified page rule") {
    val input = PageRule(
      id = None,
      targets = List.empty,
      actions = List.empty,
      priority = 42,
      status = Active,
    )

    val fakeService = new FakeCloudflareService(authorization)
    val client = buildPageRuleClient(fakeService.updatePageRule(zoneId, pageRuleId))
    val output = client.update(zoneId, input)

    val expected = List(Left(CannotUpdateUnidentifiedPageRule(input)))

    assertIO(output.attempt.compile.toList, expected)
  }

  test("delete should delete the given page rule") {
    val fakeService = new FakeCloudflareService(authorization)
    val client = buildPageRuleClient(fakeService.deletePageRule(zoneId, pageRuleId))
    val output = client.delete(zoneId, pageRuleId)

    val expected = List(pageRuleId)
    assertIO(output.compile.toList, expected)
  }

  test("delete should return success if the page rule id doesn't exist") {
    val fakeService = new FakeCloudflareService(authorization)
    val client = buildPageRuleClient(fakeService.deletePageRuleThatDoesNotExist(zoneId, true))
    val output = client.delete(zoneId, pageRuleId)

    val expected = List(pageRuleId)
    assertIO(output.compile.toList, expected)
  }

  test("delete should return success if the page rule id is invalid") {
    val fakeService = new FakeCloudflareService(authorization)
    val client = buildPageRuleClient(fakeService.deletePageRuleThatDoesNotExist(zoneId, false))
    val output = client.delete(zoneId, pageRuleId)

    val expected = List(pageRuleId)
    assertIO(output.compile.toList, expected)
  }

  // property-based: buildUri and parseUri are inverses
  private val nonEmptyAlphaNumericString = Gen.identifier
  implicit private val arbitraryZoneId: Arbitrary[ZoneId] = Arbitrary(nonEmptyAlphaNumericString.map(shapeless.tag[ZoneIdTag][String]))
  implicit private val arbitraryPageRuleId: Arbitrary[PageRuleId] = Arbitrary(nonEmptyAlphaNumericString.map(shapeless.tag[PageRuleIdTag][String]))

  property("buildUri and parseUri should be inverses") {
    import org.scalacheck.Prop.forAll

    forAll { (zoneId: ZoneId, pageRuleId: PageRuleId) =>
      val client = new PageRuleClient[IO] {
        override def list(zoneId: ZoneId): fs2.Stream[IO, PageRule] = ???
        override def getById(zoneId: ZoneId, pageRuleId: String): fs2.Stream[IO, PageRule] = ???
        override def create(zoneId: ZoneId, pageRule: PageRule): fs2.Stream[IO, PageRule] = ???
        override def update(zoneId: ZoneId, pageRule: PageRule): fs2.Stream[IO, PageRule] = ???
        override def delete(zoneId: ZoneId, pageRuleId: String): fs2.Stream[IO, PageRuleId] = ???
      }

      assertEquals(client.parseUri(client.buildUri(zoneId, pageRuleId).renderString), Some((zoneId, pageRuleId)))
    }
  }
}
