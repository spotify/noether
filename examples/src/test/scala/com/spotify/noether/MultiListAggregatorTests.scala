package com.spotify.noether

import com.twitter.algebird._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers._

class MultiListAggregatorTests extends AnyFlatSpec {

  it must "aggregate into a list of individual aggregator results" in {

    val multiListAgg = MultiListAggregator.apply[Long, Long, Long](List(
      "min"  -> Aggregator.min,
      "max"  -> Aggregator.max,
      "size" -> Aggregator.size
    ))

    val result = multiListAgg(List(0, 1, 2, 3, 4, 5))

    result("min") mustBe 0L
    result("max") mustBe 5L
    result("size") mustBe 6L
  }

}

