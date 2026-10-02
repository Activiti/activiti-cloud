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

import static org.activiti.cloud.services.query.app.repository.QuerydslBindingsHelper.whitelist;

import com.querydsl.core.types.dsl.StringPath;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.activiti.api.process.model.ProcessInstance;
import org.activiti.cloud.services.query.model.QTaskCandidateUserEntity;
import org.activiti.cloud.services.query.model.TaskCandidateUserEntity;
import org.activiti.cloud.services.query.model.TaskCandidateUserId;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.querydsl.binding.QuerydslBinderCustomizer;
import org.springframework.data.querydsl.binding.QuerydslBindings;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

public interface TaskCandidateUserRepository
    extends
        PagingAndSortingRepository<TaskCandidateUserEntity, TaskCandidateUserId>,
        QuerydslPredicateExecutor<TaskCandidateUserEntity>,
        QuerydslBinderCustomizer<QTaskCandidateUserEntity>,
        CrudRepository<TaskCandidateUserEntity, TaskCandidateUserId>
{
    Set<TaskCandidateUserEntity> findByTaskIdIn(Collection<String> taskIds);

    Set<TaskCandidateUserEntity> findByTask_ProcessInstanceIdIn(Collection<String> processInstanceIds);

    @Query(
        "select tcu.userId as userId, tcu.task.processInstanceId as processInstanceId " +
            "from TaskCandidateUser tcu where tcu.userId in :userIds and tcu.task.processInstance.status = :status"
    )
    List<CandidateProcess> findRunningProcessesByCandidateUserIn(
        @Param("userIds") Collection<String> userIds,
        @Param("status") ProcessInstance.ProcessInstanceStatus status
    );

    interface CandidateProcess {
        String getUserId();
        String getProcessInstanceId();
    }

    @Override
    default void customize(QuerydslBindings bindings, QTaskCandidateUserEntity root) {
        whitelist(root).apply(bindings);

        bindings.bind(String.class).first((StringPath path, String value) -> path.eq(value));
    }
}
