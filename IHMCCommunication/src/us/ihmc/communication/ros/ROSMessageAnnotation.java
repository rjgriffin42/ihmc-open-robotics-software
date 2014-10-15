package us.ihmc.communication.ros;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by agrabertilton on 10/15/14.
 */


@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ROSMessageAnnotation {
   String documentation() default "No Documentation For This Class Is Recorded.";
}
