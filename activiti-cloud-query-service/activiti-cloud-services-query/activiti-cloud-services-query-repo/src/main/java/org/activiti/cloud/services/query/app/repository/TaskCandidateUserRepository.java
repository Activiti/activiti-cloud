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
import org.activiti.api.task.model.Task;
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

    /**
     * How many unassigned tasks each of {@code userIds} is individually named on that <b>none</b> of
     * {@code excludedGroups} can already see.
     * <p>
     * This is the remainder half of a per-user queued count. For users who share a group set, the count
     * differs only in the {@code candidateUser = me} term, so the shared part is counted once with
     * {@code TaskSpecification.forGroups(filter, excludedGroups)} and this query supplies the rest -
     * <b>one</b> round trip returning one row per member who has any, rather than one COUNT per member. That
     * is what keeps the cost independent of how many people are in the group.
     * <p>
     * The {@code NOT EXISTS} is the whole correctness argument. It makes this a set <em>difference</em>
     * ("tasks I am named on that my groups cannot already see"), so the two halves are disjoint and adding
     * them is legal. Without it a task carrying both a candidate group in {@code excludedGroups} and a
     * candidate user row for the same person is counted twice. Visibility is a union, not a partition.
     * <p>
     * Users absent from the result have no such task; the caller must read that as zero, not as unknown.
     *
     * @param userIds        the bucket's members
     * @param statuses       task statuses to count, from the pinned pushed filter
     * @param excludedGroups the bucket's group set; <b>must not be empty</b>, since an empty JPQL {@code IN}
     *                       list is not portable - a subscriber holding no groups has no shared half to
     *                       subtract and is counted directly instead
     */
    @Query(
        """
        SELECT cu.userId AS userId, COUNT(DISTINCT cu.taskId) AS count
        FROM TaskCandidateUser cu
        JOIN cu.task t
        WHERE cu.userId IN :userIds
          AND t.status IN :statuses
          AND t.assignee IS NULL
          AND NOT EXISTS (
            SELECT cg.taskId FROM TaskCandidateGroup cg
            WHERE cg.taskId = cu.taskId AND cg.groupId IN :excludedGroups
          )
        GROUP BY cu.userId
        """
    )
    List<CandidateUserTaskCount> countTasksNamingUserOutsideGroups(
        @Param("userIds") Collection<String> userIds,
        @Param("statuses") Collection<Task.TaskStatus> statuses,
        @Param("excludedGroups") Collection<String> excludedGroups
    );

    /** One row of {@link #countTasksNamingUserOutsideGroups}: a user and their remainder. */
    interface CandidateUserTaskCount {
        String getUserId();

        long getCount();
    }

    @Override
    default void customize(QuerydslBindings bindings, QTaskCandidateUserEntity root) {
        whitelist(root).apply(bindings);

        bindings.bind(String.class).first((StringPath path, String value) -> path.eq(value));
    }
}
