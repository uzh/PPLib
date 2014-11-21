package ch.uzh.ifi.pdeboer.pplib.hcomp;

import java.lang.annotation.*;

/**
 * Created by pdeboer on 21/11/14.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface HCompPortal {
	Class<? extends HCompPortalBuilder> builder();

	boolean autoInit() default true;
}
