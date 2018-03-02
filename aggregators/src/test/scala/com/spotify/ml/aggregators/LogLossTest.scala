package com.spotify.ml.aggregators

import org.scalactic.TolerantNumerics
import org.scalatest.{FlatSpec, Matchers}

class LogLossTest extends FlatSpec with Matchers {
  private implicit val doubleEq = TolerantNumerics.tolerantDoubleEquality(0.1)
  private val classes = 10
  private def s(idx: Int, score: Double): List[Double] =
    0.until(classes).map(i => if(i == idx) score else 0.0).toList

  it should "return correct scores" in {
      val data = List((s(0, 0.8), 0), (s(1, 0.6), 1), (s(2, 0.7), 2))
        .map{case(scores, label) => LogLossPrediction(scores, label)}

      assert(LogLossAggregator(data) === 0.363548039673)
    }
}