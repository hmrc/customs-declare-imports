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

import java.util

import org.mockito.ArgumentCaptor
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

class NotificationControllerSpec extends CustomsImportsBaseSpec with ImportsTestData with MockitoSugar with BeforeAndAfterEach {

  val saveUri = "/notify"
  val bearerToken = "Bearer DummyBearerToken"

  val fakeXmlRequest: FakeRequest[String] = FakeRequest("POST", saveUri)
    .withBody(exampleAcceptNotification("01").toString).withHeaders(CONTENT_TYPE -> ContentTypes.XML(Codec.utf_8), AUTHORIZATION -> bearerToken)

  val fakeXmlRequestWithMultipleNotifications: FakeRequest[String] = FakeRequest("POST", saveUri)
    .withBody(notificationWithMulitpleResponses.toString).withHeaders(CustomsHeaderNames.XConversationIdName -> conversationId,
    CONTENT_TYPE -> ContentTypes.XML(Codec.utf_8), AUTHORIZATION -> bearerToken)


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

  "POST /notify" when {
    "then bearer token is invalid" should {
      "return 401 Unauthorized" in {
        when(mockAppConfig.notificationBearerToken).thenReturn(s"$bearerToken is wrong")
        val result = route(app, fakeXmlRequestWithHeaders).get
        status(result) shouldBe UNAUTHORIZED
      }
    }

    "the bearer token is valid" should {
      "return 200 when notification is received and request is processed" in {
        when(mockAppConfig.notificationBearerToken).thenReturn(bearerToken)
        when(mockImportService.handleNotificationReceived(any[String], any[Int], any[String], any[NodeSeq])).thenReturn(Future.successful(true))

        val result = route(app, fakeXmlRequestWithHeaders).get
        status(result) shouldBe OK
        verify(mockImportService, times(1)).handleNotificationReceived(any[String], any[Int], any[String], any[NodeSeq])
      }

      "return 200 when mulitple notifications are received and request is processed, import service called 3 times" in {
        when(mockAppConfig.notificationBearerToken).thenReturn(bearerToken)
        when(mockImportService.handleNotificationReceived(any[String], any[Int], any[String], any[NodeSeq])).thenReturn(Future.successful(true))

        val mrnCaptor: ArgumentCaptor[String] = ArgumentCaptor.forClass(classOf[String])
        val functionCodeCaptor: ArgumentCaptor[Int] = ArgumentCaptor.forClass(classOf[Int])
        val result = route(app, fakeXmlRequestWithMultipleNotifications).get
        status(result) shouldBe OK
        verify(mockImportService, times(3)).handleNotificationReceived(any[String], functionCodeCaptor.capture(), mrnCaptor.capture(), any[NodeSeq])
        val functionCodes = functionCodeCaptor.getAllValues
        functionCodes.get(0) shouldBe 1
        functionCodes.get(1) shouldBe 13
        functionCodes.get(2) shouldBe 9

        mrnCaptor.getAllValues.get(0) shouldBe "18GBJCM3USAFD2WD51"
        mrnCaptor.getAllValues.get(1) shouldBe "18GBJCM3USAFDHGJGJHG1"
        mrnCaptor.getAllValues.get(2) shouldBe "18GBJCM3USADGHDGHD"
      }

      "return 200 when notification is received and request cannot be processed (import service returns false)" in {
        when(mockAppConfig.notificationBearerToken).thenReturn(bearerToken)
        when(mockImportService.handleNotificationReceived(any[String], any[Int], any[String], any[NodeSeq])).thenReturn(Future.successful(false))

        val result = route(app, fakeXmlRequestWithHeaders).get
        status(result) shouldBe OK
        verify(mockImportService, times(1)).handleNotificationReceived(any[String], any[Int], any[String], any[NodeSeq])
      }

      "return 200 when notification is received and request doesn't contain a conversation Id)" in {
        when(mockAppConfig.notificationBearerToken).thenReturn(bearerToken)

        val result = route(app, fakeXmlRequestWithNoConversationId).get
        status(result) shouldBe OK
        verifyZeroInteractions(mockImportService)
      }

      "return 200 when notification is received and request doesn't contain a parsable FunctionCode)" in {
        when(mockAppConfig.notificationBearerToken).thenReturn(bearerToken)

        val result = route(app, fakeXmlRequestInvalidFunctionCode).get
        status(result) shouldBe OK
        verifyZeroInteractions(mockImportService)
      }

      "return 200 when invalidXMl is sent" in {
        when(mockAppConfig.notificationBearerToken).thenReturn(bearerToken)

        val result = route(app, fakeinvalidRequestWithHeaders).get
        status(result) shouldBe OK
        verifyZeroInteractions(mockImportService)
      }

      "return 200 when headers not present" in {
        when(mockAppConfig.notificationBearerToken).thenReturn(bearerToken)

        val result = route(app, fakeXmlRequest).get
        status(result) shouldBe OK
        verifyZeroInteractions(mockImportService)
      }

    }

  }

}
