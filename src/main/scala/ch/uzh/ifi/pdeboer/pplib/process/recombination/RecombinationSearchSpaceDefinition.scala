package ch.uzh.ifi.pdeboer.pplib.process.recombination

import ch.uzh.ifi.pdeboer.pplib.process.entities.ProcessStub

import scala.reflect.ClassTag
import scala.reflect.runtime.universe._

/**
 * Created by pdeboer on 27/05/15.
 */
case class RecombinationSearchSpaceDefinition[T <: ProcessStub[_, _]](hints: RecombinationHints)(implicit val classTag: ClassTag[T], val typeTag: TypeTag[T])
