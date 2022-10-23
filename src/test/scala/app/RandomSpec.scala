package app

import core.MainSpec
import generators.AccountGenerators.genAccount
import zio.Scope
import zio.test.Assertion.equalTo
import zio.test._

object RandomSpec extends MainSpec {

  override def spec: Spec[TestEnvironment with Scope, Any] = suite("suite")(
    test("test1") {
      for {
        _ <- zio.Console.printLine("")
      } yield assertTrue(1 == 1)
    },
    test("test2") {
      for {
        _ <- zio.Console.printLine("")
      } yield assertTrue(2 == 1)
    },
    test("gen") {
      check(genAccount) { account =>
        println(account)
        assertZIO(AccountService.createAccount(account))(equalTo(AccountCreated(account.id)))
      }
    }
  )
}
