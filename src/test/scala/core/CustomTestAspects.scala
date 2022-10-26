package core

import zio.test.{TestAspect, TestAspectAtLeastR, TestEnvironment, TestFailure, TestRandom, TestSuccess}
import zio.{Trace, UIO, ZIO}


object CustomTestAspects {
  def printSeedIfFailure: TestAspectAtLeastR[TestEnvironment] =
    new TestAspect.PerTest.AtLeastR[TestEnvironment] {
      override def perTest[R >: Nothing <: TestEnvironment, E >: Nothing <: Any]
      (test: ZIO[R, TestFailure[E], TestSuccess])(implicit trace: Trace): ZIO[R, TestFailure[E], TestSuccess] = for {
        seed <- TestRandom.getSeed
        result <- test.tapError(_ => zio.Console.printLine(s"Test failed with seed = $seed").orDie)
      } yield result
    }

  def setSeedBeforeEach(seed: UIO[Long]): TestAspect[Nothing, Any, Nothing, Any] =
    TestAspect.before(seed.flatMap(TestRandom.setSeed(_)))
}