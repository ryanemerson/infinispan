package org.infinispan.marshall.core;

import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;

import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

/**
 * SecurityActions for the {@link org.infinispan.marshall.core} package.
 * <p>
 * Do not move. Do not change class and method visibility to avoid being called from other
 * {@link java.security.CodeSource}s, thus granting privilege escalation to external code.
 *
 * @author Ryan Emerson
 * @since 10.0
 */
final class SecurityActions {

   private static final Log log = LogFactory.getLog(SecurityActions.class);

   static Method getMethodAndSetAccessible(Object o, String methodName, Class<?>... parameterTypes) throws NoSuchMethodException {
      return getMethodAndSetAccessible(o.getClass(), methodName, parameterTypes);
   }

   static Method getMethodAndSetAccessible(Class<?> clazz, String methodName, Class<?>... parameterTypes) throws NoSuchMethodException {
      Method method = clazz.getDeclaredMethod(methodName, parameterTypes);
      setAccessible(method);
      return method;
   }

   private static void setAccessible(Method method) {
      try {
         if (System.getSecurityManager() != null) {
            AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
               method.setAccessible(true);
               return null;
            });
         } else {
            method.setAccessible(true);
         }
      } catch (Exception e) {
         log.unableToSetAccesible(method, e);
      }
   }
}
