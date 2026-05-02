// SPDX-License-Identifier: Apache-2.0
package org.hiero.tck.config;

import com.thetransactioncompany.jsonrpc2.server.Dispatcher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeanConfig {
    @Bean
    public Dispatcher dispatcher() {
        return new Dispatcher();
    }
}
