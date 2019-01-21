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

package uk.gov.hmrc.customs.imports.models

import play.api.libs.json._
import reactivemongo.bson.BSONObjectID
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats.mongoEntity



case class Submission(
  eori: String,
  conversationId: String,
  lrn: String,
  mrn: Option[String] = None,
  submittedTimestamp: Long = System.currentTimeMillis(),
  id: BSONObjectID = BSONObjectID.generate()
)

object Submission {
  implicit val objectIdFormats: Format[BSONObjectID] = ReactiveMongoFormats.objectIdFormats
  implicit val formats: Format[Submission] = mongoEntity {
    Json.format[Submission]
  }
}

case class SubmissionData(
  eori: String,
  conversationId: String,
  lrn: String,
  mrn: Option[String],
  submittedTimestamp: Long
)

object SubmissionData {
  implicit val format: OFormat[SubmissionData] = Json.format[SubmissionData]

  def buildSubmissionData(submission: Submission, noOfNotifications: Int): SubmissionData =
    SubmissionData(
      eori = submission.eori,
      conversationId = submission.conversationId,
      lrn = submission.lrn,
      mrn = submission.mrn,
      submittedTimestamp = submission.submittedTimestamp
    )
}

case class SubmitDeclarationResponse(eori: String, conversationId: String, ducr: String, lrn: Option[String] = None, mrn: Option[String] = None)

object SubmitDeclarationResponse {
  implicit val formats: OFormat[SubmitDeclarationResponse] = Json.format[SubmitDeclarationResponse]
}

case class ImportsResponse(status: Int, message: String)

object ImportsResponse {
  implicit val formats: OFormat[ImportsResponse] = Json.format[ImportsResponse]
}
