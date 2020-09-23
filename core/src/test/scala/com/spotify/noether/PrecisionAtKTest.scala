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

class PrecisionAtKTest extends AggregatorTest {
  import RankingData._
  implicit private val doubleEq: Equality[Double] = TolerantNumerics.tolerantDoubleEquality(0.1)

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
