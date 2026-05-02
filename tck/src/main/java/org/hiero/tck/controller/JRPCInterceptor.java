// SPDX-License-Identifier: Apache-2.0
package org.hiero.tck.controller;

import com.thetransactioncompany.jsonrpc2.JSONRPC2ParseException;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Request;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Interceptor class to map HttpServletRequest body to {@link JSONRPC2Request}
 * before the request reaches the controller
 */
public class JRPCInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(final HttpServletRequest request, final HttpServletResponse response, final Object handler)
            throws Exception {
        // Map HTTP Servlet Request to JSON-RPC Request
        var jsonrpcRequest = mapToJSONRPC2Request(request);

        // Store the JSON-RPC request in request attribute for further processing
        request.setAttribute("jsonrpcRequest", jsonrpcRequest);

        // Continue processing the request
        return true;
    }

    JSONRPC2Request mapToJSONRPC2Request(final HttpServletRequest httpRequest)
            throws IOException, JSONRPC2ParseException {
        // Read the JSON-RPC request from the HTTP request body
        BufferedReader reader = httpRequest.getReader();
        StringBuilder requestBody = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            requestBody.append(line);
        }
        reader.close();

        // Parse the JSON-RPC request
        return JSONRPC2Request.parse(requestBody.toString());
    }
}
