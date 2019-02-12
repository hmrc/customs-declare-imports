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

import org.joda.time.DateTime
import play.api.libs.json.{Json, Writes}
import uk.gov.hmrc.customs.imports.models.SubmissionActionType.SubmissionActionType

case class DeclarationNotification(functionCode: Int, conversationId: String, dateTimeIssued: DateTime)

case class DeclarationAction(dateTimeSent: DateTime, actionType: SubmissionActionType, notifications: Seq[DeclarationNotification] = Seq.empty)

case class Declaration(eori: String, localReferenceNumber: String, submittedDateTime: DateTime, mrn: Option[String] = None, actions: Seq[DeclarationAction] = Seq.empty)

object Declaration {
  implicit val dateTimeWrites = Writes.jodaDateWrites("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
  implicit val formatNotification = Json.format[DeclarationNotification]
  implicit val formatAction = Json.format[DeclarationAction]
  implicit val formatDeclaration = Json.format[Declaration]
}
