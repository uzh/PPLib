import ch.uzh.ifi.pdeboer.pplib.hcomp.crowdflower.CrowdFlowerPortalAdapter
import ch.uzh.ifi.pdeboer.pplib.hcomp.{FreetextQuery, HComp}

HComp.addPortal(new CrowdFlowerPortalAdapter("PatrickTest", "s2xE6ApWLDRobTg5yj8a", sandbox = true))
val query = HComp.crowdFlower.sendQuery(FreetextQuery("wie heisst du?"))