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

package integration.repositories

import org.scalatest.BeforeAndAfterEach
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.customs.imports.models.Submission
import uk.gov.hmrc.customs.imports.repositories.SubmissionRepository
import unit.base.{CustomsImportsBaseSpec, ImportsTestData}

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class SubmissionRepositorySpec extends CustomsImportsBaseSpec with BeforeAndAfterEach
  with ImportsTestData {

  override protected def afterEach(): Unit = {
    super.afterEach()
    Await.result(repo.removeAll(), 1 second)
  }

  override lazy val app: Application = GuiceApplicationBuilder().build()
  val repo: SubmissionRepository = component[SubmissionRepository]

  "SubmissionRepository" should {
    "save declaration return true and persisted submission is correct" in {
        repo.save(submission).futureValue.ok must be(true)

        // we can now display a list of all the declarations belonging to the current user, searching by EORI
        val foundSubmission = repo.findByEori(eori).futureValue
        foundSubmission.length must be(1)
        foundSubmission.head.eori must be(eori)
        foundSubmission.head.mrn must be(Some(mrn))

        // a timestamp has been generated representing "creation time" of case class instance
        foundSubmission.head.submittedTimestamp must (be >= before).and(be <= System.currentTimeMillis())

    }

    "findByEori returns the correct persisted submission " in {

      val differentSubmission = Submission(eori + 123, lrn + 123, Some(mrn + 123 ))

      repo.save(submission).futureValue.ok must be(true)

      repo.save(differentSubmission).futureValue.ok must be(true)

      val foundSubmission = repo.findByEori(eori).futureValue
      foundSubmission.length must be(1)
      foundSubmission.head.eori must be(eori)
      foundSubmission.head.mrn must be(Some(mrn))

    }

    "findByEori returns empty Seq when it cannot find a matching submission" in {

      val result = repo.findByEori("someRandomString").futureValue
      result.isEmpty must be(true)
    }

    "getByEoriAndMrn returns the correct persisted submission " in {

      repo.save(submission).futureValue.ok must be(true)

      // or we can retrieve it by eori and MRN
      val submission2 = repo.getByEoriAndMrn(eori, mrn).futureValue.value
      submission2.eori must be(eori)
      submission2.mrn must be(Some(mrn))
    }

    "updateSubmission updates the submission when called" in {
      repo.save(submission).futureValue.ok must be(true)
      // update status test
      val submissionToUpdate = repo.getByEoriAndMrn(eori, mrn).futureValue.value
      val updatedSubmission = submissionToUpdate.copy(mrn = Some(mrn + 123))

      repo.updateSubmission(updatedSubmission).futureValue must be(true)

      val newSubmission = repo.getByEoriAndMrn(eori, mrn + 123).futureValue.value

      newSubmission must be(updatedSubmission)
    }

    // TODO add unhappy paths for save, update and getBy[X]
    // the findBy Test is flaky and fails with duplicate key error
    //The future returned an exception of type: reactivemongo.api.commands.LastError, with message: DatabaseException['E11000 duplicate key error collection: customs-declare-imports.submissions index: _id_ dup key: { : ObjectId('5c50544501000001002b097d') }' (code = 11000)].
    //ScalaTestFailureLocation: integration.repositories.SubmissionRepositorySpec$$anonfun$1$$anonfun$apply$mcV$sp$2 at (SubmissionRepositorySpec.scala:58)
    //org.scalatest.exceptions.TestFailedException: The future returned an exception of type: reactivemongo.api.commands.LastError, with message: DatabaseException['E11000 duplicate key error collection: customs-declare-imports.submissions index: _id_ dup key: { : ObjectId('5c50544501000001002b097d') }' (code = 11000)].
  }
}
