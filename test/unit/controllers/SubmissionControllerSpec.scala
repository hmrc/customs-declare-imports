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
import play.api.mvc.Codec
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.{InsufficientConfidenceLevel, InsufficientEnrolments}
import uk.gov.hmrc.customs.imports.connectors.CustomsDeclarationsResponse
import uk.gov.hmrc.customs.imports.controllers.CustomsHeaderNames
import uk.gov.hmrc.customs.imports.models.Submission
import uk.gov.hmrc.http.HeaderCarrier
import unit.base.{CustomsImportsBaseSpec, ImportsTestData}

import scala.concurrent.{ExecutionContext, Future}

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
      CONTENT_TYPE -> ContentTypes.XML(Codec.utf_8))

  override def beforeEach() {
    reset(mockSubmissionRepository, mockDeclarationsApiConnector, mockAuthConnector)
  }

  "POST /declaration " should {

      "return 200 when submission is persisted and xml request is processed" in {
        withAuthorizedUser()
        when(mockDeclarationsApiConnector.submitImportDeclaration(any[String], any[String])(any[HeaderCarrier],any[ExecutionContext]))
          .thenReturn(Future.successful(CustomsDeclarationsResponse(randomConversationId)))
        when(mockSubmissionRepository.save(any[Submission])).thenReturn(Future.successful(true))

        val result = route(app, fakeXmlRequestWithHeaders).value
        status(result) must be(OK)
          verify(mockSubmissionRepository, times(1)).save(any[Submission])
      }

    "return 401 when authorisation fails, no enrolments" in {
      unAuthorisedUser(exceptionToThrow = InsufficientEnrolments("jhkjhk"))

      val result = route(app, fakeXmlRequestWithHeaders).value
      status(result) must be(UNAUTHORIZED)
    }

    "return 401 when authorisation fails, other authorisation Error" in {
      unAuthorisedUser(exceptionToThrow = InsufficientConfidenceLevel("vote of no confidence"))

      val result = route(app, fakeXmlRequestWithHeaders).value
      status(result) must be(UNAUTHORIZED)
    }

    "return 401 when user is without Eori" in {
      userWithoutEori()

      val result = route(app, fakeXmlRequestWithHeaders).value
      status(result) must be(UNAUTHORIZED)
    }

    "return 500 when authorisation fails with non auth related exception" in {
      unAuthorisedUser(exceptionToThrow = new RuntimeException("Grrrrrr"))

      val result = route(app, fakeXmlRequestWithHeaders).value
      status(result) must be(INTERNAL_SERVER_ERROR)
    }



    "return 500 when confirm submission is NOT persisted and xml request is processed" in {
      withAuthorizedUser()
      when(mockDeclarationsApiConnector.submitImportDeclaration(any[String], any[String])(any[HeaderCarrier],any[ExecutionContext]))
        .thenReturn(Future.successful(CustomsDeclarationsResponse(randomConversationId)))
      when(mockSubmissionRepository.save(Submission(declarantEoriValue, declarantLrnValue))).thenReturn(Future.successful(false))
      val result = route(app, fakeXmlRequestWithHeaders).value
      status(result) must be(INTERNAL_SERVER_ERROR)
      verify(mockSubmissionRepository, times(1)).save(any[Submission])
    }

    "return 500 when something goes wrong" in {
      withAuthorizedUser()
      when(mockDeclarationsApiConnector.submitImportDeclaration(any[String], any[String])(any[HeaderCarrier],any[ExecutionContext]))
        .thenReturn(Future.failed(httpException))

      val result = route(app, fakeXmlRequestWithHeaders).value
      status(result) must be(INTERNAL_SERVER_ERROR)
      verifyZeroInteractions(mockSubmissionRepository)
    }

    "return 400 when nonXMl is sent" in {
      when(mockDeclarationsApiConnector.submitImportDeclaration(any[String], any[String])(any[HeaderCarrier],any[ExecutionContext]))
        .thenReturn(Future.successful(CustomsDeclarationsResponse(randomConversationId)))

      val result = route(app, fakeNonXmlRequestWithHeaders).value
      status(result) must be(BAD_REQUEST)
      verifyZeroInteractions(mockSubmissionRepository)
    }

    "return 500 when headers not present" in {
      when(mockDeclarationsApiConnector.submitImportDeclaration(any[String], any[String])(any[HeaderCarrier],any[ExecutionContext]))
        .thenReturn(Future.successful(CustomsDeclarationsResponse(randomConversationId)))

      val result = route(app, fakeXmlRequest).value
      status(result) must be(INTERNAL_SERVER_ERROR)
      verifyZeroInteractions(mockSubmissionRepository)

    }

  }

}
