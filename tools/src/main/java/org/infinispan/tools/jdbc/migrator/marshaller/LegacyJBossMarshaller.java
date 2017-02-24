package org.infinispan.tools.jdbc.migrator.marshaller;

import org.infinispan.commons.marshall.StreamingMarshaller;
import org.infinispan.commons.marshall.jboss.AbstractJBossMarshaller;
import org.infinispan.commons.marshall.jboss.DefaultContextClassResolver;
import org.infinispan.commons.marshall.jboss.SerializeWithExtFactory;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;

/**
 * A JBossMarshaller implementation used exclusively for reading byte arrays marshalled by Infinispan 8.
 */
public class LegacyJBossMarshaller extends AbstractJBossMarshaller implements StreamingMarshaller {
   LegacyJBossMarshaller() {
      baseCfg.setClassExternalizerFactory(new SerializeWithExtFactory());
      baseCfg.setObjectTable(new ExternalizerTable(this));
      baseCfg.setClassResolver(new DefaultContextClassResolver(GlobalConfigurationBuilder.class.getClassLoader()));
   }
}
