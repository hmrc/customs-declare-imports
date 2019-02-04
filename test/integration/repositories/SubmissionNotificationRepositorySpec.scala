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
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.concurrent.Execution.Implicits
import play.api.libs.json.JsString
import uk.gov.hmrc.customs.imports.models.SubmissionNotification
import uk.gov.hmrc.customs.imports.repositories.SubmissionNotificationRepository
import uk.gov.hmrc.play.test.UnitSpec
import unit.base.ImportsTestData

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}
import scala.reflect.ClassTag

class SubmissionNotificationRepositorySpec extends UnitSpec with GuiceOneAppPerSuite with BeforeAndAfterEach
  with ScalaFutures with ImportsTestData {

  override protected def afterEach(): Unit = {
    super.afterEach()
    Await.result(repo.removeAll(), 1 second)
  }

  protected def component[T: ClassTag]: T = app.injector.instanceOf[T]

  implicit val mat: Materializer = app.materializer

  implicit val ec: ExecutionContext = Implicits.defaultContext

  implicit lazy val patience: PatienceConfig =
    PatienceConfig(timeout = 5.seconds, interval = 50.milliseconds) // be more patient than the default


  override lazy val app: Application = GuiceApplicationBuilder().build()

  val repo: SubmissionNotificationRepository = component[SubmissionNotificationRepository]

  "NotificationsRepository" should {
    "save notification with functionCode, conversationId and timestamp" in {
      repo.insert(submissionNotification).futureValue.ok shouldBe true

      val foundAction: Seq[SubmissionNotification] = await(repo.find("conversationId" -> JsString(conversationId)))

      foundAction.size shouldBe 1
      foundAction.head.conversationId shouldBe conversationId
      foundAction.head.functionCode shouldBe functionCodeACK
      foundAction.head.dateTimeIssued should (be >= before).and(be <= System.currentTimeMillis())
    }
  }
}
