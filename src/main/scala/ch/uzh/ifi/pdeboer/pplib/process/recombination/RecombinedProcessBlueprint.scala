package ch.uzh.ifi.pdeboer.pplib.process.recombination

import ch.uzh.ifi.pdeboer.pplib.process.entities.{HCompPortalAccess, PassableProcessParam, ProcessStub}

import scala.reflect.ClassTag
import scala.xml.NodeSeq

/**
 * Created by pdeboer on 09/10/14.
 */
class RecombinedProcessBlueprint(val stubs: Map[String, PassableProcessParam[_]]) {
	var name: Option[String] = None
	private var procs: List[(String, ProcessStub[_, _])] = Nil

	def created = procs

	def createProcess[IN, OUT](key: String = "", lowerPrioParams: Map[String, Any] = Map.empty,
							   forcedParams: Map[String, Any] = Map.empty): ProcessStub[IN, OUT] = {
		val targetProcess: PassableProcessParam[_] = if (key == "" && stubs.size == 1) stubs.values.head else stubs(key)
		val p = targetProcess.create(lowerPrioParams, forcedParams).asInstanceOf[ProcessStub[IN, OUT]]
		stubs.synchronized {
			procs = (key, p) :: procs
		}
		p
	}


	override def toString = s"RecombinedProcessBlueprint($stubs)"
}

class SimpleRecombinationVariantXMLExporter(val variant: RecombinedProcessBlueprint) {
	def passableToXML(passableProcessParam: PassableProcessParam[_], maxRecursions: Int = 4): NodeSeq =
		<ProcessDef>
			<Class>
				{passableProcessParam.clazz}
			</Class>
			<Params>
				{passableProcessParam.params.map(param =>
				<Param>
					<Key>
						{param._1}
					</Key> <Value>
					{param._2 match {
						case pa: PassableProcessParam[_] if maxRecursions > 0 => passableToXML(pa, maxRecursions - 1)
						case o => o
					}}
				</Value>
				</Param>
			)}
			</Params>
		</ProcessDef>

	def xml: NodeSeq = <ProcessClasses>
		{variant.stubs.map(p => {
			<Process>
				<Key>
					{p._1}
				</Key>{passableToXML(p._2)}
			</Process>
		})}
	</ProcessClasses>
}

class RecombinationVariantProcessXMLExporter(val variant: RecombinedProcessBlueprint, val processResultExporters: List[ProcessResultXMLExporter[_]] = List(new MapExporter(), new ListExporter(), new SetExporter())) {
	def xml: NodeSeq = <Variant>
		{variant.name match {
			case Some(x: String) => <Name>
				{x}
			</Name>
			case _ => {}
		}}<ProcessExecutions>
			{variant.created.map {
				case (processKey, process) => {
					<ProcessExecution>
						<Name>
							{processKey}
						</Name>{process.xml}{process match {
						case v: HCompPortalAccess => <Cost>
							{v.portal.cost}
						</Cost>
						case _ => {}
					}}{exportResult(process)}
					</ProcessExecution>
				}
			}}
		</ProcessExecutions>
	</Variant>

	def exportResult(process: ProcessStub[_, _]): NodeSeq = {
		<Results>
			{process.results.map {
			case (input, output) => {
				<Result>
					<Input>
						{transformDataWithExporter(process, process.inputClass.runtimeClass, input)}
					</Input>
					<Output>
						{transformDataWithExporter(process, process.outputClass.runtimeClass, output)}
					</Output>
				</Result>
			}
		}}
		</Results>
	}


	def transformDataWithExporter(process: ProcessStub[_, _], clazz: Class[_], data: Any): Any = {
		processResultExporters.find(_.isApplicableTo(clazz)) match {
			case Some(exporter: ProcessResultXMLExporter[_]) => exporter.interpretUnsafe(data)
			case None => data
		}
	}
}

abstract class ProcessResultXMLExporter[T: ClassTag] {
	def exactMatchOfClass: Boolean

	//only exact matches allowed
	def isApplicableTo(t: Class[_]) = {
		val clazz: Class[_] = implicitly[ClassTag[T]].runtimeClass
		if (exactMatchOfClass) clazz == t else t.isAssignableFrom(clazz)
	}

	def interpret(data: T): NodeSeq

	def interpretUnsafe(data: Any) = interpret(data.asInstanceOf[T])
}

class ListExporter(val exactMatchOfClass: Boolean = false) extends ProcessResultXMLExporter[List[_]] {
	def itemExport(data: List[_]) = data.map(d => <Item>
		{d}
	</Item>)

	override def interpret(data: List[_]) = <List>
		{itemExport(data)}
	</List>
}

class SetExporter(val exactMatchOfClass: Boolean = false) extends ProcessResultXMLExporter[Set[_]] {
	override def interpret(data: Set[_]): NodeSeq = <Set>
		{new ListExporter(exactMatchOfClass).itemExport(data.toList)}
	</Set>
}

class MapExporter(val exactMatchOfClass: Boolean = false) extends ProcessResultXMLExporter[Map[_, _]] {
	override def interpret(data: Map[_, _]): NodeSeq = <Map>
		{data.map(d => {
			<Item>
				<Key>
					{d._1}
				</Key>
				<Value>
					{d._2}
				</Value>
			</Item>
		})}
	</Map>
}