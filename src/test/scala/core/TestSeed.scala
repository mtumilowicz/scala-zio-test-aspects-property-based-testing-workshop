package core

import zio.System.env
import zio.{UIO, ZIO}


object TestSeed {

  // seed should be stable in one job, if job fails for some seed it should fail always (no retries until success)
  val ciJobId: ZIO[Any, Option[SecurityException], Long] = env("TRAVIS_BUILD_ID").some.map(_.toLong)
  val randomSeed: UIO[Long] = ZIO.succeed(scala.util.Random.nextLong())
  val seed: UIO[Long] = ciJobId.orElse(randomSeed)

}
