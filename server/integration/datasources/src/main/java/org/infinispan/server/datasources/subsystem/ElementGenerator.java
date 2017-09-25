package org.infinispan.server.datasources.subsystem;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

/**
 * @author Ryan Emerson
 * @since 9.1
 */
class ElementGenerator {

   public static void main(String[] args) throws Exception {
      File file = new File("/home/remerson/workspace/RedHat/infinispan/infinispan/server/integration/datasources/src/main/resources/schema/StrippedModelKeys.txt");
      try (BufferedReader br = new BufferedReader(new FileReader(file))) {
         String line;
         while ((line = br.readLine()) != null) {
            if (line.isEmpty())
               continue;

            String name = line.replace("static final String ", "").trim().split(" ")[0];
            System.out.println(String.format("%1$s(ModelKeys.%1$s, true),", name));
         }
      }
   }
}
