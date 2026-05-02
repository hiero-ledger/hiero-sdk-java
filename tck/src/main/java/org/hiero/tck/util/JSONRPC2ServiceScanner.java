// SPDX-License-Identifier: Apache-2.0
package org.hiero.tck.util;

import com.thetransactioncompany.jsonrpc2.server.*;
import org.hiero.tck.annotation.JSONRPC2Service;
import org.springframework.context.ApplicationContext;

/**
 * Utility class to register all {@link JSONRPC2Service} annotated classes
 * to the JSON-RPC dispatcher
 */
public class JSONRPC2ServiceScanner {
    private JSONRPC2ServiceScanner() {
        // private constructor for utility class
    }

    public static void registerServices(Dispatcher dispatcher, ApplicationContext context) {
        String[] serviceNames = context.getBeanNamesForAnnotation(JSONRPC2Service.class);
        if (serviceNames != null) { // NOSONAR
            for (String serviceName : serviceNames) {
                Object service = context.getBean(serviceName);
                dispatcher.register((RequestHandler) service);
            }
        }
    }
}
