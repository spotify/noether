/*
 * Copyright 2018 Spotify AB.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

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
