package generators

import app.{Account, AccountId, AccountStatus}
import zio.test.Gen
import zio.test.magnolia.DeriveGen
import RefinedTypesGenerators._

object AccountGenerators {

  val genAccount: Gen[Any, Account] = DeriveGen[Account]

  val genAccountStatus: Gen[Any, AccountStatus] = Gen.fromIterable(AccountStatus.values)

  val genAccount2: Gen[Any, Account] =
    (Gen.uuid <*>
      genAccountStatus <*>
      genNonEmptyString
      ).map { case (uuid, status, str) => Account(AccountId(uuid), status, str)
    }
}
