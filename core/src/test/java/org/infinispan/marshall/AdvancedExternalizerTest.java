package org.infinispan.marshall;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.infinispan.Cache;
import org.infinispan.commons.marshall.AbstractExternalizer;
import org.infinispan.commons.marshall.AdvancedExternalizer;
import org.infinispan.commons.marshall.SerializeWith;
import org.infinispan.commons.util.Util;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.marshall.persistence.PersistenceMarshaller;
import org.infinispan.test.MultipleCacheManagersTest;
import org.infinispan.test.TestingUtil;
import org.testng.annotations.Test;

/**
 * Tests configuration of user defined {@link AdvancedExternalizer} implementations
 *
 * @author Galder Zamarreño
 * @since 5.0
 */
@Test(groups = "functional", testName = "marshall.AdvancedExternalizerTest")
public class AdvancedExternalizerTest extends MultipleCacheManagersTest {

   @Override
   protected void createCacheManagers() throws Throwable {
      GlobalConfigurationBuilder globalConfig = createForeignExternalizerGlobalConfig();
      ConfigurationBuilder cfg = getDefaultClusteredCacheConfig(CacheMode.REPL_SYNC, false);
      createCluster(globalConfig, cfg, 2);
      waitForClusterToForm(getCacheName());
   }

   protected String getCacheName() {
      return "ForeignExternalizers";
   }

   protected GlobalConfigurationBuilder createForeignExternalizerGlobalConfig() {
      GlobalConfigurationBuilder builder = new GlobalConfigurationBuilder().clusteredDefault();
      builder.serialization()
         .addAdvancedExternalizer(AdvancedExternalizer.USER_EXT_ID_MIN, new IdViaConfigObj.Externalizer())
         .addAdvancedExternalizer(new IdViaAnnotationObj.Externalizer())
         .addAdvancedExternalizer(3456, new IdViaBothObj.Externalizer());
      return builder;
   }

   public void testReplicatePojosWithUserDefinedExternalizers(Method m) {
      Cache cache1 = manager(0).getCache(getCacheName());
      Cache cache2 = manager(1).getCache(getCacheName());
      IdViaConfigObj configObj = new IdViaConfigObj().setName("Galder");
      String key = "k-" + m.getName() + "-viaConfig";
      cache1.put(key, configObj);
      assertEquals(configObj.name, ((IdViaConfigObj)cache2.get(key)).name);
      IdViaAnnotationObj annotationObj = new IdViaAnnotationObj().setDate(new Date(System.currentTimeMillis()));
      key = "k-" + m.getName() + "-viaAnnotation";
      cache1.put(key, annotationObj);
      assertEquals(annotationObj.date, ((IdViaAnnotationObj)cache2.get(key)).date);
      IdViaBothObj bothObj = new IdViaBothObj().setAge(30);
      key = "k-" + m.getName() + "-viaBoth";
      cache1.put(key, bothObj);
      assertEquals(bothObj.age, ((IdViaBothObj)cache2.get(key)).age);
   }

   public void testExternalizerConfigInfo() {
      Map<Integer, AdvancedExternalizer<?>> advExts =
            manager(0).getCacheManagerConfiguration().serialization().advancedExternalizers();

      assertEquals(3, advExts.size());
      assertExternalizer(advExts, AdvancedExternalizer.USER_EXT_ID_MIN, IdViaConfigObj.Externalizer.class, false);
      assertExternalizer(advExts, 3456, IdViaBothObj.Externalizer.class, false);
      assertExternalizer(advExts, 5678, IdViaAnnotationObj.Externalizer.class, true);
   }

   private void assertExternalizer(Map<Integer, AdvancedExternalizer<?>> exts, Integer id, Class expectedClass, boolean assertId) {
      AdvancedExternalizer<?> ext = exts.get(id);
      assertNotNull(ext);
      assertEquals(expectedClass, ext.getClass());
      if (assertId)
         assertEquals(id, ext.getId());
   }

   public void testPersistenceMarshallerWithExternalizer() throws Exception {
      testMarshallObjectWithPersistenceMarshaller(new IdViaConfigObj().setName("Test"));
   }

   public void testPersistenceMarshallerWithAnnotatedExternalizer() throws Exception {
      testMarshallObjectWithPersistenceMarshaller(new AnnotatedObject().setName("Test"));
   }

   private void testMarshallObjectWithPersistenceMarshaller(Object obj) throws Exception {
      PersistenceMarshaller pm = TestingUtil.extractPersistenceMarshaller(manager(0));
      byte[] bytes = pm.objectToByteBuffer(obj);
      Object deserialized = pm.objectFromByteBuffer(bytes);
      assertEquals(obj, deserialized);
   }

   public static class IdViaConfigObj {
      String name;

      public IdViaConfigObj setName(String name) {
         this.name = name;
         return this;
      }

      @Override
      public boolean equals(Object o) {
         if (this == o) return true;
         if (o == null || getClass() != o.getClass()) return false;
         IdViaConfigObj that = (IdViaConfigObj) o;
         return Objects.equals(name, that.name);
      }

      @Override
      public int hashCode() {
         return Objects.hash(name);
      }

      @Override
      public String toString() {
         return "IdViaConfigObj{" +
               "name='" + name + '\'' +
               '}';
      }

      public static class Externalizer extends AbstractExternalizer<IdViaConfigObj> {
         @Override
         public void writeObject(ObjectOutput output, IdViaConfigObj object) throws IOException {
            output.writeUTF(object.name);
         }

         @Override
         public IdViaConfigObj readObject(ObjectInput input) throws IOException, ClassNotFoundException {
            return new IdViaConfigObj().setName(input.readUTF());
         }

         @Override
         public Set<Class<? extends IdViaConfigObj>> getTypeClasses() {
            return Util.<Class<? extends IdViaConfigObj>>asSet(IdViaConfigObj.class);
         }
      }
   }

   public static class IdViaAnnotationObj {
      Date date;

      public IdViaAnnotationObj setDate(Date date) {
         this.date = date;
         return this;
      }

      public static class Externalizer extends AbstractExternalizer<IdViaAnnotationObj> {
         @Override
         public void writeObject(ObjectOutput output, IdViaAnnotationObj object) throws IOException {
            output.writeObject(object.date);
         }

         @Override
         public IdViaAnnotationObj readObject(ObjectInput input) throws IOException, ClassNotFoundException {
            return new IdViaAnnotationObj().setDate((Date) input.readObject());
         }

         @Override
         public Integer getId() {
            return 5678;
         }

         @Override
         public Set<Class<? extends IdViaAnnotationObj>> getTypeClasses() {
            return Util.<Class<? extends IdViaAnnotationObj>>asSet(IdViaAnnotationObj.class);
         }
      }
   }

   public static class IdViaBothObj {
      int age;

      public IdViaBothObj setAge(int age) {
         this.age = age;
         return this;
      }

      public static class Externalizer extends AbstractExternalizer<IdViaBothObj> {
         @Override
         public void writeObject(ObjectOutput output, IdViaBothObj object) throws IOException {
            output.writeInt(object.age);
         }

         @Override
         public IdViaBothObj readObject(ObjectInput input) throws IOException, ClassNotFoundException {
            return new IdViaBothObj().setAge(input.readInt());
         }

         @Override
         public Integer getId() {
            return 9012;
         }

         @Override
         public Set<Class<? extends IdViaBothObj>> getTypeClasses() {
            return Util.<Class<? extends IdViaBothObj>>asSet(IdViaBothObj.class);
         }
      }
   }

   @SerializeWith(AnnotatedObject.Externalizer.class)
   public static class AnnotatedObject {

      String name;

      AnnotatedObject setName(String name) {
         this.name = name;
         return this;
      }

      @Override
      public boolean equals(Object o) {
         if (this == o) return true;
         if (o == null || getClass() != o.getClass()) return false;
         AnnotatedObject that = (AnnotatedObject) o;
         return Objects.equals(name, that.name);
      }

      @Override
      public int hashCode() {
         return Objects.hash(name);
      }

      public static class Externalizer implements org.infinispan.commons.marshall.Externalizer<AnnotatedObject> {

         public Externalizer() {
         }

         @Override
         public void writeObject(ObjectOutput output, AnnotatedObject object) throws IOException {
            output.writeUTF(object.name);
         }

         @Override
         public AnnotatedObject readObject(ObjectInput input) throws IOException, ClassNotFoundException {
            return new AnnotatedObject().setName(input.readUTF());
         }
      }
   }
}
