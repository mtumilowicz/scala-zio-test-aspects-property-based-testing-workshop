package generators

import eu.timepit.refined.types.string.NonEmptyString
import zio.test.Gen
import zio.test.magnolia.DeriveGen

object RefinedTypesGenerators {

  val genNonEmptyString: Gen[Any, NonEmptyString] =
    Gen.stringBounded(1, 10)(Gen.char)
    .map(NonEmptyString.unsafeFrom)
  implicit val deriveGenNonEmptyString = DeriveGen.instance(genNonEmptyString)

}
