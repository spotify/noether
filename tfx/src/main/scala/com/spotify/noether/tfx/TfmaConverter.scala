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

package com.spotify.noether.tfx

import com.twitter.algebird.Aggregator
import tensorflow_model_analysis.MetricsForSliceOuterClass._

trait TfmaConverter[A, B, T <: Aggregator[A, B, _]] {
  def convertToTfmaProto(underlying: T): Aggregator[A, B, EvalResult]
}

sealed trait Plot {
  val plotData: PlotsForSlice
}
object Plot {
  case class CalibrationHistogram(plotData: PlotsForSlice) extends Plot
  case class ConfusionMatrix(plotData: PlotsForSlice) extends Plot
}

case class EvalResult(metrics: Option[MetricsForSlice], plots: Option[Plot])
object EvalResult {
  def apply(metrics: MetricsForSlice): EvalResult = EvalResult(Some(metrics), None)
  def apply(metrics: MetricsForSlice, plot: Plot): EvalResult =
    EvalResult(Some(metrics), Some(plot))
  def apply(plot: Plot): EvalResult = EvalResult(None, Some(plot))
}
