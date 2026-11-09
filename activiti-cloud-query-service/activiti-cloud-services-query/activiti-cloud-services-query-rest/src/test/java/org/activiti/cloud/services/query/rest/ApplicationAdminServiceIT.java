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
package org.activiti.cloud.services.query.rest;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;
import org.activiti.QueryRestTestApplication;
import org.activiti.cloud.services.query.app.repository.ApplicationRepository;
import org.activiti.cloud.services.query.model.ApplicationEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(
    classes = { QueryRestTestApplication.class },
    properties = {
        "spring.main.banner-mode=off",
        "spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect",
    }
)
@TestPropertySource("classpath:application-test.properties")
@Testcontainers
@Transactional
class ApplicationAdminServiceIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:15-alpine").waitingFor(
        Wait.forListeningPort()
    );

    @Autowired
    private ApplicationRepository applicationRepository;

    private ApplicationAdminService service;

    @BeforeEach
    void setUp() {
        applicationRepository.deleteAll();
        service = new ApplicationAdminService(applicationRepository);
    }

    @Test
    void shouldReturnTwentyApplicationsSortedByVersionDescendingAcrossPages() {
        // saved in shuffled order so that insertion order cannot explain the result
        List<Integer> shuffled = IntStream.rangeClosed(1, 20).boxed().collect(java.util.stream.Collectors.toList());
        Collections.shuffle(shuffled, new java.util.Random(42));
        shuffled.forEach(i -> applicationRepository.save(application("app-" + i, "app", version(i))));

        Page<ApplicationEntity> firstPage = service.findAllSliced(null, PageRequest.of(0, 10));
        Page<ApplicationEntity> secondPage = service.findAllSliced(null, PageRequest.of(1, 10));

        assertThat(firstPage.getContent())
            .extracting(ApplicationEntity::getVersion)
            .containsExactly("20", "19", "18", "17", "16", "15", "14", "13", "12", "11");
        assertThat(firstPage.hasNext()).isTrue();
        assertThat(secondPage.getContent())
            .extracting(ApplicationEntity::getVersion)
            .containsExactly("10", "09", "08", "07", "06", "05", "04", "03", "02", "01");
        assertThat(secondPage.hasNext()).isFalse();
    }

    @Test
    void shouldUseIdDescendingAsTieBreakerWhenVersionsAreEqual() {
        for (int i = 1; i <= 20; i++) {
            applicationRepository.save(application(String.format("id-%02d", i), "app", "1"));
        }

        Page<ApplicationEntity> firstPage = service.findAllSliced(null, PageRequest.of(0, 10));
        Page<ApplicationEntity> secondPage = service.findAllSliced(null, PageRequest.of(1, 10));

        assertThat(firstPage.getContent())
            .extracting(ApplicationEntity::getId)
            .containsExactly("id-20", "id-19", "id-18", "id-17", "id-16", "id-15", "id-14", "id-13", "id-12", "id-11");
        assertThat(secondPage.getContent())
            .extracting(ApplicationEntity::getId)
            .containsExactly("id-10", "id-09", "id-08", "id-07", "id-06", "id-05", "id-04", "id-03", "id-02", "id-01");
    }

    @Test
    void shouldHonourRequestedSortOverDefault() {
        IntStream.rangeClosed(1, 20).forEach(i ->
            applicationRepository.save(application("app-" + i, "app", version(i)))
        );

        Page<ApplicationEntity> page = service.findAllSliced(
            null,
            PageRequest.of(0, 5, Sort.by("version").ascending())
        );

        assertThat(page.getContent())
            .extracting(ApplicationEntity::getVersion)
            .containsExactly("01", "02", "03", "04", "05");
    }

    private String version(int i) {
        return String.format("%02d", i);
    }

    private ApplicationEntity application(String id, String name, String version) {
        return new ApplicationEntity(id, name, version);
    }
}
