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
package org.activiti.cloud.services.query.app.repository;

import java.util.List;

import org.activiti.cloud.services.query.model.BPMNActivityEntity;
import org.activiti.cloud.services.query.model.BPMNActivityEntity.BPMNActivityStatus;
import org.activiti.cloud.services.query.model.QBPMNActivityEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.querydsl.binding.QuerydslBinderCustomizer;
import org.springframework.data.querydsl.binding.QuerydslBindings;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.StringPath;

@RepositoryRestResource(exported = false)
public interface BPMNActivityRepository extends PagingAndSortingRepository<BPMNActivityEntity, String>,
                                                QuerydslPredicateExecutor<BPMNActivityEntity>,
                                                QuerydslBinderCustomizer<QBPMNActivityEntity> {

    @Override
    default void customize(QuerydslBindings bindings,
                           QBPMNActivityEntity root) {

        bindings.bind(String.class).first(
                (StringPath path, String value) -> path.eq(value));
    }

    List<BPMNActivityEntity> findByProcessInstanceIdAndStatus(String processInstanceId,
                                                              BPMNActivityStatus status);

    List<BPMNActivityEntity> findByProcessInstanceId(String processInstanceId);

    BPMNActivityEntity findByProcessInstanceIdAndElementId(String processInstanceId,
                                                           String elementId);

    BPMNActivityEntity findByProcessInstanceIdAndElementIdAndExecutionId(String processInstanceId,
                                                                         String elementId,
                                                                         String executionId);

    Page<BPMNActivityEntity> findByActivityType(String activityType,
                                                Predicate predicate,
                                                Pageable pagable);


}