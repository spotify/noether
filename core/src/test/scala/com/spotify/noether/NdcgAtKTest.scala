package com.spotify.noether

import org.scalactic.{Equality, TolerantNumerics}

class NdcgAtKTest extends AggregatorTest {
  import RankingData._
  private implicit val doubleEq: Equality[Double] = TolerantNumerics.tolerantDoubleEquality(0.1)

  it should "compute ndcg for rankings" in {
    assert(run(NdcgAtK[Int](3))(rankingData) === 1.0 / 3)
    assert(run(NdcgAtK[Int](5))(rankingData) === 0.328788)
    assert(run(NdcgAtK[Int](10))(rankingData) === 0.487913)
    assert(run(NdcgAtK[Int](15))(rankingData) === run(NdcgAtK[Int](10))(rankingData))
  }

  it should "compute ndcg for rankings with few predictions" in {
    assert(run(NdcgAtK[Int](1))(smallRankingData) === 0.5)
    assert(run(NdcgAtK[Int](2))(smallRankingData) === 0.30657)
  }
}
