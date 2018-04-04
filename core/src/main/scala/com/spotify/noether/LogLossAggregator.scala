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

case object LogLossAggregator
  extends Aggregator[Prediction[Int, List[Double]], (Double, Long), Double] {
  def prepare(input: Prediction[Int, List[Double]]): (Double, Long) =
    (math.log(input.predicted(input.actual)), 1L)
  def semigroup: Semigroup[(Double, Long)] = implicitly[Semigroup[(Double, Long)]]
  def present(score: (Double, Long)): Double = -1*(score._1/score._2)
}
