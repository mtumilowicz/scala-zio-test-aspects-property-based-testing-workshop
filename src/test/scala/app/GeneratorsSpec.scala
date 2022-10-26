package app

import core.BaseSpec
import generators.AccountGenerators.genAccount
import generators.ContributorGenerators._
import zio.Scope
import zio.test.Assertion.{equalTo, isOneOf}
import zio.test._

object GeneratorsSpec extends BaseSpec {

  override def spec: Spec[TestEnvironment with Scope, Any] = suite("suite")(
    test("create account test") {
      check(genAccount) { account =>
        assertZIO(AccountService.createAccount(account))(equalTo(AccountCreated(account.id)))
      }
    },
    test("verify if repositories are from file") {
      check(genRepositoriesFromFile) { testData =>
        assert(testData.name)(isOneOf(Set("zio/zio", "ghostdogpr/caliban", "zio/zio-kafka")))
      }
    },
  )
}
