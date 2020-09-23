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

class MeanAveragePrecisionTest extends AggregatorTest {
  import RankingData._

  implicit private val doubleEq: Equality[Double] = TolerantNumerics.tolerantDoubleEquality(0.1)

  it should "compute map for rankings" in {
    assert(run(MeanAveragePrecision[Int]())(rankingData) === 0.355026)
  }
}
