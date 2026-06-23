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
package org.activiti.cloud.query.init;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;
import javax.xml.parsers.DocumentBuilderFactory;
import org.activiti.cloud.query.liquibase.QueryLiquibaseApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import tools.jackson.databind.json.JsonMapper;

/**
 * Integration test that verifies the Liquibase changeset 40
 * ({@code 40-alter.pg.schema.9.1.0.sql}) correctly migrates pre-existing
 * process hierarchy data into the new {@code process_instance_hierarchy}
 * closure table.
 *
 * <p>Strategy:
 * <ol>
 *   <li>Disable Spring's automatic Liquibase run for this test context.</li>
 *   <li>Programmatically execute the SQL files referenced by the master
 *       changelog up to (but excluding) the changeSet that creates the
 *       hierarchy (i.e. apply changesets 1..39).</li>
 *   <li>Populate {@code process_instance} rows to simulate existing data.</li>
 *   <li>Execute the full changeset 40 script (the migration) as a single
 *       statement and assert the closure table contents.</li>
 * </ol>
 */
@SpringBootTest(
    classes = { QueryLiquibaseApplication.class, ProcessHierarchyMigrationIT.TestEntityScanConfig.class },
    properties = { "spring.jpa.hibernate.ddl-auto=none", "spring.liquibase.enabled=false" }
)
@Testcontainers
class ProcessHierarchyMigrationIT {

    @Configuration
    @EntityScan(
        basePackages = { "org.activiti.cloud.services.query.model", "org.activiti.cloud.services.audit.jpa.events" }
    )
    static class TestEntityScanConfig {}

    @MockitoBean
    private JsonMapper objectMapper;

    private static final String MASTER_CHANGELOG = "config/query/liquibase/master.xml";
    private static final String CHANGESET_40_ID = "alter40-schema"; // id used in master.xml for pg changeSet 40
    private static final String MIGRATION_SCRIPT_PATH = "config/query/liquibase/changelog/40-alter.pg.schema.9.1.0.sql";

    // Strip a leading /* ... */ block from the start of SQL content, if present.
    private static String stripLeadingBlockComment(String sql) {
        if (sql == null) {
            return null;
        }
        // remove optional UTF-8 BOM
        if (sql.startsWith("\uFEFF")) {
            sql = sql.substring(1);
        }
        String trimmed = sql.trim();
        if (trimmed.startsWith("/*")) {
            int endIdx = trimmed.indexOf("*/");
            if (endIdx != -1) {
                return trimmed.substring(endIdx + 2).trim();
            }
            // unterminated block comment: return original to let caller handle or fail
            return trimmed;
        }
        return sql;
    }

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:15-alpine").waitingFor(
        Wait.forListeningPort()
    );

    @Autowired
    private DataSource dataSource;

    @Test
    void should_migrateProcessInstancesRelationships_intoProcessHierarchyTable() throws Exception {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);

        // Step 0: Apply changeSets 1..39 by parsing master.xml and executing referenced sqlFiles
        applyChangeSetsUpTo39();

        // Step 1: Populate process_instance table (existing data before migration)
        insertProcess(jdbc, "A", null, null);
        insertProcess(jdbc, "B", "A", null);
        insertProcess(jdbc, "C", "B", null);
        insertProcess(jdbc, "D", null, "A");
        insertProcess(jdbc, "E", null, null);
        jdbc.update("INSERT INTO process_instance (id, parent_id) VALUES (?, ?)", "F", "F");

        // Sanity: ensure process rows present
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM process_instance", Integer.class)).isEqualTo(6);

        // Step 2: Execute the full migration script 40 (single execute)
        executeMigrationScript();

        assertHierarchyRow(jdbc, "A", "A", 0, "self");
        assertHierarchyRow(jdbc, "B", "B", 0, "self");
        assertHierarchyRow(jdbc, "C", "C", 0, "self");
        assertHierarchyRow(jdbc, "D", "D", 0, "self");
        assertHierarchyRow(jdbc, "E", "E", 0, "self");
        assertHierarchyRow(jdbc, "F", "F", 0, "self");
        assertHierarchyRow(jdbc, "A", "B", 1, "subprocess");
        assertHierarchyRow(jdbc, "B", "C", 1, "subprocess");
        assertHierarchyRow(jdbc, "A", "C", 2, "subprocess");
        assertHierarchyRow(jdbc, "A", "D", 1, "linked");
        assertThat(countRowsInvolving(jdbc, "E")).isEqualTo(1);
        assertThat(countRowsInvolving(jdbc, "F")).isEqualTo(1);
    }

    private void applyChangeSetsUpTo39() throws Exception {
        // Read master.xml and iterate changeSet nodes until we encounter changeSet with id=CHANGESET_40_ID
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(MASTER_CHANGELOG)) {
            if (is == null) {
                throw new IllegalStateException("Master changelog not found: " + MASTER_CHANGELOG);
            }
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(is);
            Element root = doc.getDocumentElement();
            NodeList changeSets = root.getElementsByTagName("changeSet");
            String baseDir = "config/query/liquibase/";

            try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
                for (int i = 0; i < changeSets.getLength(); i++) {
                    Element cs = (Element) changeSets.item(i);
                    String id = cs.getAttribute("id");
                    if (CHANGESET_40_ID.equals(id)) {
                        // stop before changeSet 40
                        break;
                    }
                    // find sqlFile child elements
                    NodeList sqlFiles = cs.getElementsByTagName("sqlFile");
                    for (int j = 0; j < sqlFiles.getLength(); j++) {
                        Element sqlFile = (Element) sqlFiles.item(j);
                        String dbms = sqlFile.getAttribute("dbms");
                        // execute only if dbms is empty or includes postgresql
                        if (!dbms.isEmpty() && !dbms.contains("postgresql")) {
                            continue;
                        }
                        String path = sqlFile.getAttribute("path");
                        String relative = sqlFile.getAttribute("relativeToChangelogFile");
                        String resourcePath = path;
                        if ("true".equalsIgnoreCase(relative) || !path.startsWith("/")) {
                            resourcePath = baseDir + path;
                        }
                        try (InputStream fis = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
                            if (fis == null) {
                                throw new IllegalStateException(
                                    "Referenced SQL file not found on classpath: " + resourcePath
                                );
                            }
                            String sql = new String(fis.readAllBytes(), StandardCharsets.UTF_8);
                            sql = stripLeadingBlockComment(sql);
                            boolean splitStatements = "true".equalsIgnoreCase(sqlFile.getAttribute("splitStatements"));
                            if (splitStatements) {
                                // Honor Liquibase splitStatements: execute each ';'-delimited statement
                                // separately. Required because statements like
                                // `CREATE INDEX CONCURRENTLY` cannot run inside a JDBC pipeline.
                                for (String part : sql.split(";")) {
                                    String trimmed = part.trim();
                                    if (!trimmed.isEmpty()) {
                                        stmt.execute(trimmed);
                                    }
                                }
                            } else {
                                stmt.execute(sql);
                            }
                        }
                    }
                }
            }
        }
    }

    private void executeMigrationScript() throws SQLException {
        String sql;
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(MIGRATION_SCRIPT_PATH)) {
            assertThat(is).as("Migration script not found on classpath: %s", MIGRATION_SCRIPT_PATH).isNotNull();
            sql = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            sql = stripLeadingBlockComment(sql);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read migration script: " + MIGRATION_SCRIPT_PATH, e);
        }
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }

    private void insertProcess(JdbcTemplate jdbc, String id, String parentId, String linkedId) {
        jdbc.update(
            "INSERT INTO process_instance (id, parent_id, linked_process_instance_id) VALUES (?, ?, ?)",
            id,
            parentId,
            linkedId
        );
    }

    private void assertHierarchyRow(
        JdbcTemplate jdbc,
        String ancestorId,
        String descendantId,
        int depth,
        String relationType
    ) {
        Integer count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM process_instance_hierarchy WHERE ancestor_id = ? AND descendant_id = ? AND depth = ? AND relation_type = ?",
            Integer.class,
            ancestorId,
            descendantId,
            depth,
            relationType
        );
        assertThat(count).isEqualTo(1);
    }

    private int countRowsInvolving(JdbcTemplate jdbc, String processId) {
        Integer count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM process_instance_hierarchy WHERE ancestor_id = ? OR descendant_id = ?",
            Integer.class,
            processId,
            processId
        );
        return count != null ? count : 0;
    }
}
