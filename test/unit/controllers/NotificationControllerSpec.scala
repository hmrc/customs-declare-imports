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
import uk.gov.hmrc.http.HeaderCarrier
import unit.base.{CustomsImportsBaseSpec, ImportsTestData}

import scala.concurrent.{ExecutionContext, Future}
import scala.xml.NodeSeq

class NotificationControllerSpec extends CustomsImportsBaseSpec with ImportsTestData with MockitoSugar with BeforeAndAfterEach{

  val saveUri = "/notify"

  val xmlBody: String =  randomSubmitDeclaration.toXml

  val fakeXmlRequest: FakeRequest[String] = FakeRequest("POST", saveUri)
    .withBody(xmlBody).withHeaders(CONTENT_TYPE -> ContentTypes.XML(Codec.utf_8))

  val fakeXmlRequestWithHeaders: FakeRequest[String] = fakeXmlRequest
    .withHeaders(CustomsHeaderNames.XConversationIdName -> conversationId,
      CONTENT_TYPE -> ContentTypes.XML(Codec.utf_8))


  val fakeinvalidRequestWithHeaders: FakeRequest[String] = FakeRequest("POST", saveUri)
    .withBody("<somerubbish></somerubbish>")
    .withHeaders(CustomsHeaderNames.XConversationIdName -> conversationId,
      CONTENT_TYPE -> ContentTypes.JSON)

  override def beforeEach() {
    reset(mockImportService)
  }

  "POST /notify " should {

      "return 200 when notification is received and request is processed" in {

        val result = route(app, fakeXmlRequestWithHeaders).get
          status(result) shouldBe OK
      }

    "return 200 when invalidXMl is sent" in {

      val result = route(app, fakeinvalidRequestWithHeaders).get
      status(result) shouldBe OK

    }

    "return 200 when headers not present" in {

      val result = route(app, fakeXmlRequest).get
      status(result) shouldBe OK


    }




  }

}
