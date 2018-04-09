package com.spotify.noether

import breeze.linalg.DenseMatrix
import com.twitter.algebird.{Aggregator, Semigroup}

/**
 * Special Case for a Binary Confusion Matrix to make it easier to compose with other
 * binary aggregators
 *
 * @param threshold Threshold to apply on predictions
 */
case class BinaryConfusionMatrix(threshold: Double = 0.5)
  extends Aggregator[Prediction[Boolean, Double], Map[(Int, Int), Long], DenseMatrix[Long]]{
  private val confusionMatrix = ConfusionMatrix(Seq(0, 1))

  def prepare(input: Prediction[Boolean, Double]): Map[(Int, Int), Long] = {
    val pred = Prediction(if(input.actual) 1 else 0, if(input.predicted > threshold) 1 else 0)
    confusionMatrix.prepare(pred)
  }
  def semigroup: Semigroup[Map[(Int, Int), Long]] = confusionMatrix.semigroup
  def present(m: Map[(Int, Int), Long]): DenseMatrix[Long] = confusionMatrix.present(m)
}
