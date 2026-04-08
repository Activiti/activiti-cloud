/*
 * Copyright 2017-2026 Hyland Software, Inc. and its affiliates.
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
package org.activiti.cloud.services.common.security.jwt.validator;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class ExpiredValidationCheckTest {

    private static final long OFFSET = 1000L;

    private ExpiredValidationCheck expiredValidationCheck;

    @BeforeEach
    void setUp() {
        expiredValidationCheck = new ExpiredValidationCheck(OFFSET);
    }

    @Test
    void shouldReturnTrueWhenTokenIsNotExpiredWithNegativeOffset() {
        expiredValidationCheck = new ExpiredValidationCheck(-90000);
        Jwt jwt = buildJwt().expiresAt(java.time.Instant.now().plusSeconds(60)).build();

        assertTrue(expiredValidationCheck.isValid(jwt));
    }

    @Test
    void shouldReturnTrueWhenTokenIsNotExpired() {
        Jwt jwt = buildJwt().expiresAt(java.time.Instant.now().plusSeconds(60)).build();

        assertTrue(expiredValidationCheck.isValid(jwt));
    }

    @Test
    void shouldReturnFalseWhenTokenIsExpired() {
        Jwt jwt = buildJwt().expiresAt(java.time.Instant.now().minusSeconds(60)).build();

        assertFalse(expiredValidationCheck.isValid(jwt));
    }

    private Jwt.@NonNull Builder buildJwt() {
        return Jwt.withTokenValue("token").header("alg", "none").claim("sub", "user");
    }
}
