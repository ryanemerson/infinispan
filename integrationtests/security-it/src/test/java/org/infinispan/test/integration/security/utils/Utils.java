package org.infinispan.test.integration.security.utils;

import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.security.common.CoreUtils;

/**
 * Common utilities for JDG security tests.
 *
 * @author Jan Lanik
 * @author Josef Cacek
 * @author Vitalii Chepeliuk
 */
public class Utils extends CoreUtils {

    /**
     * Returns canonical hostname retrieved from management address of the givem
     * {@link org.jboss.as.arquillian.container.ManagementClient}.
     *
     * @param managementClient
     */
    public static String getCannonicalHost(final ManagementClient managementClient) {
        return getCannonicalHost(managementClient.getMgmtAddress());
    }
}
