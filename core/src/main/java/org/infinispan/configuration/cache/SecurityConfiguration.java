package org.infinispan.configuration.cache;

/**
 * SecurityConfiguration.
 *
 * @author Tristan Tarrant
 * @since 7.0
 */
public class SecurityConfiguration {

   private final AuthorizationConfiguration authorizationConfiguration;

   SecurityConfiguration(AuthorizationConfiguration authorization) {
      this.authorizationConfiguration = authorization;
   }

   public AuthorizationConfiguration authorization() {
      return authorizationConfiguration;
   }

   @Override
   public String toString() {
      return "SecurityConfiguration [authorization=" + authorizationConfiguration + "]";
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((authorizationConfiguration == null) ? 0 : authorizationConfiguration.hashCode());
      return result;
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj)
         return true;
      if (obj == null)
         return false;
      if (getClass() != obj.getClass())
         return false;
      SecurityConfiguration other = (SecurityConfiguration) obj;
      if (authorizationConfiguration == null) {
         if (other.authorizationConfiguration != null)
            return false;
      } else if (!authorizationConfiguration.equals(other.authorizationConfiguration))
         return false;
      return true;
   }
}
