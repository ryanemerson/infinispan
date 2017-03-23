package org.infinispan.partitionhandling;

import org.testng.annotations.Test;

/**
 * @author Ryan Emerson
 * @since 9.0
 */
@Test(groups = "functional", testName = "partitionhandling.MergePolicyPrimaryAlwaysTest")
public class MergePolicyPrimaryAlwaysTest extends BaseMergePolicyTest {
   public MergePolicyPrimaryAlwaysTest() {
      super(MergePolicy.PRIMARY_ALWAYS);
   }

   public void test() {
      assert true;
   }
}
