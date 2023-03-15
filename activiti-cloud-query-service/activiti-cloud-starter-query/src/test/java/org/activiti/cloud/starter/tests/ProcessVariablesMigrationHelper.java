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
package org.activiti.cloud.starter.tests;

import java.io.IOException;
import java.math.BigInteger;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
public class ProcessVariablesMigrationHelper {

    @PersistenceContext
    private EntityManager entityManager;

    @Value("classpath:sql/task_process_variable_migration.sql")
    private Resource sqlFile;

    @Transactional
    public BigInteger getTaskProcessVariableCount(String taskId) {
        return (BigInteger) entityManager
            .createNativeQuery("select count(*) from task_process_variable tpv where tpv.task_id = '" + taskId + "'")
            .getSingleResult();
    }

    @Transactional
    public void deleteFromTaskProcessVariable(String taskId) {
        entityManager
            .createNativeQuery("delete from task_process_variable tpv where tpv.task_id = '" + taskId + "'")
            .executeUpdate();
    }

    @Transactional
    public void migrateTaskProcessVariableData() throws IOException {
        entityManager.createNativeQuery(new String(sqlFile.getInputStream().readAllBytes())).executeUpdate();
    }
}
