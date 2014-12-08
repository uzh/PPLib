package ch.uzh.ifi.pdeboer.pplib.util

import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory

/**
 * Created by pdeboer on 08/12/14.
 */
trait LazyLogger {
	@transient protected lazy val logger: Logger =
		Logger(LoggerFactory.getLogger(getClass.getName))
}
