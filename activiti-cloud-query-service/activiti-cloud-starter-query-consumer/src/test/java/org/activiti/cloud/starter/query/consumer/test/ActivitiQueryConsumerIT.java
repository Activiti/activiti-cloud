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
package org.activiti.cloud.starter.query.consumer.test;

import static org.assertj.core.api.Assertions.assertThat;

import com.zaxxer.hikari.HikariDataSource;
import java.util.function.Supplier;
import org.activiti.cloud.conf.FixedQueryConsumerPartitionedChannelCountProvider;
import org.activiti.cloud.conf.QueryConsumerPartitionedChannelCountProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.EnableTestBinder;

@SpringBootTest(
    classes = QueryConsumerTestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = "activiti.cloud.services.oauth2.iam-name=test"
)
@EnableTestBinder
public class ActivitiQueryConsumerIT {

    @Autowired
    private QueryConsumerPartitionedChannelCountProvider queryConsumerPartitionedChannelCountProvider;

    @Autowired
    private HikariDataSource hikariDataSource;

    @Test
    void contextLoads() {
        assertThat(queryConsumerPartitionedChannelCountProvider)
            .isInstanceOf(FixedQueryConsumerPartitionedChannelCountProvider.class)
            .extracting(Supplier::get)
            .isEqualTo(hikariDataSource.getMaximumPoolSize());
    }
}
