package com.spotify.ml.aggregators

import org.scalactic.TolerantNumerics
import org.scalatest.{FlatSpec, Matchers}

class AUCAggregatorTest extends FlatSpec with Matchers {
  private implicit val doubleEq = TolerantNumerics.tolerantDoubleEquality(0.1)

  private val data =
    List(
      (0.1, 0.0), (0.1, 1.0), (0.4, 0.0), (0.6, 0.0), (0.6, 1.0), (0.6, 1.0), (0.8, 1.0)
    ).map{case(s, pred) => Prediction(pred.toInt, s)}

  it should "return roc auc" in {
    assert(AUCAggregator(ROC, samples=50)(data) === 0.7)
  }

  it should "return pr auc" in {
    assert(AUCAggregator(PR, samples=50)(data) === 0.83)
  }
}