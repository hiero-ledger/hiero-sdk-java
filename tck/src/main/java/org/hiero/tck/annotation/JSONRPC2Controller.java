// SPDX-License-Identifier: Apache-2.0
package org.hiero.tck.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Marks classes as HTTP handlers.
 * In the context of this application these handlers
 * should support the JSON-RPC spec
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Controller
@ResponseBody
public @interface JSONRPC2Controller {}
