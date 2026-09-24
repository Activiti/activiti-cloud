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
package org.activiti.services.connectors.mtc;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

@Order
public class MtcBinderEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String PROPERTY_SOURCE_NAME = "mtcBinderProperties";
    private static final String MTC_BINDER_NAME = "mtc";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Boolean mtcEnabled = environment.getProperty("mtc.connector.enabled", Boolean.class, false);
        if (!mtcEnabled) {
            return;
        }

        Map<String, Object> props = new LinkedHashMap<>();

        props.put("spring.cloud.stream.default-binder", "rabbit");
        props.put("spring.cloud.stream.binders.rabbit.type", "rabbit");
        props.put("spring.cloud.stream.binders.rabbit.inherit-environment", "true");
        props.put("spring.cloud.stream.binders.rabbit.default-candidate", "true");

        String binderPrefix = "spring.cloud.stream.binders." + MTC_BINDER_NAME;
        props.put(binderPrefix + ".type", "rabbit");
        props.put(binderPrefix + ".inherit-environment", "true");
        props.put(binderPrefix + ".default-candidate", "false");
        props.put(binderPrefix + ".environment.spring.cloud.stream.rabbit.default.producer.prefix", "");
        props.put(binderPrefix + ".environment.spring.cloud.stream.rabbit.default.consumer.prefix", "");

        props.put("spring.cloud.stream.bindings.mtcResultsConsumer.binder", MTC_BINDER_NAME);

        environment.getPropertySources().addLast(new MapPropertySource(PROPERTY_SOURCE_NAME, props));
    }
}
