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
package org.activiti.cloud.util;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Date;

public abstract class DateUtils {

    public static final DateTimeFormatter ISO_DATE_TIME_FORMATTER = new DateTimeFormatterBuilder()
        .append(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        .optionalStart()
        .appendOffset("+HH:MM", "+00:00")
        .optionalEnd()
        .optionalStart()
        .appendOffset("+HHMM", "+0000")
        .optionalEnd()
        .optionalStart()
        .appendOffset("+HH", "Z")
        .optionalEnd()
        .toFormatter();

    public static Date parseDate(String stringDate) {
        return Date.from(Instant.from(DateUtils.ISO_DATE_TIME_FORMATTER.parse(stringDate)));
    }
}
