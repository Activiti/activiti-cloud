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

public class JsonValueFunctions {

    public static final String VALUE_EQUALS = "jsonb_value_eq";
    public static final String NUMERIC_EQUALS = "jsonb_numeric_eq";
    public static final String NUMERIC_GREATER_THAN = "jsonb_numeric_gt";
    public static final String NUMERIC_GREATER_THAN_EQUAL = "jsonb_numeric_gte";
    public static final String NUMERIC_LESS_THAN = "jsonb_numeric_lt";
    public static final String NUMERIC_LESS_THAN_EQUAL = "jsonb_numeric_lte";
    public static final String LIKE_CASE_SENSITIVE = "jsonb_value_like_cs";
    public static final String LIKE_CASE_INSENSITIVE = "jsonb_value_like_ci";
    public static final String DATE_EQUALS = "jsonb_date_eq";
    public static final String DATE_GREATER_THAN = "jsonb_date_gt";
    public static final String DATE_GREATER_THAN_EQUAL = "jsonb_date_gte";
    public static final String DATE_LESS_THAN = "jsonb_date_lt";
    public static final String DATE_LESS_THAN_EQUAL = "jsonb_date_lte";
    public static final String DATETIME_EQUALS = "jsonb_datetime_eq";
    public static final String DATETIME_GREATER_THAN = "jsonb_datetime_gt";
    public static final String DATETIME_GREATER_THAN_EQUAL = "jsonb_datetime_gte";
    public static final String DATETIME_LESS_THAN = "jsonb_datetime_lt";
    public static final String DATETIME_LESS_THAN_EQUAL = "jsonb_datetime_lte";
}
