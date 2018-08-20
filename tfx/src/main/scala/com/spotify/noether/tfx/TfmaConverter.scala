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

import com.google.protobuf.DoubleValue
import com.spotify.noether._
import com.twitter.algebird.Aggregator
import tensorflow_model_analysis.MetricsForSliceOuterClass.ConfusionMatrixAtThresholds.ConfusionMatrixAtThreshold
import tensorflow_model_analysis.MetricsForSliceOuterClass._

import scala.language.implicitConversions

//scalastyle:off

trait TfmaConverter[A, B, T <: Aggregator[A, B, _]] {
  def convertToTfmaProto(underlying: T): Aggregator[A, B, MetricsForSlice]
}

object TfmaConverter {

  implicit val binaryConfusionMatrixConverter
  : TfmaConverter[Prediction[Boolean, Double], Map[(Int, Int), Long], BinaryConfusionMatrix] =
    (underlying: BinaryConfusionMatrix) => underlying
      .andThenPresent {
        matrix =>
          MetricsForSlice.newBuilder()
            .setSliceKey(SliceKey.getDefaultInstance)
            .putMetrics("Noether_BinaryConfusionMatrix",
              MetricValue.newBuilder()
                .setConfusionMatrixAtThresholds(ConfusionMatrixAtThresholds.newBuilder()
                  .setMatrices(0, ConfusionMatrixAtThreshold.newBuilder()
                    .setThreshold(underlying.threshold)
                    //                  .setFalseNegatives(matrix)))
                    .build())
                  .build())
                .build()).build()
      }

  implicit val aucConverter: TfmaConverter[Prediction[Boolean, Double], MetricCurve, AUC] =
    (underlying: AUC) => underlying.andThenPresent { areaValue =>
      val metricName = underlying.metric match {
        case ROC => "Noether_AUC:ROC"
        case PR => "Noether_AUC:PR"
      }
      MetricsForSlice.newBuilder()
        .setSliceKey(SliceKey.getDefaultInstance)
        .putMetrics(metricName,
          MetricValue.newBuilder()
            .setDoubleValue(DoubleValue.newBuilder().setValue(areaValue))
            .build())
        .build()
    }

}

trait TfmaConversionOps[A, B, T <: Aggregator[A, B, _]] {
  val self: T
  val converter: TfmaConverter[A, B, T]
  def asTfmaProto: Aggregator[A, B, MetricsForSlice]  = converter.convertToTfmaProto(self)
}

object TfmaConversionOps {

  import TfmaConverter._

  def apply[A, B, T <: Aggregator[A, B, _]](instance: T, tfmaConverter: TfmaConverter[A, B, T])
  : TfmaConversionOps[A, B, T] =
    new TfmaConversionOps[A, B, T] {
      override val self: T = instance
      override val converter: TfmaConverter[A, B, T] = tfmaConverter
    }

  implicit def binaryConfusionMatrixConversion
  (agg: BinaryConfusionMatrix)
  (implicit c: TfmaConverter[Prediction[Boolean, Double],
                             Map[(Int, Int), Long],
                             BinaryConfusionMatrix])
  : TfmaConversionOps[Prediction[Boolean, Double], Map[(Int, Int), Long], BinaryConfusionMatrix] =
    TfmaConversionOps[Prediction[Boolean, Double], Map[(Int, Int), Long], BinaryConfusionMatrix](agg, c)

  implicit def aucConversion(agg: AUC)
                             (implicit c: TfmaConverter[Prediction[Boolean, Double],
                                                        MetricCurve,
                                                        AUC])
  : TfmaConversionOps[Prediction[Boolean, Double], MetricCurve, AUC] =
    TfmaConversionOps[Prediction[Boolean, Double], MetricCurve, AUC](agg, c)
}
//scalastyle:on
