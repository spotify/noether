package com.spotify.noether

import org.scalactic.TolerantNumerics

class BinaryConfusionMatrixTest extends AggregatorTest {
  it should "return correct scores" in {
    val data = List(
      (false, 0.1), (false, 0.6), (false, 0.2), (true, 0.2), (true, 0.8), (true, 0.7), (true, 0.6)
    ).map{case(pred, s) => Prediction(pred, s)}

    val matrix = run(BinaryConfusionMatrix())(data)

    assert(matrix(1, 1) === 3L)
    assert(matrix(0, 1) === 1L)
    assert(matrix(1, 0) === 1L)
    assert(matrix(0, 0) === 2L)
  }
}
