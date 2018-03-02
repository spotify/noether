package com.spotify.ml.aggregators

import org.scalactic.TolerantNumerics
import org.scalatest.{FlatSpec, Matchers}

class ConfusionMatrixTest extends FlatSpec with Matchers {
  private implicit val doubleEq = TolerantNumerics.tolerantDoubleEquality(0.1)

  it should "return correct scores" in {
      val data =
        List((0.1, 0.0), (0.1, 1.0), (0.4, 0.0), (0.6, 0.0), (0.6, 1.0), (0.6, 1.0), (0.8, 1.0))
        .map{case(s, pred) => Prediction(pred.toInt, s)}

      val score = ConfusionMatrixAggregator()(data)

      assert(score.recall === 0.75)
      assert(score.precision === 0.75)
      assert(score.fscore ===  0.75)
      assert(score.fpr === 0.333)
    }
}