package com.spotify.noether.tfx

import com.spotify.noether.{AUC, BinaryConfusionMatrix, Prediction, ROC}
import org.scalatest.{FlatSpec, Matchers}
import com.spotify.noether.tfx.TfmaConversionOps._

class TfmaConverterTest extends FlatSpec with Matchers {

  "Stuff" should "work" in {
    val binAgg = BinaryConfusionMatrix().asTfmaProto
    val aucAgg = AUC(ROC).asTfmaProto
  }

}
