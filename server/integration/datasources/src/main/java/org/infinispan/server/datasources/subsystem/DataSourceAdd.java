package org.infinispan.server.datasources.subsystem;

import java.util.Arrays;

import org.jboss.as.controller.AbstractAddStepHandler;

/**
 * @author Ryan Emerson
 * @since 9.2
 */
class DataSourceAdd extends AbstractAddStepHandler {
   static final DataSourceAdd INSTANCE = new DataSourceAdd();

   private DataSourceAdd() {
      super(Arrays.asList(DataSourceResource.DATASOURCE_ATTRIBUTE));
   }
}