/*
 * Copyright 2020 Spotify AB.
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

import com.twitter.algebird._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers._

class MultiListAggregatorTests extends AnyFlatSpec {

  it must "aggregate into a list of individual aggregator results" in {

    val multiListAgg = MultiListAggregator.apply[Long, Long, Long](
      List(
        "min" -> Aggregator.min,
        "max" -> Aggregator.max,
        "size" -> Aggregator.size
      )
    )

    val result = multiListAgg(List(0, 1, 2, 3, 4, 5))

    result("min") mustBe 0L
    result("max") mustBe 5L
    result("size") mustBe 6L
  }

}
