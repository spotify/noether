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

final case class Scores(fscore: Double, precision: Double, recall: Double, fpr: Double)
  extends Serializable

final case class ClassificationAggregator(threshold: Double = 0.5, beta: Double = 1.0)
  extends Aggregator[Prediction, ConfusionMatrix, Scores]
    with Serializable {

  private val aggregator = ConfusionMatrixAggregator(threshold)

  def prepare(input: Prediction): ConfusionMatrix = aggregator.prepare(input)

  def semigroup: Semigroup[ConfusionMatrix] = aggregator.semigroup

  def present(m: ConfusionMatrix): Scores = {
    val precDenom = m.tp.toDouble + m.fp.toDouble
    val precision = if(precDenom > 0.0) m.tp.toDouble/precDenom else 1.0

    val recallDenom = m.tp.toDouble + m.fn.toDouble
    val recall = if(recallDenom > 0.0) m.tp.toDouble/recallDenom else 1.0

    val fpDenom = m.fp.toDouble + m.tn.toDouble
    val fpr = if(fpDenom > 0.0) m.fp.toDouble/fpDenom else 0.0

    val betaSqr = Math.pow(beta, 2.0)

    val fScoreDenom = (betaSqr*precision) + recall
    val fscore = if(fScoreDenom > 0.0){
      (1 + betaSqr) * ((precision*recall) / fScoreDenom)
    } else { 1.0 }

    Scores(fscore, precision, recall, fpr)
  }
}