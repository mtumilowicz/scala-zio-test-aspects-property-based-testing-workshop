package generators

import app.contributors.{ContributorService, Repository}
import zio.test.Gen

object ContributorGenerators {
  val genRepositoriesFromFile: Gen[Any, Repository] = Gen.fromZIO {
    for {
      chunk <- ContributorService.repositories("src/test/resources/contributors.txt").runCollect.orDie
    } yield Gen.fromIterable(chunk)
  }.flatten
}
