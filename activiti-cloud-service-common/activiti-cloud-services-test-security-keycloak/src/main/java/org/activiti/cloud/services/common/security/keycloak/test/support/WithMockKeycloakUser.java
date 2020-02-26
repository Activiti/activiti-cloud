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

package org.activiti.cloud.services.common.security.keycloak.test.support;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.test.context.support.TestExecutionEvent;
import org.springframework.security.test.context.support.WithSecurityContext;
import org.springframework.test.context.TestContext;

/**
 * Annotation for testing with mock Keycloak user
 */
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@WithSecurityContext(factory = WithMockKeycloakUserSecurityContextFactory.class)
public @interface WithMockKeycloakUser {

    String username() default "testuser";
    
    String[] roles() default {};

    String[] groups() default {};
    
    boolean isActive() default true;

    boolean isInteractive() default false;
    
    String rolePrefix() default "ROLE_";
    
    /**
     * Determines when the {@link SecurityContext} is setup. The default is before
     * {@link TestExecutionEvent#TEST_METHOD} which occurs during
     * {@link org.springframework.test.context.TestExecutionListener#beforeTestMethod(TestContext)}
     * @return the {@link TestExecutionEvent} to initialize before
     * @since 5.1
     */
    @AliasFor(annotation = WithSecurityContext.class)
    TestExecutionEvent setupBefore() default TestExecutionEvent.TEST_METHOD;    
}