package ch.uzh.ifi.pdeboer.pplib.patterns

/**
 * Created by pdeboer on 27/11/14.
 * Only makes sense if data is normally distributed.
 */
class SigmaPruner(val numSigmas: Int, val data: List[Double]) {
	assert(numSigmas >= 0)

	lazy val sum: Double = data.reduce(_ + _)
	lazy val mean: Double = sum / data.length
	lazy val devs: List[Double] = data.map(score => (score - mean) * (score - mean))
	lazy val stddev: Double = Math.sqrt(devs.reduce(_ + _) / devs.length)

	lazy val distance: Double = numSigmas * stddev

	lazy val minAllowedValue: Double = mean - distance
	lazy val maxAllowedValue: Double = mean + distance

	lazy val dataWithinRange: List[Double] = data.filter(v => v >= minAllowedValue && v <= maxAllowedValue)
}
