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

package com.spotify.ml.aggregators

import com.twitter.algebird.Semigroup
import com.twitter.algebird.Aggregator

final case class ErrorPrediction(scores: List[Double], label: Int) extends Serializable {
  override def toString: String = s"$label,${scores.mkString(":")}"
}

final case object ErrorRateAggregator extends Aggregator[ErrorPrediction, (Double, Long), Double] {
  def prepare(input: ErrorPrediction): (Double, Long) = {
    val best = input.scores.zipWithIndex.maxBy(_._1)._2
    if(best == input.label) (0.0, 1L) else (1.0, 1L)
  }
  def semigroup: Semigroup[(Double, Long)] = implicitly[Semigroup[(Double, Long)]]
  def present(score: (Double, Long)): Double = score._1/score._2
}