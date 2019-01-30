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

package uk.gov.hmrc.customs.imports.repositories

import org.scalatest.BeforeAndAfterEach
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import unit.base.{CustomsImportsBaseSpec, ImportsTestData}

import scala.concurrent.ExecutionContext.Implicits.global

class NotificationsRepositorySpec extends CustomsImportsBaseSpec with BeforeAndAfterEach with ImportsTestData {

  override protected def afterEach(): Unit = {
    super.afterEach()
    repo.removeAll()
  }

  override lazy val app: Application = GuiceApplicationBuilder().build()

  val repo: NotificationsRepository = component[NotificationsRepository]

  "NotificationsRepository" should {
    "save notification with eori, conversationId and timestamp" in {
      repo.save(notification).futureValue shouldBe true

      // we can now display a list of all the declarations belonging to the current user, searching by EORI
      val foundDeclarationNotification = repo.findByEori(eori).futureValue
      foundDeclarationNotification.length shouldBe 1
      foundDeclarationNotification.head.eori shouldBe eori
      foundDeclarationNotification.head.conversationId shouldBe conversationId

      foundDeclarationNotification.head.dateTimeReceived shouldBe now

      // we can also retrieve the submission individually by conversation Id
      val declarationNotification1 = await(repo.getByConversationId(conversationId)).get
      declarationNotification1.eori shouldBe eori
      declarationNotification1.conversationId shouldBe conversationId

      // or we can retrieve it by eori and conversationId
      val declarationNotification2 = await(repo.getByEoriAndConversationId(eori, conversationId)).get
      declarationNotification2.eori shouldBe eori
      declarationNotification2.conversationId shouldBe conversationId
    }
  }
}
