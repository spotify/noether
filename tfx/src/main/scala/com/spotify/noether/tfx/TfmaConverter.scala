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
import tensorflow_model_analysis.MetricsForSliceOuterClass.ConfusionMatrixAtThresholds._
import tensorflow_model_analysis.MetricsForSliceOuterClass._

trait TfmaConverter[A, B, T <: Aggregator[A, B, _]] {
  def convertToTfmaProto(underlying: T): Aggregator[A, B, MetricsForSlice]
}

object TfmaConverter {

  implicit val binaryConfusionMatrixConverter
    : TfmaConverter[BinaryPred, Map[(Int, Int), Long], BinaryConfusionMatrix] =
    new TfmaConverter[BinaryPred, Map[(Int, Int), Long], BinaryConfusionMatrix] {
      override def convertToTfmaProto(underlying: BinaryConfusionMatrix)
        : Aggregator[BinaryPred, Map[(Int, Int), Long], MetricsForSlice] =
        underlying
          .andThenPresent { matrix =>
            MetricsForSlice
              .newBuilder()
              .setSliceKey(SliceKey.getDefaultInstance)
              .putMetrics(
                "Noether_BinaryConfusionMatrix",
                MetricValue
                  .newBuilder()
                  .setConfusionMatrixAtThresholds(
                    newBuilder()
                      .setMatrices(0,
                                   ConfusionMatrixAtThreshold
                                     .newBuilder()
                                     .setThreshold(underlying.threshold)
                                     //                  .setFalseNegatives(matrix)))
                                     .build())
                      .build())
                  .build()
              )
              .build()
          }
    }

  implicit val aucConverter: TfmaConverter[BinaryPred, MetricCurve, AUC] =
    new TfmaConverter[BinaryPred, MetricCurve, AUC] {
      override def convertToTfmaProto(
        underlying: AUC): Aggregator[BinaryPred, MetricCurve, MetricsForSlice] =
        underlying
          .andThenPresent { areaValue =>
            val metricName = underlying.metric match {
              case ROC => "Noether_AUC:ROC"
              case PR  => "Noether_AUC:PR"
            }
            MetricsForSlice
              .newBuilder()
              .setSliceKey(SliceKey.getDefaultInstance)
              .putMetrics(metricName,
                          MetricValue
                            .newBuilder()
                            .setDoubleValue(DoubleValue.newBuilder().setValue(areaValue))
                            .build())
              .build()
          }
    }
}
