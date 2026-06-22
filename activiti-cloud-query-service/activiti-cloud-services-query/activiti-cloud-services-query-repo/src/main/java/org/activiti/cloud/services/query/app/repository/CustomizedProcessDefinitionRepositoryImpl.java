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

import com.querydsl.core.types.Predicate;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.activiti.cloud.services.query.model.ProcessDefinitionEntity;
import org.activiti.cloud.services.query.model.QProcessDefinitionEntity;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;

public class CustomizedProcessDefinitionRepositoryImpl
    extends QuerydslRepositorySupport
    implements CustomizedProcessDefinitionRepository {

    private final JPAQueryFactory queryFactory;

    public CustomizedProcessDefinitionRepositoryImpl(EntityManager entityManager) {
        super(ProcessDefinitionEntity.class);
        this.queryFactory = new JPAQueryFactory(entityManager);
    }

    @Override
    public List<ProcessDefinitionEntity> findAllLatestVersions(Predicate predicate) {
        QProcessDefinitionEntity pd = QProcessDefinitionEntity.processDefinitionEntity;
        QProcessDefinitionEntity pdInner = new QProcessDefinitionEntity("pdInner");

        return queryFactory
            .selectFrom(pd)
            .where(
                predicate,
                pd.version.eq(
                    JPAExpressions.select(pdInner.version.max()).from(pdInner).where(pdInner.key.eq(pd.key))
                )
            )
            .fetch();
    }
}
