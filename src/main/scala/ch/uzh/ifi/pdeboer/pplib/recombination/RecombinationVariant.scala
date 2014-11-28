package ch.uzh.ifi.pdeboer.pplib.recombination

import scala.reflect.ClassTag
import scala.xml.NodeSeq

/**
 * Created by pdeboer on 09/10/14.
 */
case class RecombinationVariant(stubs: Map[String, ProcessStub[_, _]]) {
	var name: Option[String] = None

	def apply[IN, OUT](key: String, data: IN) = getProcess(key).process(data)

	def getProcess[IN, OUT](key: String) = stubs(key).asInstanceOf[ProcessStub[IN, OUT]]

	def totalCost = stubs.values.map {
		case v: ProcessStubWithHCompPortalAccess[_, _] => v.portal.cost
		case _ => 0d
	}.sum
}

class RecombinationVariantXMLExporter(val variant: RecombinationVariant, val processResultExporters: List[ProcessResultXMLExporter[_]] = List(new MapExporter(), new ListExporter(), new SetExporter())) {
	def xml = <Variant>
		{variant.name match {
			case Some(x: String) => <Name>
				{x}
			</Name>
			case _ => {}
		}}<ProcessExecutions>
			{variant.stubs.map {
				case (processKey, process) => {
					<ProcessExecution>
						<Name>
							{processKey}
						</Name>{process.xml}{process match {
						case v: ProcessStubWithHCompPortalAccess[_, _] => <Cost>
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
						{transformDataWithExporter(process, process.inputType.runtimeClass, input)}
					</Input>
					<Output>
						{transformDataWithExporter(process, process.outputType.runtimeClass, output)}
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
	//only exact matches allowed
	def isApplicableTo(t: Class[_]) = implicitly[ClassTag[T]].runtimeClass == t

	def interpret(data: T): NodeSeq

	def interpretUnsafe(data: Any) = interpret(data.asInstanceOf[T])
}

class ListExporter extends ProcessResultXMLExporter[List[_]] {
	def itemExport(data: List[_]) = data.map(d => <Item>
		{d}
	</Item>)

	override def interpret(data: List[_]) = <List>
		{itemExport(data)}
	</List>
}

class SetExporter extends ProcessResultXMLExporter[Set[_]] {
	override def interpret(data: Set[_]): NodeSeq = <Set>
		{new ListExporter().itemExport(data.toList)}
	</Set>
}

class MapExporter extends ProcessResultXMLExporter[Map[_, _]] {
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