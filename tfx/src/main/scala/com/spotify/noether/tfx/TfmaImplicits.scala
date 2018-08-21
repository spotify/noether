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

import com.spotify.noether.tfx.Tfma.ConversionOps
import com.spotify.noether._

import scala.language.implicitConversions

trait TfmaImplicits {
  implicit def confusionMatrixConversion(agg: ConfusionMatrix)(
    implicit c: TfmaConverter[Prediction[Int, Int], Map[(Int, Int), Long], ConfusionMatrix])
    : ConversionOps[Prediction[Int, Int], Map[(Int, Int), Long], ConfusionMatrix] =
    ConversionOps[Prediction[Int, Int], Map[(Int, Int), Long], ConfusionMatrix](agg, c)

  implicit def binaryConfusionMatrixConversion(agg: BinaryConfusionMatrix)(
    implicit c: TfmaConverter[BinaryPred, Map[(Int, Int), Long], BinaryConfusionMatrix])
    : ConversionOps[BinaryPred, Map[(Int, Int), Long], BinaryConfusionMatrix] =
    ConversionOps[BinaryPred, Map[(Int, Int), Long], BinaryConfusionMatrix](agg, c)

  implicit def aucConversion(agg: AUC)(implicit c: TfmaConverter[BinaryPred, MetricCurve, AUC])
    : ConversionOps[BinaryPred, MetricCurve, AUC] =
    ConversionOps[BinaryPred, MetricCurve, AUC](agg, c)

  implicit def errorRateSummaryConversion(agg: ErrorRateSummary.type)(
    implicit c: TfmaConverter[Prediction[Int, List[Double]], (Double, Long), ErrorRateSummary.type])
    : ConversionOps[Prediction[Int, List[Double]], (Double, Long), ErrorRateSummary.type] =
    ConversionOps[Prediction[Int, List[Double]], (Double, Long), ErrorRateSummary.type](agg, c)
}
