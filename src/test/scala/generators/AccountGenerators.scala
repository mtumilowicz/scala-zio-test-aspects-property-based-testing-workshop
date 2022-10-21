package generators

import app.Account
import zio.test.Gen
import zio.test.magnolia.DeriveGen
import RefinedTypesGenerators._

object AccountGenerators {
  val genAccount: Gen[Any, Account] = DeriveGen[Account]
}
