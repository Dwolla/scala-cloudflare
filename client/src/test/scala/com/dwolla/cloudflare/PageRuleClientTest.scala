package com.dwolla.cloudflare

import java.time.Instant

import cats.effect._
import com.dwolla.cloudflare.domain.model.Exceptions.UnexpectedCloudflareErrorException
import com.dwolla.cloudflare.domain.model._
import com.dwolla.cloudflare.domain.model.pagerules.PageRuleStatus.{Active, Disabled}
import com.dwolla.cloudflare.domain.model.pagerules._
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification
import dwolla.cloudflare.FakeCloudflareService
import org.http4s.HttpRoutes
import org.http4s.syntax.all._
import org.scalacheck.{Arbitrary, Gen}
import org.specs2.matcher.{IOMatchers, Matchers}
import org.specs2.specification.Scope

class PageRuleClientTest extends Specification with ScalaCheck with IOMatchers with Matchers {

  trait Setup extends Scope {
    val zoneId: ZoneId = tagZoneId("zone-id")
    val pageRuleId = tagPageRuleId("50fdc2d542e0f6c6246963277d1dc140")

    val authorization = CloudflareAuthorization("email", "key")
    val fakeService = new FakeCloudflareService(authorization)

    protected def buildPageRuleClient(service: HttpRoutes[IO]): PageRuleClient[IO] =
      PageRuleClient(new StreamingCloudflareApiExecutor(fakeService.client(service), authorization))

  }

  "list" should {

    "list the page rules for the given zone" in new Setup {
      private val client = buildPageRuleClient(fakeService.listPageRules(zoneId))
      private val output = client.list(zoneId)

      output.compile.toList must returnValue(contain(PageRule(
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
      )) and contain(PageRule(
        id = Option("b7cc3152e872cf6e02384706fbabcc7f").map(tagPageRuleId),
        targets = List(
          PageRuleTarget("url", PageRuleConstraint("matches", "http://*.hydragents.xyz/*"))
        ),
        actions = List(AlwaysUseHttps),
        priority = 1,
        status = Disabled,
        created_on = Option("2017-03-27T17:28:36.000000Z").map(Instant.parse),
        modified_on = Option("2017-03-27T18:33:11.000000Z").map(Instant.parse)
      )))
    }

  }

  "get by id" should {

    "return the page rule with the given id" in new Setup {
      private val client = buildPageRuleClient(fakeService.getPageRuleById(zoneId, pageRuleId))
      private val output = client.getById(zoneId, pageRuleId: String)

      output.compile.toList must returnValue(List(PageRule(
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
      )))
    }

  }

  "create" should {
    val input = PageRule(
      targets = List(
        PageRuleTarget("url", PageRuleConstraint("matches", "http://hydragents.xyz/")),
      ),
      actions = List(
        AlwaysUseHttps,
      ),
      priority = 42,
      status = Active,
    )

    "send the json object and return its value" in new Setup {
      private val client = buildPageRuleClient(fakeService.createPageRule(zoneId, pageRuleId))
      private val output = client.create(zoneId, input)

      output.compile.toList must returnValue(List(input.copy(
        id = Option(pageRuleId),
        created_on = Option("1983-09-10T21:33:59.000000Z").map(Instant.parse),
        modified_on = Option("2019-01-24T11:09:11.000000Z").map(Instant.parse),
      )))
    }

    "raise a reasonable error if Cloudflare's business rules are violated" in new Setup {
      private val client = buildPageRuleClient(fakeService.createPageRuleFails)
      private val output = client.create(zoneId, input)

      output.attempt.compile.toList must returnValue(List(
        Left(
          // TODO error code 1004 seems to be a validation error; we may want to create a more specific exception for that
          UnexpectedCloudflareErrorException(
            List(Error(Option(1004), "Page Rule validation failed: See messages for details.")),
            List(Message(Option(1), ".distinctTargetUrl: Your zone already has an existing page rule with that URL. If you are modifying only page rule settings use the Edit Page Rule option instead", None))
          )
        )
      ))
    }
  }

  "update" should {
    "update the given page rule" in new Setup {
      private val input = PageRule(
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

      private val client = buildPageRuleClient(fakeService.updatePageRule(zoneId, pageRuleId))
      private val output = client.update(zoneId, input)

      output.compile.toList must returnValue(List(input.copy(modified_on = Option("2019-01-24T11:09:11.000000Z").map(Instant.parse))))
    }

    "raise an exception when trying to update an unidentified page rule" in new Setup {
      private val input = PageRule(
        id = None,
        targets = List.empty,
        actions = List.empty,
        priority = 42,
        status = Active,
      )

      private val client = buildPageRuleClient(fakeService.updatePageRule(zoneId, pageRuleId))
      private val output = client.update(zoneId, input)

      output.attempt.compile.toList must returnValue(List(
        Left(CannotUpdateUnidentifiedPageRule(input))
      ))
    }
  }

  "delete" should {
    "delete the given page rule" in new Setup {
      private val client = buildPageRuleClient(fakeService.deletePageRule(zoneId, pageRuleId))
      private val output = client.delete(zoneId, pageRuleId)

      output.compile.toList must returnValue(List(pageRuleId))
    }

    "return success if the page rule id doesn't exist" in new Setup {
      private val client = buildPageRuleClient(fakeService.deletePageRuleThatDoesNotExist(zoneId, true))
      private val output = client.delete(zoneId, pageRuleId)

      output.compile.toList must returnValue(List(pageRuleId))
    }

    "return success if the page rule id is invalid" in new Setup {
      private val client = buildPageRuleClient(fakeService.deletePageRuleThatDoesNotExist(zoneId, false))
      private val output = client.delete(zoneId, pageRuleId)

      output.compile.toList must returnValue(List(pageRuleId))
    }
  }

  "buildUri and parseUri" should {
    val nonEmptyAlphaNumericString = Gen.asciiPrintableStr.suchThat(_.nonEmpty)
    implicit val arbitraryZoneId = Arbitrary(nonEmptyAlphaNumericString.map(shapeless.tag[ZoneIdTag][String]))
    implicit val arbitraryPageRuleId = Arbitrary(nonEmptyAlphaNumericString.map(shapeless.tag[PageRuleIdTag][String]))

    "be the inverse of each other" >> { prop { (zoneId: ZoneId, pageRuleId: PageRuleId) =>
      val client = new PageRuleClient[IO] {
        override def list(zoneId: ZoneId): fs2.Stream[IO, PageRule] = ???
        override def getById(zoneId: ZoneId, pageRuleId: String): fs2.Stream[IO, PageRule] = ???
        override def create(zoneId: ZoneId, pageRule: PageRule): fs2.Stream[IO, PageRule] = ???
        override def update(zoneId: ZoneId, pageRule: PageRule): fs2.Stream[IO, PageRule] = ???
        override def delete(zoneId: ZoneId, pageRuleId: String): fs2.Stream[IO, PageRuleId] = ???
      }

      client.parseUri(client.buildUri(zoneId, pageRuleId)) must beSome((zoneId, pageRuleId))
    }}
  }
}
