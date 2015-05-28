package ch.uzh.ifi.pdeboer.pplib.util

import ch.uzh.ifi.pdeboer.pplib.process.entities.{PassableProcessParam, ProcessParameter}

import scala.xml.Elem

/**
 * Created by pdeboer on 04/05/15.
 */
class ProcessPrinter(val root: PassableProcessParam[_], val onlyIncludeParameters: Option[List[ProcessParameter[_]]] = None, val maxDepth: Int = 10) {
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
								{if (maxDepth > 0) new ProcessPrinter(childProcess, onlyIncludeParameters, maxDepth - 1).lines else "..."}
							</SubProcess>
						case _ =>
							value
					}}
					</value>
		}}
		</Process>
	}
}