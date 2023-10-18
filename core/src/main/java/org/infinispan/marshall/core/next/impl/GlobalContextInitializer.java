package org.infinispan.marshall.core.next.impl;

import org.infinispan.protostream.SerializationContextInitializer;
import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;

/**
 * Interface used to initialise the {@link org.infinispan.marshall.core.GlobalMarshaller}'s {@link
 * org.infinispan.protostream.SerializationContext} using the specified Pojos, Marshaller implementations and provided
 * .proto schemas.
 *
 * @author Ryan Emerson
 * @since 11.0
 */
@AutoProtoSchemaBuilder(
      dependsOn = {
            org.infinispan.commons.GlobalContextInitializer.class,
            org.infinispan.commons.marshall.PersistenceContextInitializer.class,
            org.infinispan.marshall.persistence.impl.PersistenceContextInitializer.class
      },
      includeClasses = {
            DummyClass.class,
      },
      schemaFileName = "global.core.proto",
      schemaFilePath = "proto/generated",
      schemaPackageName = GlobalContextInitializer.PACKAGE_NAME)
public interface GlobalContextInitializer extends SerializationContextInitializer {
   String PACKAGE_NAME = "org.infinispan.global.core";

   SerializationContextInitializer INSTANCE = new GlobalContextInitializerImpl();

   static String getFqTypeName(Class clazz) {
      return PACKAGE_NAME + "." + clazz.getSimpleName();
   }
}
