/*
 *  Copyright (c) 2018, salesforce.com, inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */
package org.infinispan.plugins.proto.compatibility;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermissions;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

/**
 * Modified version of the https://github.com/salesforce/proto-backwards-compat-maven-plugin, that does not require
 * proto.lock files to be created inline with .proto files and allows execution to be skipped.
 *
 * @author Ryan Emerson
 * @since 10.0
 */
@Mojo(
      name = "proto-schema-compatibility-check",
      defaultPhase = LifecyclePhase.VERIFY,
      requiresDependencyResolution = ResolutionScope.COMPILE,
      threadSafe = true)
public class ProtoCompatibilityMojo extends AbstractMojo {

   @Parameter(defaultValue = "${project}", readonly = true, required = true)
   private MavenProject mavenProject;

   @Parameter(defaultValue = "${basedir}")
   private String protoLockRoot;

   @Parameter(defaultValue = "${project.build.directory}/classes/schema/proto", readonly = true)
   private String protoSourceRoot;

   /**
    * Execute the plugin.
    * @throws MojoExecutionException thrown when execution of protolock fails.
    * @throws MojoFailureException thrown when compatibility check fails.
    */
   public void execute() throws MojoExecutionException, MojoFailureException {
      Path exeDirPath = Paths.get(mavenProject.getBuild().getDirectory(), "protolock-bin");
      if (!Files.isDirectory(exeDirPath)) {
         try {
            Files.createDirectory(exeDirPath);
         } catch (IOException e) {
            throw new MojoExecutionException("Unable to create the protolock binary directory", e);
         }
      }

      String classifier = mavenProject.getProperties().getProperty("os.detected.classifier");
      String exeExtension = classifier.startsWith("windows") ? ".exe" : "";
      Path exePath = exeDirPath.resolve("protolock" + exeExtension);
      if (!Files.exists(exePath)) {
         String protolockResourcePath = classifier + "/protolock" + exeExtension;

         try (InputStream in = this.getClass().getClassLoader().getResourceAsStream(protolockResourcePath)) {
            if (in == null) {
               throw new MojoExecutionException(
                     "OS not supported. Unable to find a protolock binary for the classifier " + classifier);
            }

            Files.copy(in, exePath);

            PosixFileAttributeView attributes = Files.getFileAttributeView(exePath, PosixFileAttributeView.class);
            if (attributes != null) {
               attributes.setPermissions(PosixFilePermissions.fromString("rwxrwxr-x"));
            }
         } catch (IOException e) {
            throw new MojoExecutionException("Unable to write the protolock binary", e);
         }
      }

      try {
         Path lockFile = Paths.get(protoLockRoot, "proto.lock");
         if (!Files.exists(lockFile)) {
            runCmd(exePath, "init").waitFor();
            getLog().info("Initialized protolock.");
         } else {
            Process protolock = runCmd(exePath, "status");
            BufferedReader stdInput = new BufferedReader(new InputStreamReader(protolock.getInputStream()));
            String s;
            while ((s = stdInput.readLine()) != null) {
               getLog().error(s);
            }

            int exitVal = protolock.waitFor();
            if (exitVal != 0) {
               throw new MojoFailureException(String.format("Backwards compatibility check failed with return code %d!", exitVal));
            } else {
               runCmd(exePath, "commit");
               getLog().info("Backwards compatibility check passed.");
            }
         }
      } catch (IOException | InterruptedException e) {
         throw new MojoExecutionException("An error occurred while running protolock", e);
      }
   }

   private Process runCmd(Path exePath, String action) throws IOException {
      String cmd = String.format("%s %s --lockdir %s --protoroot %s", exePath, action, protoLockRoot, protoSourceRoot);
      return Runtime.getRuntime().exec(cmd, null, mavenProject.getBasedir());
   }
}
