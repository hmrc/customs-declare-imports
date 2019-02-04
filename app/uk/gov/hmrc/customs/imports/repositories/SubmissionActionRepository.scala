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

import javax.inject.{Inject, Singleton}
import play.api.libs.json.JsString
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.BSONObjectID
import uk.gov.hmrc.customs.imports.models.{Submission, SubmissionAction}
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats.objectIdFormats
import uk.gov.hmrc.mongo.{AtomicUpdate, ReactiveRepository}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SubmissionActionRepository @Inject()(implicit mc: ReactiveMongoComponent, ec: ExecutionContext)
  extends ReactiveRepository[SubmissionAction, BSONObjectID](
    "submissionActions",
    mc.mongoConnector.db,
    SubmissionAction.formats,
    objectIdFormats
  ) {


  override def indexes: Seq[Index] = Seq(
  Index(Seq("conversationId" -> IndexType.Ascending), unique = true, name = Some("conversationIdx")),
  Index(Seq("submissionId" -> IndexType.Ascending), name = Some("submissionIdx"))
  )

  def findByConversationId(conversationId: String): Future[Option[SubmissionAction]] = {
    find("conversationId" -> JsString(conversationId)).map(_.headOption)
  }

}
