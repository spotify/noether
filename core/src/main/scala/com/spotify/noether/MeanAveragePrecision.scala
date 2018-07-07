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

import com.twitter.algebird.{Aggregator, Semigroup}

/**
 * Returns the mean average precision (MAP) of all the predictions.
 * If a query has an empty ground truth set, the average precision will be zero
 */
case class MeanAveragePrecision[T]()
    extends Aggregator[RankingPrediction[T], (Double, Long), Double] {
  def prepare(input: RankingPrediction[T]): (Double, Long) = {
    val labSet = input.actual.toSet
    if (labSet.nonEmpty) {
      var i = 0
      var cnt = 0
      var precSum = 0.0
      val n = input.predicted.length
      while (i < n) {
        if (labSet.contains(input.predicted(i))) {
          cnt += 1
          precSum += cnt.toDouble / (i + 1)
        }
        i += 1
      }
      (precSum / labSet.size, 1L)
    } else {
      (0.0, 1L)
    }
  }

  def semigroup: Semigroup[(Double, Long)] = implicitly[Semigroup[(Double, Long)]]

  def present(score: (Double, Long)): Double = score._1 / score._2
}
