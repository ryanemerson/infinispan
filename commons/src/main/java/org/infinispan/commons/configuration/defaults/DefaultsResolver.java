package org.infinispan.commons.configuration.defaults;

import java.util.Map;
import java.util.Set;

/**
 * @author Ryan Emerson
 */
public interface DefaultsResolver {
   boolean isValidClass(String className);
   Map<String, String> extractDefaults(Set<Class> classes, String separator) throws Exception;
}
