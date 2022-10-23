package generators

import app.{Account, AccountId, AccountStatus}
import zio.test.Gen
import zio.test.magnolia.DeriveGen
import RefinedTypesGenerators._
import eu.timepit.refined.types.string.NonEmptyString

object AccountGenerators {
  val genAccount: Gen[Any, Account] = DeriveGen[Account]

  val genAccount2: Gen[Any, Account] =
    (Gen.uuid <*>
      Gen.fromIterable(AccountStatus.values) <*>
      Gen.string1(Gen.char)
      ).map { case (uuid, status, str) => Account(AccountId(uuid), status, NonEmptyString.unsafeFrom(str))
    }
}
