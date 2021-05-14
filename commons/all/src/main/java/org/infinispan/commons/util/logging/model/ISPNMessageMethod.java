package org.infinispan.commons.util.logging.model;

import org.infinispan.commons.util.logging.annotations.Description;
import org.jboss.logging.processor.model.MessageMethod;

public interface ISPNMessageMethod extends MessageMethod {
   
   /**
    * The {@link Description} to be used for the method.
    *
    * @return the message.
    */
   Description description();

}
