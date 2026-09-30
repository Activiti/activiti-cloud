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
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.activiti.api.task.model.Task;
import org.activiti.cloud.services.query.model.QTaskEntity;
import org.activiti.cloud.services.query.model.TaskEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.querydsl.binding.QuerydslBinderCustomizer;
import org.springframework.data.querydsl.binding.QuerydslBindings;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

public interface TaskRepository
    extends
        PagingAndSortingRepository<TaskEntity, String>,
        CustomizedJpaSpecificationExecutor<TaskEntity>,
        QuerydslPredicateExecutor<TaskEntity>,
        QuerydslBinderCustomizer<QTaskEntity>,
        CustomizedTaskRepository,
        CrudRepository<TaskEntity, String>
{
    List<TaskEntity> findByProcessInstanceIdIn(Collection<String> processInstanceIds);

    // Assignees with no task in the given status are absent from the result (their count is zero);
    // mirrors the predicate the REST count endpoint applies for {status:[ASSIGNED], assignee:[user]}.
    @Query(
        "select t.assignee as assignee, count(t) as taskCount " +
            "from Task t " +
            "where t.assignee in :assignees and t.status = :status " +
            "group by t.assignee"
    )
    List<AssigneeCount> countGroupedByAssignee(
        @Param("assignees") Collection<String> assignees,
        @Param("status") Task.TaskStatus status
    );

    interface AssigneeCount {
        String getAssignee();
        long getTaskCount();
    }

    // Per user, the number of unassigned tasks in the given status they are personally a candidate on that
    // none of the given groups can already see (NOT EXISTS a candidate group in :groups). Excluding the
    // group-visible tasks keeps this "personal remainder" disjoint from the shared group-visible count so
    // the two can be summed without double-counting. Users with none are absent from the result (zero).
    @Query(
        "select cu.userId as userId, count(distinct t.id) as taskCount " +
            "from TaskCandidateUser cu join cu.task t " +
            "where cu.userId in :userIds " +
            "and t.status = :status " +
            "and t.assignee is null " +
            "and not exists (" +
            "select cg.taskId from TaskCandidateGroup cg " +
            "where cg.taskId = t.id and cg.groupId in :groups" +
            ") " +
            "group by cu.userId"
    )
    List<UserCount> countQueuedPersonalRemainderGroupedByUser(
        @Param("userIds") Collection<String> userIds,
        @Param("status") Task.TaskStatus status,
        @Param("groups") Collection<String> groups
    );

    interface UserCount {
        String getUserId();
        long getTaskCount();
    }

    @Override
    default void customize(QuerydslBindings bindings, QTaskEntity root) {
        bindings.bind(String.class).first((StringPath path, String value) -> path.eq(value));
        bindings.bind(root.createdFrom).first((path, value) -> root.createdDate.after(value));
        bindings.bind(root.createdTo).first((path, value) -> root.createdDate.before(value));
        bindings.bind(root.lastModifiedFrom).first((path, value) -> root.lastModified.after(value));
        bindings.bind(root.lastModifiedTo).first((path, value) -> root.lastModified.before(value));
        bindings.bind(root.lastClaimedFrom).first((path, value) -> root.claimedDate.after(value));
        bindings.bind(root.lastClaimedTo).first((path, value) -> root.claimedDate.before(value));
        bindings.bind(root.completedFrom).first((path, value) -> root.completedDate.after(value));
        bindings.bind(root.completedTo).first((path, value) -> root.completedDate.before(value));
        bindings.bind(root.dueDateFrom).first((path, value) -> root.dueDate.after(value));
        bindings.bind(root.dueDateTo).first((path, value) -> root.dueDate.before(value));
        bindings
            .bind(root.candidateGroupId)
            .first((path, value) -> root.taskCandidateGroups.any().groupId.in(Arrays.asList(value.split(","))));

        bindings.bind(root.name).first((path, value) -> path.like("%" + value.toString() + "%"));
        bindings.bind(root.description).first((path, value) -> path.like("%" + value.toString() + "%"));

        whitelist(root)
            .excluding(root.variables)
            .excluding(root.processVariables)
            .excluding(root.standalone)
            .apply(bindings);
    }
}
