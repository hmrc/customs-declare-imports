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

package uk.gov.hmrc.customs.imports.base

import java.util.UUID

import org.joda.time.DateTime
import uk.gov.hmrc.customs.imports.models._
import uk.gov.hmrc.wco.dec.Response

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
  val lrn: Option[String] = Some(randomString(SEVENTY))
  val mrn: String = randomString(SIXTEEN)
  val conversationId: String = UUID.randomUUID.toString
  val ducr: String = randomString(SIXTEEN)

  val before: Long = System.currentTimeMillis()
  val submission = Submission(eori, conversationId, ducr, lrn, Some(mrn))
  val submissionData: SubmissionData = SubmissionData.buildSubmissionData(submission, 0)
  val seqSubmissions: Seq[Submission] = Seq(submission)
  val seqSubmissionData: Seq[SubmissionData] = Seq(submissionData)

  val now: DateTime = DateTime.now
  val response1: Seq[Response] = Seq(Response(functionCode = Random.nextInt(), functionalReferenceId = Some("123")))
  val response2: Seq[Response] = Seq(Response(functionCode = Random.nextInt(), functionalReferenceId = Some("456")))

  val notification = DeclarationNotification(now, conversationId, eori, None, DeclarationMetadata(), response1)

  val submissionResponse = SubmissionResponse(eori, conversationId, ducr, lrn, Some(mrn))

  protected def randomString(length: Int): String = Random.alphanumeric.take(length).mkString
}
