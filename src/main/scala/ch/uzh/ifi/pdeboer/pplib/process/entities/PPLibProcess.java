package ch.uzh.ifi.pdeboer.pplib.process.entities;

import java.lang.annotation.*;

/**
 * Created by pdeboer on 10/11/14.
 * Annotation that adds this class as Recombination process that is to be added to RecombinationDB automatically.
 * Requires designated class to work with an empty default constructor
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PPLibProcess {
	boolean autoInit() default true;

	Class<? extends ProcessFactory<?>> builder() default JavaAnnotationCompatibleDefaultProcessFactoryWrapper.class;
}