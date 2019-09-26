package org.infinispan.query.remote.impl;

import java.util.Map;

import org.infinispan.AdvancedCache;
import org.infinispan.marshall.persistence.impl.PersistenceMarshallerImpl;
import org.infinispan.objectfilter.Matcher;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.descriptors.Descriptor;
import org.infinispan.query.dsl.IndexedQueryMode;
import org.infinispan.query.dsl.Query;
import org.infinispan.query.dsl.embedded.impl.EmbeddedQueryFactory;
import org.infinispan.query.dsl.embedded.impl.LuceneQueryMaker;
import org.infinispan.query.dsl.embedded.impl.QueryEngine;
import org.infinispan.query.impl.ComponentRegistryUtils;

/**
 * @author anistor@redhat.com
 * @since 9.0
 */
class BaseRemoteQueryEngine extends QueryEngine<Descriptor> {

   private final EmbeddedQueryFactory queryFactory;

   BaseRemoteQueryEngine(AdvancedCache<?, ?> cache, boolean isIndexed, Class<? extends Matcher> matcherImplClass,
                         LuceneQueryMaker.FieldBridgeAndAnalyzerProvider<Descriptor> fieldBridgeAndAnalyzerProvider) {
      super(cache, isIndexed, matcherImplClass, fieldBridgeAndAnalyzerProvider);

      PersistenceMarshallerImpl pm = ComponentRegistryUtils.getPersistenceMarshaller(cache);
      SerializationContext ctx = pm.protoStreamUserMarshaller() ? pm.getSerializationContext() : null;
      queryFactory = new EmbeddedQueryFactory(this, ctx);
   }

   Query makeQuery(String queryString, Map<String, Object> namedParameters, long startOffset, int maxResults, IndexedQueryMode queryMode) {
      Query query = queryFactory.create(queryString, queryMode);
      query.startOffset(startOffset);
      query.maxResults(maxResults);
      if (namedParameters != null) {
         query.setParameters(namedParameters);
      }
      return query;
   }
}
