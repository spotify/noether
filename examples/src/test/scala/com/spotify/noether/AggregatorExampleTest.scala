package com.spotify.noether

import org.scalatest.{FlatSpec, Matchers}

class AggregatorExampleTest extends FlatSpec with Matchers {
  it should "not fail when executing example" in {
    AggregatorExample.main(Array.empty)
  }
}
