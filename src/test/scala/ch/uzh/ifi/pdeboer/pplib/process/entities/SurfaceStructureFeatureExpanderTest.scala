package ch.uzh.ifi.pdeboer.pplib.process.entities

import java.io.FileWriter

import ch.uzh.ifi.pdeboer.pplib.examples.textshortening.{ShortNDeepStructure, ShortNTestDataInitializer}
import ch.uzh.ifi.pdeboer.pplib.process.recombination.Recombinator
import org.junit.{Assert, Test}

/**
  * Created by pdeboer on 10/12/15.
  */
class SurfaceStructureFeatureExpanderTest {
	@Test
	def testExpander: Unit = {
		val testData = new ShortNTestDataInitializer()
		testData.initializePortal()
		val deepStructure = new ShortNDeepStructure()
		val recombinations = new Recombinator(deepStructure).recombine()

		Some(new FileWriter("output.xml")).foreach(s => {
			s.write(<Processes>
				{recombinations.map(_.recombinedProcessBlueprint.createProcess().xml)}
			</Processes>.toString)
			s.close()
		})

		val fe = new SurfaceStructureFeatureExpander(recombinations)
		val wr = new FileWriter("out.csv")
		wr.write(fe.features.map(_.path).mkString(",") + "\n")
		fe.surfaceStructures.foreach(s => {
			wr.write(fe.features.map(f => fe.featureValueAt(f, s).getOrElse("")).mkString(",") + "\n")
		})
		wr.close()
	}

	@Test
	def testFeatureDetection: Unit = {
		val exp = new XMLFeatureExpander(List(testXML))
		println(exp.features)
		Assert.assertEquals(55, exp.features.size)
		val findWorkerCount = exp.features.find(_.path == "/findProcess/workerCount")
		val fixerDeciderMaxIterations = exp.features.find(_.path == "/fixProcess/fixerProcess/decide/maxIterations")

		Assert.assertTrue(findWorkerCount.isDefined)
		Assert.assertTrue(fixerDeciderMaxIterations.isDefined)

		Assert.assertEquals(Some("5"), exp.valueAtPath(testXML, findWorkerCount.get.path))
		Assert.assertEquals(Some("20"), exp.valueAtPath(testXML, fixerDeciderMaxIterations.get.path))
		Assert.assertEquals(Some("ch.uzh.ifi.pdeboer.pplib.process.stdlib.ContestWithMultipleEqualWinnersProcess"), exp.valueAtPath(testXML, "/findProcess"))
	}

	val testXML = <Process>
		<Class>
			ch.uzh.ifi.pdeboer.pplib.process.stdlib.FindFixPatchProcess
		</Class>
		<InputClass>
			scala.collection.immutable.List
		</InputClass>
		<OutputClass>
			scala.collection.immutable.List
		</OutputClass>
		<Parameters>
			<Parameter>
				<Name>
					findProcess
				</Name>
				<Type>
					TypeTag[ch.uzh.ifi.pdeboer.pplib.process.entities.PassableProcessParam[ch.uzh.ifi.pdeboer.pplib.process.entities.DecideProcess[List[ch.uzh.ifi.pdeboer.pplib.process.entities.Patch],List[ch.uzh.ifi.pdeboer.pplib.process.entities.Patch]]]]
				</Type>
				<Value>
					<Process>
						<Class>
							ch.uzh.ifi.pdeboer.pplib.process.stdlib.ContestWithMultipleEqualWinnersProcess
						</Class>
						<InputClass>
							scala.collection.immutable.List
						</InputClass>
						<OutputClass>
							scala.collection.immutable.List
						</OutputClass>
						<Parameters>
							<Parameter>
								<Name>
									maxIterations
								</Name>
								<Type>
									TypeTag[Int]
								</Type>
								<Value>
									20
								</Value>
								<IsSpecified>
									true
								</IsSpecified>
							</Parameter> <Parameter>
							<Name>
								shuffle
							</Name>
							<Type>
								TypeTag[Boolean]
							</Type>
							<Value>
								true
							</Value>
							<IsSpecified>
								true
							</IsSpecified>
						</Parameter> <Parameter>
							<Name>
								workerCount
							</Name>
							<Type>
								TypeTag[Int]
							</Type>
							<Value>
								5
							</Value>
							<IsSpecified>
								true
							</IsSpecified>
						</Parameter> <Parameter>
							<Name>
								maxItemsPerFind
							</Name>
							<Type>
								TypeTag[Int]
							</Type>
							<Value>
								10
							</Value>
							<IsSpecified>
								true
							</IsSpecified>
						</Parameter> <Parameter>
							<Name>
								threshold
							</Name>
							<Type>
								TypeTag[Int]
							</Type>
							<Value>
								2
							</Value>
							<IsSpecified>
								true
							</IsSpecified>
						</Parameter> <Parameter>
							<Name>
								instructionGenerator
							</Name>
							<Type>
								TypeTag[Option[ch.uzh.ifi.pdeboer.pplib.process.entities.InstructionGenerator]]
							</Type>
							<Value>
								None
							</Value>
							<IsSpecified>
								true
							</IsSpecified>
						</Parameter> <Parameter>
							<Name>
								questionAux
							</Name>
							<Type>
								TypeTag[Option[scala.xml.NodeSeq]]
							</Type>
							<Value>
								None
							</Value>
							<IsSpecified>
								true
							</IsSpecified>
						</Parameter> <Parameter>
							<Name>
								cost
							</Name>
							<Type>
								TypeTag[ch.uzh.ifi.pdeboer.pplib.hcomp.HCompQueryProperties]
							</Type>
							<Value>
								HCompQueryProperties(0,1 day,List(PercentAssignmentsRejected LessThan 4, NumberHITsApproved GreaterThan 4000, Locale EqualTo US))
							</Value>
							<IsSpecified>
								true
							</IsSpecified>
						</Parameter> <Parameter>
							<Name>
								parallel
							</Name>
							<Type>
								TypeTag[Boolean]
							</Type>
							<Value>
								true
							</Value>
							<IsSpecified>
								true
							</IsSpecified>
						</Parameter> <Parameter>
							<Name>
								portal
							</Name>
							<Type>
								TypeTag[ch.uzh.ifi.pdeboer.pplib.hcomp.HCompPortalAdapter]
							</Type>
							<Value>
								ch.uzh.ifi.pdeboer.pplib.hcomp.randomportal.RandomHCompPortalWithDefinedCandidates@77afbd81
							</Value>
							<IsSpecified>
								true
							</IsSpecified>
						</Parameter> <Parameter>
							<Name>
								memoizerName
							</Name>
							<Type>
								TypeTag[Option[String]]
							</Type>
							<Value>
								None
							</Value>
							<IsSpecified>
								true
							</IsSpecified>
						</Parameter> <Parameter>
							<Name>
								storeExecutionResults
							</Name>
							<Type>
								TypeTag[Boolean]
							</Type>
							<Value>
								true
							</Value>
							<IsSpecified>
								true
							</IsSpecified>
						</Parameter>
						</Parameters>
					</Process>
				</Value>
				<IsSpecified>
					true
				</IsSpecified>
			</Parameter> <Parameter>
			<Name>
				fixProcess
			</Name>
			<Type>
				TypeTag[ch.uzh.ifi.pdeboer.pplib.process.entities.PassableProcessParam[ch.uzh.ifi.pdeboer.pplib.process.stdlib.FixPatchProcess]]
			</Type>
			<Value>
				<Process>
					<Class>
						ch.uzh.ifi.pdeboer.pplib.process.stdlib.FixPatchProcess
					</Class>
					<InputClass>
						scala.collection.immutable.List
					</InputClass>
					<OutputClass>
						scala.collection.immutable.List
					</OutputClass>
					<Parameters>
						<Parameter>
							<Name>
								fixerProcess
							</Name>
							<Type>
								TypeTag[ch.uzh.ifi.pdeboer.pplib.process.entities.PassableProcessParam[ch.uzh.ifi.pdeboer.pplib.process.entities.CreateProcess[ch.uzh.ifi.pdeboer.pplib.process.entities.Patch,ch.uzh.ifi.pdeboer.pplib.process.entities.Patch]]]
							</Type>
							<Value>
								<Process>
									<Class>
										ch.uzh.ifi.pdeboer.pplib.process.stdlib.CollectDecideProcess
									</Class>
									<InputClass>
										ch.uzh.ifi.pdeboer.pplib.process.entities.Patch
									</InputClass>
									<OutputClass>
										ch.uzh.ifi.pdeboer.pplib.process.entities.Patch
									</OutputClass>
									<Parameters>
										<Parameter>
											<Name>
												collect
											</Name>
											<Type>
												TypeTag[ch.uzh.ifi.pdeboer.pplib.process.entities.PassableProcessParam[ch.uzh.ifi.pdeboer.pplib.process.entities.CreateProcess[ch.uzh.ifi.pdeboer.pplib.process.entities.Patch,List[ch.uzh.ifi.pdeboer.pplib.process.entities.Patch]]]]
											</Type>
											<Value>
												<Process>
													<Class>
														ch.uzh.ifi.pdeboer.pplib.process.stdlib.CollectionWithSigmaPruning
													</Class>
													<InputClass>
														ch.uzh.ifi.pdeboer.pplib.process.entities.Patch
													</InputClass>
													<OutputClass>
														scala.collection.immutable.List
													</OutputClass>
													<Parameters>
														<Parameter>
															<Name>
																pruneByTextLength
															</Name>
															<Type>
																TypeTag[Boolean]
															</Type>
															<Value>
																true
															</Value>
															<IsSpecified>
																true
															</IsSpecified>
														</Parameter> <Parameter>
														<Name>
															numSigmas
														</Name>
														<Type>
															TypeTag[Int]
														</Type>
														<Value>
															3
														</Value>
														<IsSpecified>
															true
														</IsSpecified>
													</Parameter> <Parameter>
														<Name>
															workerCount
														</Name>
														<Type>
															TypeTag[Int]
														</Type>
														<Value>
															5
														</Value>
														<IsSpecified>
															true
														</IsSpecified>
													</Parameter> <Parameter>
														<Name>
															instructions
														</Name>
														<Type>
															TypeTag[ch.uzh.ifi.pdeboer.pplib.process.entities.InstructionData]
														</Type>
														<Value>
															ch.uzh.ifi.pdeboer.pplib.process.entities.InstructionData@a11a7de6
														</Value>
														<IsSpecified>
															true
														</IsSpecified>
													</Parameter> <Parameter>
														<Name>
															injectQuery
														</Name>
														<Type>
															TypeTag[Map[String,ch.uzh.ifi.pdeboer.pplib.hcomp.HCompQuery]]
														</Type>
														<Value>

														</Value>
														<IsSpecified>
															true
														</IsSpecified>
													</Parameter> <Parameter>
														<Name>
															instructionGenerator
														</Name>
														<Type>
															TypeTag[Option[ch.uzh.ifi.pdeboer.pplib.process.entities.InstructionGenerator]]
														</Type>
														<Value>
															None
														</Value>
														<IsSpecified>
															true
														</IsSpecified>
													</Parameter> <Parameter>
														<Name>
															questionAux
														</Name>
														<Type>
															TypeTag[Option[scala.xml.NodeSeq]]
														</Type>
														<Value>
															None
														</Value>
														<IsSpecified>
															true
														</IsSpecified>
													</Parameter> <Parameter>
														<Name>
															cost
														</Name>
														<Type>
															TypeTag[ch.uzh.ifi.pdeboer.pplib.hcomp.HCompQueryProperties]
														</Type>
														<Value>
															HCompQueryProperties(0,1 day,List(PercentAssignmentsRejected LessThan 4, NumberHITsApproved GreaterThan 4000, Locale EqualTo US))
														</Value>
														<IsSpecified>
															true
														</IsSpecified>
													</Parameter> <Parameter>
														<Name>
															parallel
														</Name>
														<Type>
															TypeTag[Boolean]
														</Type>
														<Value>
															true
														</Value>
														<IsSpecified>
															true
														</IsSpecified>
													</Parameter> <Parameter>
														<Name>
															portal
														</Name>
														<Type>
															TypeTag[ch.uzh.ifi.pdeboer.pplib.hcomp.HCompPortalAdapter]
														</Type>
														<Value>
															ch.uzh.ifi.pdeboer.pplib.hcomp.randomportal.RandomHCompPortalWithDefinedCandidates@77afbd81
														</Value>
														<IsSpecified>
															true
														</IsSpecified>
													</Parameter> <Parameter>
														<Name>
															memoizerName
														</Name>
														<Type>
															TypeTag[Option[String]]
														</Type>
														<Value>
															None
														</Value>
														<IsSpecified>
															true
														</IsSpecified>
													</Parameter> <Parameter>
														<Name>
															storeExecutionResults
														</Name>
														<Type>
															TypeTag[Boolean]
														</Type>
														<Value>
															true
														</Value>
														<IsSpecified>
															true
														</IsSpecified>
													</Parameter>
													</Parameters>
												</Process>
											</Value>
											<IsSpecified>
												true
											</IsSpecified>
										</Parameter> <Parameter>
										<Name>
											decide
										</Name>
										<Type>
											TypeTag[ch.uzh.ifi.pdeboer.pplib.process.entities.PassableProcessParam[ch.uzh.ifi.pdeboer.pplib.process.entities.DecideProcess[List[ch.uzh.ifi.pdeboer.pplib.process.entities.Patch],ch.uzh.ifi.pdeboer.pplib.process.entities.Patch]]]
										</Type>
										<Value>
											<Process>
												<Class>
													ch.uzh.ifi.pdeboer.pplib.process.stdlib.ContestWithStatisticalReductionProcess
												</Class>
												<InputClass>
													scala.collection.immutable.List
												</InputClass>
												<OutputClass>
													ch.uzh.ifi.pdeboer.pplib.process.entities.Patch
												</OutputClass>
												<Parameters>
													<Parameter>
														<Name>
															confidence
														</Name>
														<Type>
															TypeTag[Double]
														</Type>
														<Value>
															0.9
														</Value>
														<IsSpecified>
															true
														</IsSpecified>
													</Parameter> <Parameter>
													<Name>
														shuffle
													</Name>
													<Type>
														TypeTag[Boolean]
													</Type>
													<Value>
														true
													</Value>
													<IsSpecified>
														true
													</IsSpecified>
												</Parameter> <Parameter>
													<Name>
														maxIterations
													</Name>
													<Type>
														TypeTag[Int]
													</Type>
													<Value>
														20
													</Value>
													<IsSpecified>
														true
													</IsSpecified>
												</Parameter> <Parameter>
													<Name>
														instructions
													</Name>
													<Type>
														TypeTag[ch.uzh.ifi.pdeboer.pplib.process.entities.InstructionData]
													</Type>
													<Value>
														ch.uzh.ifi.pdeboer.pplib.process.entities.InstructionData@a11a7de6
													</Value>
													<IsSpecified>
														true
													</IsSpecified>
												</Parameter> <Parameter>
													<Name>
														hcompQueryBuilder
													</Name>
													<Type>
														TypeTag[ch.uzh.ifi.pdeboer.pplib.process.entities.HCompQueryBuilder[List[ch.uzh.ifi.pdeboer.pplib.process.entities.Patch]]]
													</Type>
													<Value>
														ch.uzh.ifi.pdeboer.pplib.process.entities.DefaultMCQueryBuilder@3fababe4
													</Value>
													<IsSpecified>
														true
													</IsSpecified>
												</Parameter> <Parameter>
													<Name>
														instructionGenerator
													</Name>
													<Type>
														TypeTag[Option[ch.uzh.ifi.pdeboer.pplib.process.entities.InstructionGenerator]]
													</Type>
													<Value>
														None
													</Value>
													<IsSpecified>
														true
													</IsSpecified>
												</Parameter> <Parameter>
													<Name>
														questionAux
													</Name>
													<Type>
														TypeTag[Option[scala.xml.NodeSeq]]
													</Type>
													<Value>
														None
													</Value>
													<IsSpecified>
														true
													</IsSpecified>
												</Parameter> <Parameter>
													<Name>
														cost
													</Name>
													<Type>
														TypeTag[ch.uzh.ifi.pdeboer.pplib.hcomp.HCompQueryProperties]
													</Type>
													<Value>
														HCompQueryProperties(0,1 day,List(PercentAssignmentsRejected LessThan 4, NumberHITsApproved GreaterThan 4000, Locale EqualTo US))
													</Value>
													<IsSpecified>
														true
													</IsSpecified>
												</Parameter> <Parameter>
													<Name>
														parallel
													</Name>
													<Type>
														TypeTag[Boolean]
													</Type>
													<Value>
														true
													</Value>
													<IsSpecified>
														true
													</IsSpecified>
												</Parameter> <Parameter>
													<Name>
														portal
													</Name>
													<Type>
														TypeTag[ch.uzh.ifi.pdeboer.pplib.hcomp.HCompPortalAdapter]
													</Type>
													<Value>
														ch.uzh.ifi.pdeboer.pplib.hcomp.randomportal.RandomHCompPortalWithDefinedCandidates@77afbd81
													</Value>
													<IsSpecified>
														true
													</IsSpecified>
												</Parameter> <Parameter>
													<Name>
														memoizerName
													</Name>
													<Type>
														TypeTag[Option[String]]
													</Type>
													<Value>
														None
													</Value>
													<IsSpecified>
														true
													</IsSpecified>
												</Parameter> <Parameter>
													<Name>
														storeExecutionResults
													</Name>
													<Type>
														TypeTag[Boolean]
													</Type>
													<Value>
														true
													</Value>
													<IsSpecified>
														true
													</IsSpecified>
												</Parameter>
												</Parameters>
											</Process>
										</Value>
										<IsSpecified>
											true
										</IsSpecified>
									</Parameter> <Parameter>
										<Name>
											forwardPatchToDecideMessage
										</Name>
										<Type>
											TypeTag[ch.uzh.ifi.pdeboer.pplib.process.stdlib.PatchEmbeddedInString]
										</Type>
										<Value>
											PatchEmbeddedInString(The original sentence was: ,)
										</Value>
										<IsSpecified>
											true
										</IsSpecified>
									</Parameter> <Parameter>
										<Name>
											forwardPatchToDecideParameter
										</Name>
										<Type>
											TypeTag[Option[ch.uzh.ifi.pdeboer.pplib.process.entities.ProcessParameter[String]]]
										</Type>
										<Value>
											None
										</Value>
										<IsSpecified>
											true
										</IsSpecified>
									</Parameter> <Parameter>
										<Name>
											forwardParamsToCollect
										</Name>
										<Type>
											TypeTag[Boolean]
										</Type>
										<Value>
											true
										</Value>
										<IsSpecified>
											true
										</IsSpecified>
									</Parameter> <Parameter>
										<Name>
											forwardParamsToDecide
										</Name>
										<Type>
											TypeTag[Boolean]
										</Type>
										<Value>
											true
										</Value>
										<IsSpecified>
											true
										</IsSpecified>
									</Parameter> <Parameter>
										<Name>
											memoizerName
										</Name>
										<Type>
											TypeTag[Option[String]]
										</Type>
										<Value>
											None
										</Value>
										<IsSpecified>
											true
										</IsSpecified>
									</Parameter> <Parameter>
										<Name>
											storeExecutionResults
										</Name>
										<Type>
											TypeTag[Boolean]
										</Type>
										<Value>
											true
										</Value>
										<IsSpecified>
											true
										</IsSpecified>
									</Parameter>
									</Parameters>
								</Process>
							</Value>
							<IsSpecified>
								true
							</IsSpecified>
						</Parameter> <Parameter>
						<Name>
							allData
						</Name>
						<Type>
							TypeTag[List[ch.uzh.ifi.pdeboer.pplib.process.entities.Patch]]
						</Type>
						<Value>

						</Value>
						<IsSpecified>
							true
						</IsSpecified>
					</Parameter> <Parameter>
						<Name>
							targetParamToPassPatchesAllData
						</Name>
						<Type>
							TypeTag[Option[ch.uzh.ifi.pdeboer.pplib.process.entities.ProcessParameter[List[ch.uzh.ifi.pdeboer.pplib.process.entities.Patch]]]]
						</Type>
						<Value>
							Some(allData)
						</Value>
						<IsSpecified>
							true
						</IsSpecified>
					</Parameter> <Parameter>
						<Name>
							beforeAfterHandler
						</Name>
						<Type>
							TypeTag[ch.uzh.ifi.pdeboer.pplib.patterns.FixVerifyFPDriver.FVFPDBeforeAfterHandler]
						</Type>
						<Value>
							None
						</Value>
						<IsSpecified>
							true
						</IsSpecified>
					</Parameter> <Parameter>
						<Name>
							patchesToIncludeBeforeAndAfterMain
						</Name>
						<Type>
							TypeTag[(Int, Int)]
						</Type>
						<Value>
							(1,1)
						</Value>
						<IsSpecified>
							true
						</IsSpecified>
					</Parameter> <Parameter>
						<Name>
							memoizerName
						</Name>
						<Type>
							TypeTag[Option[String]]
						</Type>
						<Value>
							None
						</Value>
						<IsSpecified>
							true
						</IsSpecified>
					</Parameter> <Parameter>
						<Name>
							storeExecutionResults
						</Name>
						<Type>
							TypeTag[Boolean]
						</Type>
						<Value>
							true
						</Value>
						<IsSpecified>
							true
						</IsSpecified>
					</Parameter>
					</Parameters>
				</Process>
			</Value>
			<IsSpecified>
				true
			</IsSpecified>
		</Parameter> <Parameter>
			<Name>
				memoizerName
			</Name>
			<Type>
				TypeTag[Option[String]]
			</Type>
			<Value>
				None
			</Value>
			<IsSpecified>
				true
			</IsSpecified>
		</Parameter> <Parameter>
			<Name>
				storeExecutionResults
			</Name>
			<Type>
				TypeTag[Boolean]
			</Type>
			<Value>
				true
			</Value>
			<IsSpecified>
				true
			</IsSpecified>
		</Parameter>
		</Parameters>
	</Process>
}
