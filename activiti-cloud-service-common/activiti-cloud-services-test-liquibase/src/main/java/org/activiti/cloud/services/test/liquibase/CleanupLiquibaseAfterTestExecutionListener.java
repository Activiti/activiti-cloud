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
package org.activiti.cloud.services.test.liquibase;

import java.util.Map;
import java.util.Optional;
import liquibase.exception.UnexpectedLiquibaseException;
import liquibase.integration.spring.SpringLiquibase;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;

public class CleanupLiquibaseAfterTestExecutionListener extends AbstractTestExecutionListener {

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public void afterTestClass(TestContext testContext) {
        Optional
            .of(testContext.getTestClass())
            .filter(testClass ->
                Optional
                    .ofNullable(AnnotationUtils.findAnnotation(testClass, EnableCleanupLiquibaseAfterTest.class))
                    .isPresent()
            )
            .map(testClass -> testContext.getApplicationContext().getBeansOfType(SpringLiquibase.class))
            .map(Map::values)
            .ifPresent(values -> {
                values.forEach(springLiquibase -> {
                    try {
                        springLiquibase.setDropFirst(true);
                        springLiquibase.afterPropertiesSet();
                    } catch (Exception e) {
                        throw new UnexpectedLiquibaseException(e);
                    }
                });
            });
    }
}
