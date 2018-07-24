package org.infinispan.persistence.marshaller;

import org.infinispan.commons.marshall.StreamingMarshaller;
import org.infinispan.test.AbstractInfinispanTest;
import org.infinispan.test.data.Person;
import org.infinispan.test.fwk.TestCacheManagerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@Test(groups = "functional", testName = "persistence.PersistenceMarshallerTest")
public class PersistenceMarshallerTest extends AbstractInfinispanTest {

   private StreamingMarshaller marshaller;

   @BeforeClass
   public void setUp() {
      marshaller = TestCacheManagerFactory.createCacheManager().getCache().getAdvancedCache().getComponentRegistry().getPersistenceMarshaller();
   }

   public void testWrappedUserObject() throws Exception {
      Person value = new Person();
      byte[] userBytes = marshaller.objectToByteBuffer(value);
      Object unmarshalled = marshaller.objectFromByteBuffer(userBytes);
      Person cast = (Person) unmarshalled;
   }
}
