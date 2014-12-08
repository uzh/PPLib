import ch.uzh.ifi.pdeboer.pplib.process.stdlib.ContestWithStatisticalReductionProcess

val implementedBefore = new ContestWithStatisticalReductionProcess(
	Map(ContestWithStatisticalReductionProcess.INSTRUCTIONS_PARAMETER.key -> "Has this been implemented before"))
val answer = implementedBefore.process(List("yes", "no"))
if (answer == "yes") {
	val successfullyImpl = new ContestWithStatisticalReductionProcess(
		Map(ContestWithStatisticalReductionProcess.INSTRUCTIONS_PARAMETER.key -> "Has it been tried successfully?")
	)
	if (successfullyImpl.process(List("yes", "no")) == "yes") {
		//blabla
	} else {
		//no. blabla
	}
} else {
	//crap
}
