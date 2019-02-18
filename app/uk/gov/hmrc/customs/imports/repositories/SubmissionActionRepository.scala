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
import play.api.libs.json.{JsPath, JsString, Json, Reads}
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.{BSONDocument, BSONDocumentWriter, BSONObjectID, BSONString}
import reactivemongo.play.json.ImplicitBSONHandlers._
import uk.gov.hmrc.customs.imports.models.SubmissionAction
import uk.gov.hmrc.customs.imports.models.SubmissionActionType.SubmissionActionType
import uk.gov.hmrc.mongo.ReactiveRepository
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats.objectIdFormats

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SubmissionActionRepository @Inject()(implicit mc: ReactiveMongoComponent, ec: ExecutionContext)
  extends ReactiveRepository[SubmissionAction, BSONObjectID](
    "submissionActions",
    mc.mongoConnector.db,
    SubmissionAction.formats,
    objectIdFormats
  ) {

  def deleteBySubmissionId(submissionId: BSONObjectID): Future[Boolean] = {
    collection.delete().one(Json.obj("submissionId" -> Json.toJson(submissionId))).map(_.ok)
  }

  def getBySubmissionId(submissionId: BSONObjectID): Future[Seq[SubmissionAction]] = {
    collection.find(Json.obj("submissionId" -> Json.toJson(submissionId))).one[SubmissionAction]
      .map(maybeSubmissions => maybeSubmissions.toList)
  }


  override def indexes: Seq[Index] = Seq(
  Index(Seq("conversationId" -> IndexType.Ascending), unique = true, name = Some("conversationIdx")),
  Index(Seq("submissionId" -> IndexType.Ascending), name = Some("submissionIdx"))
  )

  def findByConversationId(conversationId: String): Future[Option[SubmissionAction]] = {
    find("conversationId" -> JsString(conversationId)).map(_.headOption)
  }

}
