package unit.services

import org.mockito.Mockito._
import org.mockito.ArgumentMatchers._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import reactivemongo.api.commands.WriteResult
import uk.gov.hmrc.customs.imports.models.Submission
import uk.gov.hmrc.customs.imports.repositories.SubmissionRepository
import uk.gov.hmrc.customs.imports.services.ImportService
import uk.gov.hmrc.play.test.UnitSpec
import unit.base.ImportsTestData

import scala.concurrent.Future

class ImportServiceSpec extends MockitoSugar with UnitSpec with ScalaFutures with ImportsTestData{

  trait SetUp {
    val mockSubmissionRepo = mock[SubmissionRepository]
    val testObj = new ImportService(mockSubmissionRepo)
  }

  "ImportService" should {
    "save submission Data in repository" in new SetUp() {
      val mockWriteResult = mock[WriteResult]
      when(mockSubmissionRepo.save(any[Submission])).thenReturn(Future.successful(mockWriteResult))

      testObj.handleDeclarationSubmit(submission)
    }
  }
}
