package core

import zio.Chunk
import zio.test.{TestAspect, TestAspectAtLeastR, TestEnvironment, TestRandom, ZIOSpecDefault}

abstract class MainSpec extends ZIOSpecDefault {
  override def aspects: Chunk[TestAspectAtLeastR[TestEnvironment]] = Chunk(
    CustomTestAspects.printSeedIfFailure,
    CustomTestAspects.setSeedBeforeEach(TestSeed.seed)
  )
}
