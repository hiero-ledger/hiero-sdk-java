// SPDX-License-Identifier: Apache-2.0
package org.hiero.tck;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TckServer {
    public static void main(String[] args) {
        SpringApplication.run(TckServer.class, args);
    }
}
