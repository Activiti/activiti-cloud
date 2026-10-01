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
package org.activiti.cloud.services.query.rest.config;

import java.util.Map;
import org.activiti.cloud.dialect.CustomPostgreSQLDialect;
import org.hibernate.cfg.MappingSettings;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;

@AutoConfiguration(after = DataSourceAutoConfiguration.class)
@ConditionalOnExpression(
    "'${spring.jpa.database-platform}'.toLowerCase().contains('postgres') or '${spring.datasource.url}'.toLowerCase().contains('postgres')"
)
public class CustomHibernateAutoConfiguration implements HibernatePropertiesCustomizer {

    @Override
    public void customize(Map<String, Object> hibernateProperties) {
        hibernateProperties.put("hibernate.dialect", CustomPostgreSQLDialect.class.getName());
        // NOTE: we intentionally do NOT force a global "nulls last" default here anymore.
        // That previously forced every DESC sort (including non-nullable columns such as
        // startedDate/timestamp) to render an explicit "NULLS LAST", which prevents Postgres
        // from using a plain ascending index via backward scan and forces an expensive sort.
        // The only place that genuinely needs null-last ordering is process variable sorting
        // (variable values can be legitimately null), which now requests it explicitly in
        // SpecificationSupport#applySorting instead of relying on this global setting.
        hibernateProperties.put(MappingSettings.JSON_FORMAT_MAPPER, new Jackson3JsonFormatMapper());
    }
}
