package org.infinispan.query.remote.impl;

import static org.infinispan.configuration.cache.IndexStorage.LOCAL_HEAP;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;

import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.protostream.DescriptorParserException;
import org.infinispan.protostream.FileDescriptorSource;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.SerializationContextInitializer;
import org.infinispan.test.SingleCacheManagerTest;
import org.infinispan.test.fwk.TestCacheManagerFactory;
import org.infinispan.transaction.TransactionMode;
import org.testng.annotations.Test;

/**
 * Test that ProtobufIndexedFieldProvider correctly identifies indexed fields based on the protostream annotations
 * applied in the test model (bank.proto).
 *
 * @author anistor@redhat.com
 * @since 9.0
 */
@Test(groups = "functional", testName = "query.remote.impl.ProtobufFieldIndexingMetadataTest")
public class ProtobufFieldIndexingMetadataTest extends SingleCacheManagerTest {

   private static final class TestSCI implements SerializationContextInitializer {

      @Override
      public String getProtoFile() {
         return "/** @Indexed */ message User {\n" +
               "\n" +
               "   /** @Field(store = Store.YES) */ required string name = 1;\n" +
               "\n" +
               "   required string surname = 2;\n" +
               "\n" +
               "   /** @Indexed */" +
               "   message Address {\n" +
               "      /** @Field(store = Store.YES) */ required string street = 10;\n" +
               "      required string postCode = 20;\n" +
               "   }\n" +
               "\n" +
               "   /** @Field(store = Store.YES) */ repeated Address indexedAddresses = 3;\n" +
               "\n" +
               "   /** @Field(index = Index.NO) */ repeated Address unindexedAddresses = 4;\n" +
               "}\n";
      }

      @Override
      public String getProtoFileName() {
         return "user_definition.proto";
      }

      @Override
      public void registerSchema(SerializationContext serCtx) {
         serCtx.registerProtoFiles(FileDescriptorSource.fromString(getProtoFileName(), getProtoFile()));
      }

      @Override
      public void registerMarshallers(SerializationContext ctx) {
      }
   }

   protected EmbeddedCacheManager createCacheManager() throws Exception {
      ConfigurationBuilder cfg = getDefaultStandaloneCacheConfig(true);
      cfg.transaction().transactionMode(TransactionMode.TRANSACTIONAL)
            .indexing().enable()
            .storage(LOCAL_HEAP)
            .addIndexedEntity("User");
      return TestCacheManagerFactory.createServerModeCacheManager(new TestSCI(), cfg);
   }

   public void testProtobufFieldIndexingMetadata() {
      SerializationContext serCtx = ProtobufMetadataManagerImpl.getSerializationContext(cacheManager);
      ProtobufFieldIndexingMetadata userIndexedFieldProvider = new ProtobufFieldIndexingMetadata(serCtx.getMessageDescriptor("User"));
      ProtobufFieldIndexingMetadata addressIndexedFieldProvider = new ProtobufFieldIndexingMetadata(serCtx.getMessageDescriptor("User.Address"));

      // try top level attributes
      assertTrue(userIndexedFieldProvider.isIndexed(new String[]{"name"}));
      assertFalse(userIndexedFieldProvider.isIndexed(new String[]{"surname"}));
      assertTrue(addressIndexedFieldProvider.isIndexed(new String[]{"street"}));
      assertFalse(addressIndexedFieldProvider.isIndexed(new String[]{"postCode"}));

      // try nested attributes
      assertTrue(userIndexedFieldProvider.isIndexed(new String[]{"indexedAddresses", "street"}));
      assertFalse(userIndexedFieldProvider.isIndexed(new String[]{"indexedAddresses", "postCode"}));
      assertFalse(userIndexedFieldProvider.isIndexed(new String[]{"unindexedAddresses", "street"}));
      assertFalse(userIndexedFieldProvider.isIndexed(new String[]{"unindexedAddresses", "postCode"}));
   }

   @Test(expectedExceptions = DescriptorParserException.class, expectedExceptionsMessageRegExp = "java.lang.IllegalStateException: Cannot specify an analyzer for field test.User3.name unless the field is analyzed.")
   public void testAnalyzerForNotAnalyzedField() {
      String testProto = "package test;\n" +
            "/* @Indexed */ message User3 {\n" +
            "   /* @Field(store=Store.NO, index=Index.YES, analyze=Analyze.NO, analyzer=@Analyzer(definition=\"standard\")) */" +
            "   required string name = 1;\n" +
            "}";

      SerializationContext serCtx = ProtobufMetadataManagerImpl.getSerializationContext(cacheManager);
      serCtx.registerProtoFiles(FileDescriptorSource.fromString("test2.proto", testProto));
   }
}
