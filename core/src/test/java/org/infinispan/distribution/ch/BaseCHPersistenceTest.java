package org.infinispan.distribution.ch;

import static org.testng.AssertJUnit.assertEquals;

import org.infinispan.globalstate.ScopedPersistentState;
import org.infinispan.globalstate.impl.ScopedPersistentStateImpl;
import org.testng.annotations.Test;

@Test(groups = "unit")
public abstract class BaseCHPersistenceTest {

   protected abstract ConsistentHashFactory<?> createConsistentHashFactory();

   protected abstract ConsistentHash createConsistentHash();

   public void testCHPersistence() {
      ConsistentHash ch = createConsistentHash();
      ScopedPersistentState state = new ScopedPersistentStateImpl("scope");
      ch.toScopedState(state);

      ConsistentHashFactory<?> hashFactory = createConsistentHashFactory();
      ConsistentHash restoredCH = hashFactory.fromPersistentState(state);
      assertEquals(ch, restoredCH);
   }
}
