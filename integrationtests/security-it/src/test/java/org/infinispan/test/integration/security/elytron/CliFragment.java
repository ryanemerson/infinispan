package org.infinispan.test.integration.security.elytron;

/**
 * Represents common piece in CLI commands, which can be shared across types.
 *
 * @author Josef Cacek
 */
public interface CliFragment {

    /**
     * Generates part of CLI string which uses configuration for this fragment.
     *
     * @return part of CLI command
     */
    String asString();

}