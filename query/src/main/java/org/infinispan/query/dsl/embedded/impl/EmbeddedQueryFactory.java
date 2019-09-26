package org.infinispan.query.dsl.embedded.impl;

import org.infinispan.protostream.SerializationContext;
import org.infinispan.query.dsl.IndexedQueryMode;
import org.infinispan.query.dsl.Query;
import org.infinispan.query.dsl.QueryBuilder;
import org.infinispan.query.dsl.impl.BaseQuery;
import org.infinispan.query.dsl.impl.BaseQueryFactory;

/**
 * @author anistor@redhat.com
 * @since 7.0
 */
public final class EmbeddedQueryFactory extends BaseQueryFactory {

   private final QueryEngine<?> queryEngine;
   private final SerializationContext serCtx;

   public EmbeddedQueryFactory(QueryEngine queryEngine) {
      this(queryEngine, null);
   }

   public EmbeddedQueryFactory(QueryEngine queryEngine, SerializationContext serCtx) {
      if (queryEngine == null) {
         throw new IllegalArgumentException("queryEngine cannot be null");
      }
      this.queryEngine = queryEngine;
      this.serCtx = serCtx;
   }

   @Override
   public BaseQuery create(String queryString) {
      return new DelegatingQuery<>(queryEngine, this, queryString, IndexedQueryMode.FETCH);
   }

   @Override
   public Query create(String queryString, IndexedQueryMode queryMode) {
      return new DelegatingQuery<>(queryEngine, this, queryString, queryMode);
   }

   @Override
   public QueryBuilder from(Class<?> type) {
      String typeName = serCtx == null ? type.getName() : serCtx.getMarshaller(type).getTypeName();
      return from(typeName);
   }

   @Override
   public QueryBuilder from(String type) {
      return new EmbeddedQueryBuilder(this, queryEngine, type);
   }
}
