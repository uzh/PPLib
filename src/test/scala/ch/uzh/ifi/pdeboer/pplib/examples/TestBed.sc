import ch.uzh.ifi.pdeboer.pplib.hcomp.HComp
import ch.uzh.ifi.pdeboer.pplib.process.parameter.DefaultParameters
import ch.uzh.ifi.pdeboer.pplib.process.stdlib.GeneticAlgorithmProcess

new GeneticAlgorithmProcess(Map(DefaultParameters.PORTAL_PARAMETER.key -> HComp.mechanicalTurk)).process("test patrick")