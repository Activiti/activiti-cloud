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
package org.activiti.cloud.services.query.model.dialect;

import org.hibernate.type.descriptor.converter.spi.BasicValueConverter;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.StringJavaType;
import org.hibernate.type.descriptor.jdbc.JdbcLiteralFormatter;
import org.hibernate.type.descriptor.jdbc.VarcharJdbcType;
import org.hibernate.type.internal.BasicTypeImpl;

public class DoubleQuotedStringType extends BasicTypeImpl<String> {

    private final RelationalFormType relationalFormType;

    public DoubleQuotedStringType() {
        this(RelationalFormType.EQUALS);
    }

    public DoubleQuotedStringType(RelationalFormType relationalFormType) {
        super(StringJavaType.INSTANCE, VarcharJdbcType.INSTANCE);
        this.relationalFormType = relationalFormType;
    }

    @Override
    public BasicValueConverter<String, String> getValueConverter() {
        return new BasicValueConverter<>() {
            @Override
            public String toDomainValue(String relationalForm) {
                return relationalForm.replace(".*", "").replace("(?i)", "");
            }

            @Override
            public String toRelationalValue(String domainForm) {
                return switch (relationalFormType) {
                    case EQUALS -> domainForm;
                    case LIKE_CASE_SENSITIVE -> ".*" + domainForm + ".*";
                    case LIKE_CASE_INSENSITIVE -> "(?i).*" + domainForm + ".*";
                };
            }

            @Override
            public JavaType<String> getDomainJavaType() {
                return StringJavaType.INSTANCE;
            }

            @Override
            public JavaType<String> getRelationalJavaType() {
                return StringJavaType.INSTANCE;
            }
        };
    }

    @Override
    public JdbcLiteralFormatter<String> getJdbcLiteralFormatter() {
        return (appender, value, dialect, wrapperOptions) -> {
            appender.append("\"");
            appender.append(value);
            appender.append("\"");
        };
    }
}
