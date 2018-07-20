package org.infinispan.marshall.core;

import static org.infinispan.factories.KnownComponentNames.USER_MARSHALLER;

import org.infinispan.commons.marshall.Marshaller;
import org.infinispan.factories.annotations.ComponentName;
import org.infinispan.factories.annotations.Inject;

/**
 * // TODO: Document this
 *
 * @author remerson
 * @since 4.0
 */
public class PersistenceMarshaller {

   @Inject @ComponentName(USER_MARSHALLER) private Marshaller userMarshaller;


}
