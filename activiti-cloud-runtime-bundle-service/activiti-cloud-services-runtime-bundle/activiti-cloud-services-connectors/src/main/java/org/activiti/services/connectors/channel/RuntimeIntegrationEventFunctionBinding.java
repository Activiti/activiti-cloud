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

package org.activiti.services.connectors.channel;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.activiti.cloud.common.messaging.functional.FunctionBinding;
import org.activiti.engine.ActivitiOptimisticLockingException;
import org.springframework.core.annotation.AliasFor;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;

@Retention( RetentionPolicy.RUNTIME )
@Target( {ElementType.METHOD, ElementType.TYPE} )
@FunctionBinding(retryable = @Retryable(value = ActivitiOptimisticLockingException.class,
                                        maxAttemptsExpression = "${activiti.cloud.runtime.integration.retryable.max-attempts:3}",
                                        backoff = @Backoff(delayExpression = "${activiti.cloud.runtime.integration.retryable.backoff.delay:0}")))
public @interface RuntimeIntegrationEventFunctionBinding {
    @AliasFor(annotation = FunctionBinding.class, attribute = "input")
    String value();
}
