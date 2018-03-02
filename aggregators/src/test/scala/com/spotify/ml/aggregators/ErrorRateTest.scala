package com.spotify.ml.aggregators

import org.scalactic.TolerantNumerics
import org.scalatest.{FlatSpec, Matchers}

class ErrorMatrixTest extends FlatSpec with Matchers {
  private implicit val doubleEq = TolerantNumerics.tolerantDoubleEquality(0.1)
  private val classes = 10
  private def s(idx: Int): List[Double] =
    0.until(classes).map(i => if(i == idx) 1.0 else 0.0).toList

  it should "return correct scores" in {
      val data = List((s(1), 1), (s(3), 1), (s(5), 5), (s(2), 3), (s(0), 0), (s(8), 1))
        .map{case(scores, label) => ErrorPrediction(scores, label)}

      assert(ErrorRateAggregator(data) === 0.5)
    }
}