/*
 * Copyright 2017-2020 Alfresco Software, Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.activiti.cloud.starter.juel.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.Map;
import org.activiti.cloud.starter.juel.service.JuelExpressionResolverService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class JuelExpressionResolverController {

    private final JuelExpressionResolverService juelExpressionResolverService;

    public JuelExpressionResolverController(JuelExpressionResolverService juelExpressionResolverService) {
        this.juelExpressionResolverService = juelExpressionResolverService;
    }

    @PostMapping(value = "/v1/juel", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    public Map<String, Object> resolveExpression(@RequestBody Map<String, Object> inputVariables) {
        return juelExpressionResolverService.resolveExpression(inputVariables);
    }
}
