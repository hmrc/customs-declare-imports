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

package uk.gov.hmrc.customs.imports.controllers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import play.api.http.ContentTypes
import play.api.mvc.Codec
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.customs.imports.base.{CustomsImportsBaseSpec, ImportsTestData}
import uk.gov.hmrc.customs.imports.connectors.CustomsDeclarationsResponse
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class SubmissionControllerSpec extends CustomsImportsBaseSpec with ImportsTestData with MockitoSugar {
  val saveUri = "/declaration"

  val xmlBody: String =  randomSubmitDeclaration.toXml

  val fakeXmlRequest: FakeRequest[String] = FakeRequest("POST", saveUri)
    .withBody(xmlBody)

  val fakeXmlRequestWithHeaders: FakeRequest[String] = fakeXmlRequest
    .withHeaders(CustomsHeaderNames.XEoriIdentifierHeaderName -> "123dslihuih",
      CustomsHeaderNames.XLrnHeaderName -> "ohkjhkjhkjhk",
      CONTENT_TYPE -> ContentTypes.XML(Codec.utf_8))


  val fakeNonXmlRequestWithHeaders: FakeRequest[String] = FakeRequest("POST", saveUri)
    .withBody("SOMEUNKNOWNTEXTNOTXML")
    .withHeaders(CustomsHeaderNames.XEoriIdentifierHeaderName -> "123dslihuih",
      CustomsHeaderNames.XLrnHeaderName -> "ohkjhkjhkjhk",
      CONTENT_TYPE -> ContentTypes.XML(Codec.utf_8))



  "POST /declaration " should {

      "return 200 when xml request is processed" in {
        when(mockDeclarationsApiConnector.submitImportDeclaration(any[String], any[String])(any[HeaderCarrier],any[ExecutionContext]))
          .thenReturn(Future.successful(CustomsDeclarationsResponse(randomConversationId)))
      val result = route(app, fakeXmlRequestWithHeaders).value
      status(result) must be(OK)
    }

    "return 500 when something goes wrong" in {
      when(mockDeclarationsApiConnector.submitImportDeclaration(any[String], any[String])(any[HeaderCarrier],any[ExecutionContext]))
        .thenReturn(Future.failed(httpException))

      val result = route(app, fakeXmlRequestWithHeaders).value
      status(result) must be(INTERNAL_SERVER_ERROR)

    }

    "return 400 when nonXMl is sent" in {
      when(mockDeclarationsApiConnector.submitImportDeclaration(any[String], any[String])(any[HeaderCarrier],any[ExecutionContext]))
        .thenReturn(Future.successful(CustomsDeclarationsResponse(randomConversationId)))

      val result = route(app, fakeNonXmlRequestWithHeaders).value
      status(result) must be(BAD_REQUEST)

    }

    "return 500 when headers not present" in {
      when(mockDeclarationsApiConnector.submitImportDeclaration(any[String], any[String])(any[HeaderCarrier],any[ExecutionContext]))
        .thenReturn(Future.successful(CustomsDeclarationsResponse(randomConversationId)))

      val result = route(app, fakeXmlRequest).value
      status(result) must be(INTERNAL_SERVER_ERROR)

    }
  }


//  "GET submissions using eori number" should {
//    "return 200 with submission response body" in {
//      withAuthorizedUser()
//      withSubmissions(seqSubmissions)
//      withNotification(None)
//
//      val result = route(app, FakeRequest("GET", "/submissions")).value
//
//      status(result) must be(OK)
//      contentAsJson(result) must be(jsonSeqSubmission)
//    }
//
//    "return 200 without submission response" in {
//      withAuthorizedUser()
//      withSubmissions(Seq.empty)
//      withNotification(None)
//
//      val result = route(app, FakeRequest("GET", "/submissions")).value
//
//      status(result) must be(OK)
//    }
//  }
}
