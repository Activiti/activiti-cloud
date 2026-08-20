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
package org.activiti.cloud.services.query.liquibase;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.ResultSet;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@SpringBootApplication
class ActivitiCloudQueryLiquibaseAutoConfigurationIT {

    @Autowired
    private DataSource dataSource;

    @Test
    void contextLoads() {
        // application context loads successfully
    }

    @Test
    void shouldCreateParentIdIndexForProcessInstance() throws Exception {
        boolean foundParentIdIndex = false;

        try (
            Connection connection = dataSource.getConnection();
            ResultSet indexes = connection.getMetaData().getIndexInfo(null, null, "PROCESS_INSTANCE", false, false)
        ) {
            while (indexes.next()) {
                if (
                    "PI_PARENTID_IDX".equalsIgnoreCase(indexes.getString("INDEX_NAME")) &&
                    "PARENT_ID".equalsIgnoreCase(indexes.getString("COLUMN_NAME"))
                ) {
                    foundParentIdIndex = true;
                    break;
                }
            }
        }

        assertThat(foundParentIdIndex).isTrue();
    }
}
