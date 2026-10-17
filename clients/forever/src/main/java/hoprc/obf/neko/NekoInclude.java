package hoprc.obf.neko;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks classes or methods that should be compiled into the Neko native layer.
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface NekoInclude {
}
