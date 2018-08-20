package com.spotify.noether.tfx

import com.spotify.noether.{BinaryConfusionMatrix, Prediction}
import org.scalatest.{FlatSpec, Matchers}
import com.spotify.noether.tfx.TfmaConversionOps._

class TfmaConverterTest extends FlatSpec with Matchers {

  "Stuff" should "work" in {
    val agg = BinaryConfusionMatrix().asTfmaProto

  }

}
