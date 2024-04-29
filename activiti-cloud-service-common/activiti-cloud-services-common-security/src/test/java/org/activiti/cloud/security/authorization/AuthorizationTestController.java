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
package org.activiti.cloud.security.authorization;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthorizationTestController {

    public static final String PUBLIC_GET = "/public/get";
    public static final String PUBLIC_POST = "/public/post";
    public static final String PUBLIC_PUT = "/public/put";
    public static final String PUBLIC_DELETE = "/public/delete";
    public static final String ROLE_GET = "/role/get";
    public static final String ROLE_POST = "/role/post";
    public static final String ROLE_PUT = "/role/put";
    public static final String ROLE_DELETE = "/role/delete";
    public static final String PERMISSION_GET = "/permission/get";
    public static final String PERMISSION_POST = "/permission/post";
    public static final String PERMISSION_PUT = "/permission/put";
    public static final String PERMISSION_DELETE = "/permission/delete";
    public static final String DUMMY_ENDPOINT = "/dummy-endpoint";

    @GetMapping(PUBLIC_GET)
    public ResponseEntity<Void> publicGet() {
        return ResponseEntity.ok().build();
    }

    @PostMapping(PUBLIC_POST)
    public ResponseEntity<Void> publicPost() {
        return ResponseEntity.ok().build();
    }

    @PutMapping(PUBLIC_PUT)
    public ResponseEntity<Void> publicPut() {
        return ResponseEntity.ok().build();
    }

    @DeleteMapping(PUBLIC_DELETE)
    public ResponseEntity<Void> publicDelete() {
        return ResponseEntity.ok().build();
    }

    @GetMapping(ROLE_GET)
    public ResponseEntity<Void> roleGet() {
        return ResponseEntity.ok().build();
    }

    @PostMapping(ROLE_POST)
    public ResponseEntity<Void> rolePost() {
        return ResponseEntity.ok().build();
    }

    @PutMapping(ROLE_PUT)
    public ResponseEntity<Void> rolePut() {
        return ResponseEntity.ok().build();
    }

    @DeleteMapping(ROLE_DELETE)
    public ResponseEntity<Void> roleDelete() {
        return ResponseEntity.ok().build();
    }

    @GetMapping(PERMISSION_GET)
    public ResponseEntity<Void> permissionGet() {
        return ResponseEntity.ok().build();
    }

    @PostMapping(PERMISSION_POST)
    public ResponseEntity<Void> permissionPost() {
        return ResponseEntity.ok().build();
    }

    @PutMapping(PERMISSION_PUT)
    public ResponseEntity<Void> permissionPut() {
        return ResponseEntity.ok().build();
    }

    @DeleteMapping(PERMISSION_DELETE)
    public ResponseEntity<Void> permissionDelete() {
        return ResponseEntity.ok().build();
    }

    @GetMapping(DUMMY_ENDPOINT)
    public ResponseEntity<Void> dummyEndpointGet() {
        return ResponseEntity.ok().build();
    }

    @PostMapping(DUMMY_ENDPOINT)
    public ResponseEntity<Void> dummyEndpointPost() {
        return ResponseEntity.ok().build();
    }
}
