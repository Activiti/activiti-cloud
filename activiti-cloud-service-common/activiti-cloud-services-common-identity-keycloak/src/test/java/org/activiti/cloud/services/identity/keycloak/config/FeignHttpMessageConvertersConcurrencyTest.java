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
package org.activiti.cloud.services.identity.keycloak.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.openfeign.support.FeignHttpMessageConverters;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(
    classes = ActivitiKeycloakAutoConfiguration.class,
    properties = {
        "keycloak.auth-server-url=http://localhost:8080/auth",
        "keycloak.realm=test-realm",
        "activiti.keycloak.client-id=test-client",
        "activiti.keycloak.client-secret=test-secret",
    }
)
class FeignHttpMessageConvertersConcurrencyTest {

    private static final int THREAD_COUNT = 10;

    @MockitoBean
    private OAuth2AuthorizedClientService oAuth2AuthorizedClientService;

    @MockitoBean(answers = Answers.RETURNS_DEEP_STUBS)
    private ClientRegistrationRepository clientRegistrationRepository;

    @Autowired
    private FeignHttpMessageConverters feignHttpMessageConverters;

    @Test
    void should_returnNonEmptyConverters_underConcurrentAccess() throws Exception {
        CyclicBarrier barrier = new CyclicBarrier(THREAD_COUNT);
        try (ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT)) {
            List<Future<List<HttpMessageConverter<?>>>> futures = new ArrayList<>();
            for (int i = 0; i < THREAD_COUNT; i++) {
                futures.add(
                    executor.submit(() -> {
                        barrier.await();
                        return feignHttpMessageConverters.getConverters();
                    })
                );
            }
            for (Future<List<HttpMessageConverter<?>>> future : futures) {
                assertThat(future.get(5, TimeUnit.SECONDS).isEmpty()).isFalse();
            }
        }
    }
}
