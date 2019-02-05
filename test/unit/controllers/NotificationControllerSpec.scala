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

import org.mockito.Mockito._
import org.mockito.ArgumentMatchers.any
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import play.api.http.ContentTypes
import play.api.mvc.Codec
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.customs.imports.controllers.CustomsHeaderNames
import unit.base.{CustomsImportsBaseSpec, ImportsTestData}

import scala.concurrent.Future
import scala.xml.NodeSeq

class NotificationControllerSpec extends CustomsImportsBaseSpec with ImportsTestData with MockitoSugar with BeforeAndAfterEach{

  val saveUri = "/notify"



  val fakeXmlRequest: FakeRequest[String] = FakeRequest("POST", saveUri)
    .withBody(exampleAcceptNotification("01").toString).withHeaders(CONTENT_TYPE -> ContentTypes.XML(Codec.utf_8))


  val fakeXmlRequestWithHeaders: FakeRequest[String] = fakeXmlRequest
    .withHeaders(CustomsHeaderNames.XConversationIdName -> conversationId,
  CONTENT_TYPE -> ContentTypes.XML(Codec.utf_8))

  val fakeXmlRequestInvalidFunctionCode: FakeRequest[String] = fakeXmlRequestWithHeaders
    .withBody(exampleAcceptNotification("NAN").toString)


  val fakeXmlRequestWithNoConversationId: FakeRequest[String] = fakeXmlRequest
    .withHeaders(CONTENT_TYPE -> ContentTypes.XML(Codec.utf_8))


  val fakeinvalidRequestWithHeaders: FakeRequest[String] = FakeRequest("POST", saveUri)
    .withBody("<somerubbish></somerubbish>")
    .withHeaders(CustomsHeaderNames.XConversationIdName -> conversationId,
      CONTENT_TYPE -> ContentTypes.JSON)

  override def beforeEach() {
    reset(mockImportService)
  }

  "POST /notify " should {

      "return 200 when notification is received and request is processed" in {
        when(mockImportService.handleNotificationReceived(any[String], any[Int], any[String], any[NodeSeq])).thenReturn(Future.successful(true))

        val result = route(app, fakeXmlRequestWithHeaders).get
          status(result) shouldBe OK
        verify(mockImportService, times(1)).handleNotificationReceived(any[String], any[Int], any[String], any[NodeSeq])
      }

    "return 200 when notification is received and request cannot be processed (import service returns false)" in {
      when(mockImportService.handleNotificationReceived(any[String], any[Int], any[String], any[NodeSeq])).thenReturn(Future.successful(false))

      val result = route(app, fakeXmlRequestWithHeaders).get
      status(result) shouldBe OK
      verify(mockImportService, times(1)).handleNotificationReceived(any[String], any[Int], any[String], any[NodeSeq])
    }

    "return 200 when notification is received and request doesn't contain a conversation Id)" in {

      val result = route(app, fakeXmlRequestWithNoConversationId).get
      status(result) shouldBe OK
      verifyZeroInteractions(mockImportService)
    }

    "return 200 when notification is received and request doesn't contain a parsable FunctionCode)" in {

      val result = route(app, fakeXmlRequestInvalidFunctionCode).get
      status(result) shouldBe OK
      verifyZeroInteractions(mockImportService)
    }

    "return 200 when invalidXMl is sent" in {

      val result = route(app, fakeinvalidRequestWithHeaders).get
      status(result) shouldBe OK
      verifyZeroInteractions(mockImportService)
    }

    "return 200 when headers not present" in {

      val result = route(app, fakeXmlRequest).get
      status(result) shouldBe OK
      verifyZeroInteractions(mockImportService)
    }




  }

}
