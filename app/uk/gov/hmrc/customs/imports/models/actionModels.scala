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

import play.api.mvc.{Request, WrappedRequest}


trait HasLocalReferenceNumber {
  val localReferenceNumber: LocalReferenceNumber
}

trait HasEori {
  val eori: Eori
}

case class LocalReferenceNumber(value: String) extends AnyVal

case class Eori(value: String) extends AnyVal



case class ValidatedHeadersSubmissionRequest(localReferenceNumber: LocalReferenceNumber) extends HasLocalReferenceNumber

case class AuthorizedImportSubmissionRequest[A](eori: Eori, request: Request[A]) extends WrappedRequest[A](request) with HasEori

case class ValidatedNotificationRequest(functionCode: Int, mrn: String)
