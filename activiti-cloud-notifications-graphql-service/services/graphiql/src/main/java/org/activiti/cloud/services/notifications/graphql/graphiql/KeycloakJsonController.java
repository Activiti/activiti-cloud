/*
 * Copyright 2018 Alfresco, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ConditionalOnWebApplication
public class KeycloakJsonController {
    
    @Value("${keycloak.auth-server-url}")
    private String authServerUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.resource}")
    private String resource;
    
    @Value("${keycloak.public-client}")
    private Boolean publicClient;

    @Value("${keycloak.confidential-port:0}")
    private Integer confidentialPort;
    
    @Value("${keycloak.ssl-required:none}")
    private String sslRequired;
    
    public KeycloakJsonController() {
        // 
    }
    
    @GetMapping(value="graphiql/keycloak.json",  produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String,Object>> get() {
        Map<String, Object> values = new LinkedHashMap<>();
        
        values.put("auth-server-url", authServerUrl);
        values.put("realm", realm);
        values.put("ssl-required", sslRequired);
        values.put("resource", resource);
        values.put("public-client", publicClient);
        values.put("confidential-port", confidentialPort);
        
        return ResponseEntity.ok(values);
    }
    

}
