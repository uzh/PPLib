import ch.uzh.ifi.pdeboer.pplib.hcomp.HComp
import ch.uzh.ifi.pdeboer.pplib.process.ProcessStubWithHCompPortalAccess
import ch.uzh.ifi.pdeboer.pplib.process.stdlib.GeneticAlgorithmProcess

new GeneticAlgorithmProcess(Map(ProcessStubWithHCompPortalAccess.PORTAL_PARAMETER.key -> HComp.mechanicalTurk)).process("test patrick")