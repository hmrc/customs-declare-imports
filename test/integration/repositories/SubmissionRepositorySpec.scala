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

import akka.stream.Materializer
import java.util.UUID
import org.joda.time.DateTime
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.concurrent.Execution.Implicits
import uk.gov.hmrc.customs.imports.models._
import uk.gov.hmrc.customs.imports.repositories._
import uk.gov.hmrc.play.test.UnitSpec
import unit.base.ImportsTestData

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.reflect.ClassTag

class SubmissionRepositorySpec extends UnitSpec with BeforeAndAfterEach
  with ImportsTestData with GuiceOneAppPerSuite with MockitoSugar with ScalaFutures{

  override protected def afterEach(): Unit = {
    super.afterEach()
    await(repo.removeAll())
    await(actionRepo.removeAll())
    await(submissionNotificationRepository.removeAll())
  }

  protected def component[T: ClassTag]: T = app.injector.instanceOf[T]

  override lazy val app: Application = GuiceApplicationBuilder().configure("metrics.enabled" -> false).build()
  val repo: SubmissionRepository = component[SubmissionRepository]
  val actionRepo: SubmissionActionRepository = component[SubmissionActionRepository]
  val submissionNotificationRepository: SubmissionNotificationRepository = component[SubmissionNotificationRepository]

  implicit val mat: Materializer = app.materializer

  implicit val ec: ExecutionContext = Implicits.defaultContext

  implicit lazy val patience: PatienceConfig =
    PatienceConfig(timeout = 5.seconds, interval = 50.milliseconds) // be more patient than the default


  "SubmissionRepository" should {
    "save submission, return true. Persisted submission should be correct" in {
        repo.insert(submission).futureValue.ok shouldBe true

        // we can now display a list of all the declarations belonging to the current user, searching by EORI
        val foundSubmission = repo.findByEori(eori).futureValue
        foundSubmission.length shouldBe 1
        foundSubmission.head.eori shouldBe eori
        foundSubmission.head.mrn shouldBe Some(mrn)

        // a timestamp has been generated representing "creation time" of case class instance
        foundSubmission.head.submittedDateTime.getMillis should (be >= before).and(be <= System.currentTimeMillis())
    }

    "findByEori returns the correct persisted submission " in {

      val differentSubmission = Submission(eori + 123, lrn + 123, Some(mrn + 123 ))

      repo.insert(submission).futureValue.ok shouldBe true

      repo.insert(differentSubmission).futureValue.ok shouldBe true

      val foundSubmission = repo.findByEori(eori).futureValue
      foundSubmission.length shouldBe 1
      foundSubmission.head.eori shouldBe eori
      foundSubmission.head.mrn shouldBe Some(mrn)

    }

    "findByEori returns empty Seq when it cannot find a matching submission" in {

      val result = repo.findByEori("someRandomString").futureValue
      result.isEmpty shouldBe true
    }

    "findByEori returns correct persisted submission with empty actions when there are no submissionActions" in {
      await(repo.insert(submission))

      val foundSubmission = await(repo.findByEori(eori)).head

      foundSubmission.eori shouldBe eori
      foundSubmission.mrn shouldBe Some(mrn)
      foundSubmission.actions shouldBe Seq.empty
    }

    "findByEori returns correct persisted submission with nested actions and notifications when there are submissionActions with associated notifications" in {
      await(repo.insert(submission))

      val action1 = SubmissionAction(submission.id, UUID.randomUUID.toString, SubmissionActionType.SUBMISSION)
      val action2 = SubmissionAction(submission.id, UUID.randomUUID.toString, SubmissionActionType.CANCELLATION)

      val notification1 = SubmissionNotification(1, action1.conversationId)
      val notification2 = SubmissionNotification(2, action1.conversationId)

      await(actionRepo.insert(action1))
      await(actionRepo.insert(action2))
      await(actionRepo.insert(submissionActionDifferentSubmissionId))

      await(submissionNotificationRepository.insert(notification1))
      await(submissionNotificationRepository.insert(notification2))

      val foundSubmission = await(repo.findByEori(eori)).head

      foundSubmission.eori shouldBe eori
      foundSubmission.mrn shouldBe Some(mrn)

      foundSubmission.actions.length shouldBe 2
      foundSubmission.actions.head.dateTimeSent shouldBe new DateTime(action1.dateTimeSent)
      foundSubmission.actions.head.notifications.head.functionCode shouldBe notification1.functionCode
      foundSubmission.actions.head.notifications.head.dateTimeIssued shouldBe new DateTime(notification1.dateTimeIssued)
      foundSubmission.actions.head.notifications.last.functionCode shouldBe notification2.functionCode
      foundSubmission.actions.head.notifications.last.dateTimeIssued shouldBe new DateTime(notification2.dateTimeIssued)

      foundSubmission.actions.last.dateTimeSent shouldBe new DateTime(action2.dateTimeSent)
      foundSubmission.actions.last.notifications shouldBe Seq.empty
    }

    "delete by id returns true when submission exists" in {

      repo.insert(submission).futureValue.ok shouldBe true

      val result = repo.findByEori(eori).futureValue
      result.size shouldBe 1

      val deleteResult = repo.deleteById(submission.id).futureValue
      deleteResult shouldBe true

      val result2 = repo.findByEori(eori).futureValue
      result2.size shouldBe 0
    }

    "getByEoriAndMrn returns the correct persisted submission " in {

      repo.insert(submission).futureValue.ok shouldBe true

      // or we can retrieve it by eori and MRN
      val submission2 = await(repo.getByEoriAndMrn(eori, mrn)).get
      submission2.eori shouldBe eori
      submission2.mrn shouldBe Some(mrn)
    }

    "getByEoriAndLrn returns the correct persisted submission " in {

      repo.insert(submission).futureValue.ok shouldBe true

      // or we can retrieve it by eori and MRN
      val submission2 = await(repo.getByEoriAndLrn(eori, lrn)).get
      submission2.eori shouldBe eori
      submission2.localReferenceNumber shouldBe lrn
    }

    "updateSubmission updates the submission when called" in {
      await(repo.insert(submission)).ok shouldBe true
      // update status test
      val submissionToUpdate = await(repo.getByEoriAndMrn(eori, mrn)).get
      val updatedSubmission = submissionToUpdate.copy(mrn = Some(mrn + 123))

      await(repo.updateSubmission(updatedSubmission))

      val newSubmission = await(repo.getByEoriAndMrn(eori, mrn + 123)).get

      newSubmission shouldBe updatedSubmission
    }

    // TODO add unhappy paths for save, update and getBy[X]
    // the findBy Test is flaky and fails with duplicate key error
    //The future returned an exception of type: reactivemongo.api.commands.LastError, with message: DatabaseException['E11000 duplicate key error collection: customs-declare-imports.submissions index: _id_ dup key: { : ObjectId('5c50544501000001002b097d') }' (code = 11000)].
    //ScalaTestFailureLocation: integration.repositories.SubmissionRepositorySpec$$anonfun$1$$anonfun$apply$mcV$sp$2 at (SubmissionRepositorySpec.scala:58)
    //org.scalatest.exceptions.TestFailedException: The future returned an exception of type: reactivemongo.api.commands.LastError, with message: DatabaseException['E11000 duplicate key error collection: customs-declare-imports.submissions index: _id_ dup key: { : ObjectId('5c50544501000001002b097d') }' (code = 11000)].
  }
}
