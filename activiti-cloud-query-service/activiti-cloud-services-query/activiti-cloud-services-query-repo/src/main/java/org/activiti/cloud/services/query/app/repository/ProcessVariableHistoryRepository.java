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
package org.activiti.cloud.services.query.app.repository;

import java.util.Date;
import java.util.List;
import org.activiti.cloud.services.query.model.ProcessVariableHistoryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface ProcessVariableHistoryRepository extends JpaRepository<ProcessVariableHistoryEntity, Long> {
    List<ProcessVariableHistoryEntity> findByProcessInstanceIdAndVariableNameOrderByEventTimeAscSequenceNumberAsc(
        String processInstanceId,
        String variableName
    );

    Page<ProcessVariableHistoryEntity> findByProcessInstanceIdOrderByEventTimeAscSequenceNumberAsc(
        String processInstanceId,
        Pageable pageable
    );

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM ProcessVariableHistory h WHERE h.recordCreateTime < :cutoff")
    int deleteByRecordCreateTimeBefore(@Param("cutoff") Date cutoff);
}
