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

import com.twitter.algebird.Semigroup
import com.twitter.algebird.Aggregator

final case class Prediction(label: Int, score: Double) {
  override def toString: String = s"$label,$score"
}

final case class ConfusionMatrix(tp: Long = 0L, fp: Long = 0L, fn: Long = 0L, tn: Long = 0L)

final case class ConfusionMatrixAggregator(threshold: Double = 0.5)
  extends Aggregator[Prediction, ConfusionMatrix, ConfusionMatrix]
    with Serializable {

    def prepare(input: Prediction): ConfusionMatrix =
    (input.label, input.score) match {
      case (1, score) if score > threshold => ConfusionMatrix(tp = 1L)
      case (1, score) if score < threshold => ConfusionMatrix(fn = 1L)
      case (0, score) if score < threshold => ConfusionMatrix(tn = 1L)
      case (0, score) if score > threshold => ConfusionMatrix(fp = 1L)
    }

  def semigroup: Semigroup[ConfusionMatrix] =
    Semigroup.from{case(l, r) =>
      val tp = l.tp + r.tp
      val fp = l.fp + r.fp
      val fn = l.fn + r.fn
      val tn = l.tn + r.tn
      ConfusionMatrix(tp, fp, fn, tn)
    }

  def present(m: ConfusionMatrix): ConfusionMatrix = m
}
