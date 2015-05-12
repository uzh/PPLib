package ch.uzh.ifi.pdeboer.pplib.util

import ch.uzh.ifi.pdeboer.pplib.process.entities.{PassableProcessParam, ProcessParameter}

import scala.xml.Elem

/**
 * Created by pdeboer on 04/05/15.
 */
class RecursiveProcessPrinter(val root: PassableProcessParam[_], val onlyIncludeParameters: Option[List[ProcessParameter[_]]] = None) {
	private val parameterKeys = onlyIncludeParameters.map(l => l.map(_.key).toSet).getOrElse(Set.empty)

	override def toString = {
		lines.mkString("\n")
	}

	def lines: Elem = {
		val paramsToUse = if (onlyIncludeParameters.isEmpty) root.params else root.params.filter(p => parameterKeys.contains(p._1) || p._2.isInstanceOf[PassableProcessParam[_]])
		<Process>
			<Name>
				{root.baseType}
			</Name>{paramsToUse.map {
			case (key, value) =>
				<key>
					{key}
				</key>
					<value>
						{value match {
						case childProcess: PassableProcessParam[_] =>
							<SubProcess>
								{new RecursiveProcessPrinter(childProcess, onlyIncludeParameters).lines}
							</SubProcess>
						case _ =>
							value
					}}
					</value>
		}}
		</Process>
	}
}