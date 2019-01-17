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

import play.api.libs.json.{JsValue, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.customs.imports.base.{CustomsImportsBaseSpec, ImportsTestData}
import uk.gov.hmrc.customs.imports.models.{Submission, SubmissionData, SubmissionResponse}

class SubmissionControllerSpec extends CustomsImportsBaseSpec with ImportsTestData {
  val saveUri = "/submit-declaration"

  val jsonBody: JsValue = Json.toJson[SubmissionResponse](submissionResponse)

  val fakeRequest: FakeRequest[JsValue] = FakeRequest("POST", saveUri).withBody(jsonBody)

  val submissionJson: JsValue = Json.toJson[Submission](submission)
  val jsonSeqSubmission: JsValue = Json.toJson[Seq[SubmissionData]](seqSubmissionData)

  "POST /submit-declaration " should {
    "return 200 when submission has been saved" in {
      withAuthorizedUser()
      withDataSaved(true)

      val result = route(app, fakeRequest).value

      status(result) must be(OK)
    }

    "return 500 when something goes wrong" in {
      withAuthorizedUser()
      withDataSaved(false)

      val failedResult = route(app, fakeRequest).value

      status(failedResult) must be(INTERNAL_SERVER_ERROR)
    }
  }


  "GET submissions using eori number" should {
    "return 200 with submission response body" in {
      withAuthorizedUser()
      withSubmissions(seqSubmissions)
      withNotification(None)

      val result = route(app, FakeRequest("GET", "/submissions")).value

      status(result) must be(OK)
      contentAsJson(result) must be(jsonSeqSubmission)
    }

    "return 200 without submission response" in {
      withAuthorizedUser()
      withSubmissions(Seq.empty)
      withNotification(None)

      val result = route(app, FakeRequest("GET", "/submissions")).value

      status(result) must be(OK)
    }
  }
}
