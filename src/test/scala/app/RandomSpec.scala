package app

import core.MainSpec
import generators.AccountGenerators.genAccount
import generators.ContributorGenerators._
import zio.test.Assertion.equalTo
import zio.test.TestAspect.sequential
import zio.test._
import zio.{Scope, ZIO}

import java.util.concurrent.atomic.AtomicInteger

object RandomSpec extends MainSpec {

  val counter = new AtomicInteger(0)

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
    test("gen2233") {
      check(genAccount, genAccount) { (account, _) =>
        for {
          seed <- TestRandom.getSeed
          _ <- zio.Console.printLine("gen2233 " + seed)
          result <- assertZIO(AccountService.createAccount(account))(equalTo(AccountCreated(account.id)))
        } yield result
      }
    },
    test("gen") {
      check(genAccount) { account =>
        for {
          seed <- TestRandom.getSeed
          _ <- zio.Console.printLine("gen " + seed)
          result <- assertZIO(AccountService.createAccount(account))(equalTo(AccountCreated(account.id)))
        } yield result
      }
    },
    test("gen222") {
      check(genAccount) { account =>
        for {
          seed <- TestRandom.getSeed
          _ <- zio.Console.printLine("gen222 " + seed)
          result <- assertZIO(AccountService.createAccount(account))(equalTo(AccountCreated(account.id)))
        } yield result
      }
    },
    test("gen2") {
      check(genRepositoriesFromFile) { testData =>
        for {
          seed <- TestRandom.getSeed
          _ <- zio.Console.printLine("gen2 " + seed)
        } yield assertTrue(true)
      }
    },
    test("gen3") {
      check(genAccount, genRepositoriesFromFile) { (_, _) =>
        for {
          seed <- TestRandom.getSeed
          _ <- zio.Console.printLine("gen3 " + seed + " " + counter.get())
          _ = counter.incrementAndGet()
          _ <- if (seed > 35633369995042L) ZIO.fail("got it!") else ZIO.succeed(1)
        } yield assertTrue(true)
      }
    }
  ) @@ sequential
}
