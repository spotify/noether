package com.spotify.noether

object RankingData {
  def rankingData: Seq[RankingPrediction[Int]] =
    Seq(
      Prediction(Array(1, 2, 3, 4, 5), Array(1, 6, 2, 7, 8, 3, 9, 10, 4, 5)),
      Prediction(Array(1, 2, 3), Array(4, 1, 5, 6, 2, 7, 3, 8, 9, 10)),
      Prediction(Array.empty[Int], Array(1, 2, 3, 4, 5))
    )

  def smallRankingData: Seq[RankingPrediction[Int]] =
    Seq(
      Prediction(Array(1, 2, 3, 4, 5), Array(1, 6, 2)),
      Prediction(Array(1, 2, 3), Array.empty[Int])
    )
}
