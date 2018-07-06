package com.spotify.noether

import org.scalactic.{Equality, TolerantNumerics}

class MeanAveragePrecisionTest extends AggregatorTest {
  import RankingData._

  private implicit val doubleEq: Equality[Double] = TolerantNumerics.tolerantDoubleEquality(0.1)

  it should "compute map for rankings" in {
    assert(run(MeanAveragePrecision[Int]())(rankingData) === 0.355026)
  }
}
