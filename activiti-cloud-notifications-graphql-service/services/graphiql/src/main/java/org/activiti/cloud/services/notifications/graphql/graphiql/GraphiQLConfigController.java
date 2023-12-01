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
package org.activiti.cloud.services.notifications.graphql.graphiql;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ConditionalOnWebApplication
public class GraphiQLConfigController {

    @Value("${graphiql.graphql.web.path:/graphql}")
    private String graphqlWebPath;

    @Value("${graphiql.graphql.ws.path:/ws/graphql}")
    private String graphqlWsPath;

    @Value("${server.servlet.context-path:}")
    private String contextPath;

    @GetMapping(value = "graphiql/config.js", produces = "application/javascript")
    @ResponseStatus(HttpStatus.OK)
    public String getConfigJs() {
        String config =
            "window.GraphqlApi = {" +
            "   graphqlWebPath: " +
            "\"" +
            getGraphQLWebPath() +
            "\"," +
            "   graphqlWsPath: " +
            "\"" +
            getGraphQLWsPath() +
            "\"" +
            "}";

        return config;
    }

    @GetMapping(value = "graphiql/config.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getGraphiqlJson() {
        Map<String, Object> values = new LinkedHashMap<>();

        values.put("graphqlWebPath", getGraphQLWebPath());
        values.put("graphqlWsPath", getGraphQLWsPath());

        return ResponseEntity.ok(values);
    }

    public String getGraphQLWebPath() {
        return appendSegmentToPath(contextPath, graphqlWebPath);
    }

    public String getGraphQLWsPath() {
        return appendSegmentToPath(contextPath, graphqlWsPath);
    }

    public String appendSegmentToPath(String path, String segment) {
        if (path == null || path.isEmpty() || "/".equals(path)) {
            return segment;
        }

        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }

        if (segment.startsWith("/")) {
            return path + segment;
        }

        return path + "/" + segment;
    }
}
