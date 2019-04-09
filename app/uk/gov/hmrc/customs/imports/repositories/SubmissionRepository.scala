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
import play.api.libs.json._
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.commands.Command.CommandWithPackRunner
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.api.{Cursor, FailoverStrategy, ReadPreference}
import reactivemongo.bson.BSONObjectID
import reactivemongo.play.json.{JSONSerializationPack, _}
import uk.gov.hmrc.customs.imports.models.{Declaration, Submission}
import uk.gov.hmrc.mongo.ReactiveRepository
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats.objectIdFormats

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SubmissionRepository @Inject()(implicit mc: ReactiveMongoComponent, ec: ExecutionContext)
  extends ReactiveRepository[Submission, BSONObjectID](
    "submissions",
    mc.mongoConnector.db,
    Submission.formats,
    objectIdFormats
  ) {

  override def indexes: Seq[Index] = Seq(
  Index(Seq("eori" -> IndexType.Ascending), name = Some("eoriIdx")),
  Index(Seq("localReferenceNumber" -> IndexType.Ascending), name = Some("lrnIdx"))
  )

  def deleteById(id: BSONObjectID) : Future[Boolean] = {
    removeById(id).map(_.ok)
  }

  def getByEoriAndLrn(eori: String, localReferenceNumber: String):  Future[Option[Submission]] =
    find("eori" -> JsString(eori), "localReferenceNumber" -> JsString(localReferenceNumber)).map(_.headOption)

  def findByEori(eori: String): Future[Seq[Declaration]] = {
    import this.collection.BatchCommands.AggregationFramework.{Descending, Filter, First, Group, Lookup, Match, Project, Push, Sort, Unwind}

    collection.aggregatorContext[Declaration](
      Match(Json.obj("eori" -> eori)),
      List(Lookup(from = "submissionActions",
        localField = "_id",
        foreignField = "submissionId",
        as = "actions"),
        Unwind(path = "actions",
          includeArrayIndex = None,
          preserveNullAndEmptyArrays = Some(true)),
        Lookup(
            from = "submissionsNotifications",
            localField = "actions.conversationId",
            foreignField = "conversationId",
            as = "actions.notifications"),
        Sort(Descending("submittedDateTime")),
        Group(Json.toJson("_id"))("eori" -> First(Json.toJson("$eori")),
          "localReferenceNumber" -> First(Json.toJson("$localReferenceNumber")),
          "mrn" -> First(Json.toJson("$mrn")),
          "submittedDateTime" -> First(Json.toJson("$submittedDateTime")),
          "actions" -> Push(Json.toJson("$actions"))),
        Project(Json.obj("_id" -> 1,
            "eori" -> 1,
            "localReferenceNumber" -> 1,
            "mrn" -> 1,
            "submittedDateTime" -> 1,
            "actions" -> Filter(Json.toJson("$actions"), "a", Json.obj("$ifNull" -> Json.arr("$$a._id", false)))))),
      allowDiskUse = true)
    .prepared.cursor.collect[Seq](-1, Cursor.FailOnError[Seq[Declaration]]())
  }

  def getByEoriAndMrn(eori: String, mrn: String): Future[Option[Submission]] =
    find("eori" -> JsString(eori), "mrn" -> JsString(mrn)).map(_.headOption)

  def updateSubmission(submission: Submission) = {
    val finder = Json.obj("eori" -> submission.eori, "localReferenceNumber" -> submission.localReferenceNumber)

    val modifier = Json.obj(
      "$set" ->
        Json.obj("mrn" -> submission.mrn)
    )
    findAndUpdate(finder, modifier, upsert = true)
      .map(_.lastError.exists(_.updatedExisting))
  }


}
