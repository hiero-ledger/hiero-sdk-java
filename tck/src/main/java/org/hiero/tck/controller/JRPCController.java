// SPDX-License-Identifier: Apache-2.0
package org.hiero.tck.controller;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Request;
import com.thetransactioncompany.jsonrpc2.server.Dispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.hiero.tck.annotation.JSONRPC2Controller;
import org.hiero.tck.util.JSONRPC2ServiceScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.PostMapping;

@JSONRPC2Controller
public class JRPCController {
    private final Logger logger = LoggerFactory.getLogger(JRPCController.class);
    private final Dispatcher dispatcher;

    public JRPCController(final Dispatcher dispatcher, final ApplicationContext applicationContext) {
        this.dispatcher = dispatcher;
        JSONRPC2ServiceScanner.registerServices(dispatcher, applicationContext);
    }

    /**
     * Endpoint to handle all incoming JSON-RPC requests
     */
    @PostMapping("/")
    public String handleJSONRPC2Request(final HttpServletRequest request) {
        var req = (JSONRPC2Request) request.getAttribute("jsonrpcRequest");
        var resp = dispatcher.process(req, null);

        if (resp.getError() != null) {
            String errorMessage = String.format(
                    "Error occurred processing JSON-RPC request: %s, Response error: %s",
                    req.toJSONString(), resp.getError().toString());
            logger.info(errorMessage);
        }

        return resp.toJSONString();
    }
}
