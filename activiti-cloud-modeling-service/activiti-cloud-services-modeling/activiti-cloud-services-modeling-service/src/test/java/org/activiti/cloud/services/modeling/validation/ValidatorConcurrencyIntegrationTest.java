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
package org.activiti.cloud.services.modeling.validation;

import static org.activiti.cloud.modeling.api.ValidationContext.EMPTY_CONTEXT;
import static org.activiti.cloud.services.common.util.FileUtils.resourceAsByteArray;
import static org.awaitility.Awaitility.await;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import org.activiti.cloud.modeling.api.ConnectorModelType;
import org.activiti.cloud.modeling.api.ModelValidationError;
import org.activiti.cloud.modeling.api.config.ModelingApiAutoConfiguration;
import org.everit.json.schema.Schema;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ContextConfiguration(classes = { JsonSchemaModelValidatorConfiguration.class, ModelingApiAutoConfiguration.class })
public class ValidatorConcurrencyIntegrationTest {

    private static final int THREADS = 2;

    @Autowired
    @Qualifier("connectorSchema")
    public Schema connectorSchema;

    @Autowired
    public ConnectorModelType connectorModelType;

    private static ExecutorService executorService;

    @BeforeAll
    static void setUp() {
        executorService = Executors.newFixedThreadPool(THREADS);
    }

    @AfterAll
    static void cleanUp() {
        executorService.shutdown();
    }

    @Test
    public void should_notThrowException_when_validatingAValidConnectorConcurrently() throws InterruptedException {
        // given
        ConnectorModelValidator connectorModelValidator = new ConnectorModelValidator(
            connectorSchema,
            connectorModelType
        );

        Set<Callable<Collection<ModelValidationError>>> tasks = createTasks(connectorModelValidator, THREADS);

        // when
        List<Future<Collection<ModelValidationError>>> futureList = executorService.invokeAll(tasks);

        // then
        await()
            .untilAsserted(() -> {
                if (futureList.stream().map(Future::isDone).count() == THREADS) {
                    List<ModelValidationError> errors = futureList
                        .stream()
                        .map(f -> {
                            try {
                                return f.get();
                            } catch (Exception e) {
                                throw new UndeclaredThrowableException(e);
                            }
                        })
                        .filter(Objects::nonNull)
                        .flatMap(Collection::stream)
                        .collect(Collectors.toList());

                    assert (errors).isEmpty();
                }
            });
    }

    protected Set<Callable<Collection<ModelValidationError>>> createTasks(
        ConnectorModelValidator connectorModelValidator,
        int size
    ) {
        Set<Callable<Collection<ModelValidationError>>> tasks = new LinkedHashSet<>();

        for (int i = 0; i < size; i++) {
            tasks.add(() -> {
                return connectorModelValidator.validateModelContent(
                    resourceAsByteArray("connector/connector-with-model.json"),
                    EMPTY_CONTEXT
                );
            });
        }

        return tasks;
    }
}
