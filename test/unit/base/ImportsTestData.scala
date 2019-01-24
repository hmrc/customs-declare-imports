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

package unit.base

import java.util.UUID

import org.joda.time.DateTime
import play.api.http.{ContentTypes, HeaderNames}
import play.api.http.HeaderNames.{ACCEPT, CONTENT_TYPE}
import play.api.mvc.Codec
import uk.gov.hmrc.customs.imports.models._
import uk.gov.hmrc.http.NotFoundException
import uk.gov.hmrc.wco.dec.{Declaration, MetaData, Response}
import uk.gov.hmrc.customs.imports.controllers.CustomsHeaderNames._

import scala.util.Random

trait ImportsTestData {
  /*
    The first time an declaration is submitted, we save it with the user's EORI, their LRN (if provided)
    and the conversation ID we received from the customs-declarations API response, generating a timestamp to record
    when this occurred.
   */
  val SIXTEEN = 16
  val EIGHT = 8
  val SEVENTY = 70
  val eori: String = randomString(EIGHT)
  val lrn: String = randomString(SEVENTY)
  val mrn: String = randomString(SIXTEEN)
  val conversationId: String = UUID.randomUUID.toString
  val ducr: String = randomString(SIXTEEN)

  val before: Long = System.currentTimeMillis()
  val submission = Submission(eori, conversationId, lrn, Some(mrn))
  val submissionData: SubmissionData = SubmissionData.buildSubmissionData(submission, 0)
  val seqSubmissions: Seq[Submission] = Seq(submission)
  val seqSubmissionData: Seq[SubmissionData] = Seq(submissionData)

  val now: DateTime = DateTime.now
  val response1: Seq[Response] = Seq(Response(functionCode = Random.nextInt(), functionalReferenceId = Some("123")))
  val response2: Seq[Response] = Seq(Response(functionCode = Random.nextInt(), functionalReferenceId = Some("456")))

  val notification = DeclarationNotification(now, conversationId, eori, None, DeclarationMetadata(), response1)

  val submissionResponse = SubmitDeclarationResponse(eori, conversationId, lrn, Some(mrn))

  val declarantEoriValue: String = "ZZ123456789000"
  val declarantLrnValue: String = "MyLrnValue1234"
  val devclientId = "123786"

  val declarationApiVersion ="1.0"
  val dummyToken = "Bearer BXQ3/Treo4kQCZvVcCqKPlwxRN4RA9Mb5RF8fFxOuwG5WSg+S+Rsp9Nq998Fgg0HeNLXL7NGwEAIzwM6vuA6YYhRQnTRFaBhrp+1w+kVW8g1qHGLYO48QPWuxdM87VMCZqxnCuDoNxVn76vwfgtpNj0+NwfzXV2Zc12L2QGgF9H9KwIkeIPK/mMlBESjue4V]"
  val Valid_X_EORI_IDENTIFIER_HEADER: (String, String) = XEoriIdentifierHeaderName -> declarantEoriValue
  val Valid_LRN_HEADER: (String, String) = XLrnHeaderName -> declarantLrnValue
  val Valid_AUTHORIZATION_HEADER = HeaderNames.AUTHORIZATION -> dummyToken
  val XClientIdHeader: (String, String) = XClientIdName -> devclientId
  val acceptHeader: (String, String) = ACCEPT -> s"application/vnd.hmrc.$declarationApiVersion+xml"
  val contentTypeHeader: (String, String) = CONTENT_TYPE -> ContentTypes.XML(Codec.utf_8)
  val httpException = new NotFoundException("Emulated 404 response from a web call")

  val ValidHeaders: Map[String, String] = Map(
    contentTypeHeader,
    Valid_AUTHORIZATION_HEADER,
    Valid_X_EORI_IDENTIFIER_HEADER,
    Valid_LRN_HEADER
  )

  val ValidAPIResponseHeaders: Map[String, String] = Map(
    XClientIdHeader,
    acceptHeader,
    contentTypeHeader,
    Valid_X_EORI_IDENTIFIER_HEADER
  )

  def randomSubmitDeclaration: MetaData = MetaData(declaration = Option(Declaration(
    functionalReferenceId = Some(randomString(35))
  )))

  def randomConversationId: String = UUID.randomUUID().toString

  protected def randomString(length: Int): String = Random.alphanumeric.take(length).mkString
}
