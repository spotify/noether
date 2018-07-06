package com.spotify.noether

import org.scalactic.{Equality, TolerantNumerics}

class PrecisionAtKTest extends AggregatorTest {
  import RankingData._
  private implicit val doubleEq: Equality[Double] = TolerantNumerics.tolerantDoubleEquality(0.1)

  it should "compute precisionAtK for rankings" in {
    assert(run(PrecisionAtK[Int](1))(rankingData) === 1.0 / 3)
    assert(run(PrecisionAtK[Int](2))(rankingData) === 1.0 / 3)
    assert(run(PrecisionAtK[Int](3))(rankingData) === 1.0 / 3)
    assert(run(PrecisionAtK[Int](4))(rankingData) === 0.75 / 3)
    assert(run(PrecisionAtK[Int](5))(rankingData) === 0.8 / 3)
    assert(run(PrecisionAtK[Int](10))(rankingData) === 0.8 / 3)
    assert(run(PrecisionAtK[Int](15))(rankingData) === 8.0 / 45)
  }

  it should "compute precisionAtK for rankings with few predictions" in {
    assert(run(PrecisionAtK[Int](1))(smallRankingData) === 0.5)
    assert(run(PrecisionAtK[Int](2))(smallRankingData) === 0.25)
  }
}
