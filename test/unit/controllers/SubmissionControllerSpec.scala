/*
 * Copyright 2019 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package unit.controllers

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import play.api.http.ContentTypes
import play.api.libs.json.Json
import play.api.mvc.Codec
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.{InsufficientConfidenceLevel, InsufficientEnrolments}
import uk.gov.hmrc.customs.imports.connectors.CustomsDeclarationsResponse
import uk.gov.hmrc.customs.imports.controllers.CustomsHeaderNames
import uk.gov.hmrc.customs.imports.models._
import uk.gov.hmrc.http.HeaderCarrier
import unit.base.{CustomsImportsBaseSpec, ImportsTestData}

import scala.concurrent.{ExecutionContext, Future}
import scala.xml.NodeSeq

class SubmissionControllerSpec extends CustomsImportsBaseSpec with ImportsTestData with MockitoSugar with BeforeAndAfterEach{

  val saveUri = "/declaration"

  val xmlBody: String =  randomSubmitDeclaration.toXml

  val fakeXmlRequest: FakeRequest[String] = FakeRequest("POST", saveUri)
    .withBody(xmlBody).withHeaders(CONTENT_TYPE -> ContentTypes.XML(Codec.utf_8))

  val fakeXmlRequestWithHeaders: FakeRequest[String] = fakeXmlRequest
    .withHeaders(CustomsHeaderNames.XLrnHeaderName -> declarantLrnValue,
      AUTHORIZATION -> dummyToken,
      CONTENT_TYPE -> ContentTypes.XML(Codec.utf_8))


  val fakeNonXmlRequestWithHeaders: FakeRequest[String] = FakeRequest("POST", saveUri)
    .withBody("SOMEUNKNOWNTEXTNOTXML")
    .withHeaders(CustomsHeaderNames.XLrnHeaderName -> declarantLrnValue,
      CONTENT_TYPE -> ContentTypes.JSON)

  override def beforeEach() {
    reset(mockImportService, mockAuthConnector)
  }

  "POST /declaration " should {

    "return 202 Accepted with X-Conversation-ID header when submission is persisted and xml request is processed" in {
      withAuthorizedUser()
      when(mockDeclarationsApiConnector.submitImportDeclaration(any[String], any[String])(any[HeaderCarrier],any[ExecutionContext]))
        .thenReturn(Future.successful(CustomsDeclarationsResponse(randomConversationId)))
      when(mockImportService.handleDeclarationSubmit(any[String], any[String], any[NodeSeq])(any[HeaderCarrier])).thenReturn(Future.successful(Some(conversationId)))

      val result = route(app, fakeXmlRequestWithHeaders).get

      status(result) shouldBe ACCEPTED
      result.header.headers.get("X-Conversation-ID") shouldBe Some(conversationId)

      verify(mockImportService, times(1)).handleDeclarationSubmit(any[String], any[String], any[NodeSeq])(any[HeaderCarrier])
    }

    "return 401 when authorisation fails, no enrolments" in {
      unAuthorisedUser(exceptionToThrow = InsufficientEnrolments("jhkjhk"))

      val result = route(app, fakeXmlRequestWithHeaders).get
        status(result) shouldBe UNAUTHORIZED
    }

    "return 401 when authorisation fails, other authorisation Error" in {
      unAuthorisedUser(exceptionToThrow = InsufficientConfidenceLevel("vote of no confidence"))

      val result = route(app, fakeXmlRequestWithHeaders).get
        status(result) shouldBe UNAUTHORIZED
    }

    "return 401 when user is without Eori" in {
      userWithoutEori()

      val result = route(app, fakeXmlRequestWithHeaders).get
        status(result) shouldBe UNAUTHORIZED
    }

    "return 500 when authorisation fails with non auth related exception" in {
      unAuthorisedUser(exceptionToThrow = new RuntimeException("Grrrrrr"))

      val result = route(app, fakeXmlRequestWithHeaders).get
      status(result) shouldBe INTERNAL_SERVER_ERROR
    }



    "return 500 when confirm submission is NOT persisted and xml request is processed" in {
      withAuthorizedUser()
      when(mockImportService.handleDeclarationSubmit(any[String], any[String], any[NodeSeq])(any[HeaderCarrier])).thenReturn(Future.successful(None))

        val result = route(app, fakeXmlRequestWithHeaders).get
      status(result) shouldBe INTERNAL_SERVER_ERROR
    }

    "return 500 when something goes wrong" in {
      withAuthorizedUser()
      when(mockImportService.handleDeclarationSubmit(any[String], any[String], any[NodeSeq])(any[HeaderCarrier]))
        .thenReturn(Future.failed(new RuntimeException("Mongo say no go")))

      val result = route(app, fakeXmlRequestWithHeaders).get
      status(result) shouldBe INTERNAL_SERVER_ERROR

    }

    "return 400 when nonXMl is sent" in {

      val result = route(app, fakeNonXmlRequestWithHeaders).get
      status(result) shouldBe BAD_REQUEST

    }

    "return 500 when headers not present" in {
      withAuthorizedUser()

      val result = route(app, fakeXmlRequest).get
      status(result) shouldBe INTERNAL_SERVER_ERROR

    }

  }

  "POST /notify " should {
    "return 200 OK" in {
      val result = route(app, FakeRequest("POST", "/notify")).get

      status(result) shouldBe OK
    }
  }

  "GET /declarations" should {
    val request = FakeRequest("GET", "/declarations")

    "return 200 Ok with list of declarations for the logged in user" in {
      val submissions = Seq(cancelledDeclaration, newlySubmittedDeclaration, unacknowledgedDeclaration)

      withAuthorizedUser()
      when(mockImportService.getSubmissions(any[String])).thenReturn(Future.successful(submissions))

      val result = await(route(app, request).get)

      status(result) shouldBe OK
      jsonBodyOf(result) shouldBe Json.toJson(submissions)
    }

    "return 200 Ok with empty list when there are no declarations present for the logged in user" in {
      withAuthorizedUser()
      when(mockImportService.getSubmissions(any[String])).thenReturn(Future.successful(Seq.empty))

      val result = await(route(app, request).get)

      status(result) shouldBe OK
      jsonBodyOf(result) shouldBe Json.arr()
    }

    "return 401 Unauthorized when authorisation fails due to insufficient enrolments" in {
      unAuthorisedUser(exceptionToThrow = InsufficientEnrolments("write:something"))

      val result = await(route(app, request).get)
      status(result) shouldBe UNAUTHORIZED
    }

    "return 401 Unauthorized when authorisation fails, other authorisation Error" in {
      unAuthorisedUser(exceptionToThrow = InsufficientConfidenceLevel("vote of no confidence"))

      val result = await(route(app, request).get)
      status(result) shouldBe UNAUTHORIZED
    }

    "return 401 Unauthorized when user is without Eori" in {
      userWithoutEori()

      val result = await(route(app, request).get)
      status(result) shouldBe UNAUTHORIZED
    }

    "return 500 when authorisation fails with non auth related exception" in {
      unAuthorisedUser(exceptionToThrow = new RuntimeException("Connection refused"))

      val result = await(route(app, request).get)
      status(result) shouldBe INTERNAL_SERVER_ERROR
    }

    "return 500 when something goes wrong" in {
      withAuthorizedUser()
      when(mockImportService.getSubmissions(any[String])).thenReturn(Future.failed(new RuntimeException("Connection refused")))

      val result = await(route(app, request).get)
      status(result) shouldBe INTERNAL_SERVER_ERROR
    }

  }
}
