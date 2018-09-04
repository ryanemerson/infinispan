package org.infinispan.commons.marshall;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import net.jcip.annotations.ThreadSafe;

/**
 * An extension of the {@link Marshaller} interface that facilitates the marshalling/unmarshalling of objects from
 * the provided {@link java.io.OutputStream}/{@link java.io.InputStream}
 *
 * @author remerson
 * @since 10.0
 */
@ThreadSafe
public interface StreamAwareMarshaller extends Marshaller {

   // TODO add docs
   void writeObject(Object o, OutputStream out) throws IOException;

   Object readObject(InputStream in) throws ClassNotFoundException, IOException;
}
