package org.infinispan.server.functional;

import java.io.File;
import java.nio.file.Paths;

import org.infinispan.cli.commands.CLI;
import org.infinispan.cli.impl.AeshDelegatingShell;
import org.infinispan.commons.test.CommonsTestingUtil;
import org.infinispan.commons.util.Util;
import org.infinispan.server.test.core.AeshTestConnection;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author Ryan Emerson
 * @since 11.0
 */
public class CliClusterBackupIT extends AbstractMultiClusterIT {
   static final File WORKING_DIR = new File(CommonsTestingUtil.tmpDirectory(ClusterBackupIT.class));

   public CliClusterBackupIT() {
      super("configuration/ClusteredServerTest.xml");
   }

   @BeforeClass
   public static void setup() {
      WORKING_DIR.mkdirs();
   }

   @AfterClass
   public static void teardown() {
      Util.recursiveFileRemove(WORKING_DIR);
   }

   @Test
   public void testInteractiveBackup() throws Exception {
      startSourceCluster();
      File backupFile;
      try (AeshTestConnection t = cli(source)) {
         t.readln("create cache --template=org.infinispan.DIST_SYNC backupCache");
         t.readln("cd caches/backupCache");
         t.readln("put k1 v1");
         t.readln("ls");
         t.assertContains("k1");
         t.clear();
         t.readln("backup");
         Thread.sleep(100);
         String output = t.getOutputBuffer();
         String fileName = output.substring((output.indexOf("'") + 1), (output.lastIndexOf("'")));
         backupFile = Paths.get(System.getProperty("user.dir")).resolve(fileName).toFile();
      }

      stopSourceCluster();
      startTargetCluster();

      try (AeshTestConnection t = cli(target)) {
         t.readln("restore " + backupFile);
         Thread.sleep(1000);
         t.readln("ls caches/backupCache");
         t.assertContains("k1");
      }
   }

   private AeshTestConnection cli(Cluster cluster) {
      AeshTestConnection t = new AeshTestConnection();
      CLI.main(new AeshDelegatingShell(t), new String[]{});
      String host = cluster.driver.getServerAddress(0).getHostAddress();
      int port = cluster.driver.getServerSocket(0, 11222).getPort();
      t.readln(String.format("connect %s:%d", host, port));
      t.assertContains("//containers/default]>");
      t.clear();
      return t;
   }
}
