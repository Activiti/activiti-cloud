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
import java.sql.Statement;
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

    @Test
    void shouldCreateActivityTypeStartedDateIndexForBpmnActivity() throws Exception {
        boolean foundActivityTypeColumn = false;
        boolean foundStartedDateColumn = false;

        try (
            Connection connection = dataSource.getConnection();
            ResultSet indexes = connection.getMetaData().getIndexInfo(null, null, "BPMN_ACTIVITY", false, false)
        ) {
            while (indexes.next()) {
                if ("BPMN_ACTIVITY_ACTIVITYTYPE_STARTEDDATE_IDX".equalsIgnoreCase(indexes.getString("INDEX_NAME"))) {
                    String columnName = indexes.getString("COLUMN_NAME");
                    if (
                        "ACTIVITY_TYPE".equalsIgnoreCase(columnName) &&
                        indexes.getShort("ORDINAL_POSITION") == 1 &&
                        "A".equalsIgnoreCase(indexes.getString("ASC_OR_DESC"))
                    ) {
                        foundActivityTypeColumn = true;
                    } else if (
                        "STARTED_DATE".equalsIgnoreCase(columnName) &&
                        indexes.getShort("ORDINAL_POSITION") == 2 &&
                        "D".equalsIgnoreCase(indexes.getString("ASC_OR_DESC"))
                    ) {
                        foundStartedDateColumn = true;
                    }
                }
            }
        }

        assertThat(foundActivityTypeColumn).isTrue();
        assertThat(foundStartedDateColumn).isTrue();
    }

    @Test
    void shouldCreateRootUnlinkedStartDateIndexForProcessInstance() throws Exception {
        boolean foundStartDateColumn = false;

        try (
            Connection connection = dataSource.getConnection();
            ResultSet indexes = connection.getMetaData().getIndexInfo(null, null, "PROCESS_INSTANCE", false, false)
        ) {
            while (indexes.next()) {
                if (
                    "PI_ROOT_UNLINKED_STARTDATE_IDX".equalsIgnoreCase(indexes.getString("INDEX_NAME")) &&
                    "START_DATE".equalsIgnoreCase(indexes.getString("COLUMN_NAME")) &&
                    "D".equalsIgnoreCase(indexes.getString("ASC_OR_DESC"))
                ) {
                    foundStartDateColumn = true;
                    break;
                }
            }
        }

        assertThat(foundStartDateColumn).isTrue();
    }

    @Test
    void shouldCreateAllowSelfServiceColumnOnTask() throws Exception {
        boolean foundColumn = false;

        try (
            Connection connection = dataSource.getConnection();
            ResultSet columns = connection.getMetaData().getColumns(null, null, "TASK", "ALLOW_SELF_SERVICE")
        ) {
            if (columns.next()) {
                foundColumn = true;
            }
        }

        assertThat(foundColumn).isTrue();
    }

    @Test
    void shouldDefaultAllowSelfServiceToFalseWhenColumnOmitted() throws Exception {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("INSERT INTO task (id, priority) VALUES ('task-allow-self-service-default', 0)");

            try (
                ResultSet result = statement.executeQuery(
                    "SELECT allow_self_service FROM task WHERE id = 'task-allow-self-service-default'"
                )
            ) {
                assertThat(result.next()).isTrue();
                assertThat(result.getBoolean("allow_self_service")).isFalse();
            }
        }
    }
}
