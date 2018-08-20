package com.spotify.noether.tfx

import com.spotify.noether.{AUC, BinaryConfusionMatrix, Prediction}
import com.twitter.algebird.Aggregator
import tensorflow_model_analysis.MetricsForSliceOuterClass.ConfusionMatrixAtThresholds.ConfusionMatrixAtThreshold
import tensorflow_model_analysis.MetricsForSliceOuterClass.{ConfusionMatrixAtThresholds, MetricValue, MetricsForSlice, SliceKey}

import scala.language.implicitConversions

//scalastyle:off

trait TfmaConverter[A, B, T <: Aggregator[A, B, _]] {
  def convertToTfmaProto(underlying: T): Aggregator[A, B, MetricsForSlice]
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

  implicit def mkBinaryConfusionMatrixConverter(agg: BinaryConfusionMatrix)
                                               (implicit c: TfmaConverter[Prediction[Boolean, Double],
                                                 Map[(Int, Int), Long],
                                                 BinaryConfusionMatrix])
  : TfmaConversionOps[Prediction[Boolean, Double], Map[(Int, Int), Long], BinaryConfusionMatrix] =
    TfmaConversionOps[Prediction[Boolean, Double], Map[(Int, Int), Long], BinaryConfusionMatrix](agg, c)
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

}
//scalastyle:on
