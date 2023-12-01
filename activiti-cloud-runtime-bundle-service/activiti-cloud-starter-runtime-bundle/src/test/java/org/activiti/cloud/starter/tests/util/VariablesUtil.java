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
package org.activiti.cloud.starter.tests.util;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Date;
import java.util.TimeZone;
import org.activiti.common.util.DateFormatterProvider;
import org.springframework.boot.test.context.TestComponent;

@TestComponent
public class VariablesUtil {

    private final DateFormatterProvider dateFormatterProvider;

    public VariablesUtil(DateFormatterProvider dateFormatterProvider) {
        this.dateFormatterProvider = dateFormatterProvider;
    }

    public String getDateFormattedString(Date date) throws Exception {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        format.setTimeZone(TimeZone.getTimeZone("UTC"));

        return format.format(date);
    }

    public LocalDateTime convertDateToLocalDate(Date date) {
        return date.toInstant().atZone(dateFormatterProvider.getZoneId()).toLocalDateTime();
    }

    public String formatLocalDateTimeStringWithPattern(LocalDateTime date, String datePattern) {
        return new DateTimeFormatterBuilder()
            .appendPattern(datePattern)
            .toFormatter()
            .withZone(dateFormatterProvider.getZoneId())
            .format(date);
    }

    public String getDateTimeFormattedString(Date date) throws Exception {
        return formatLocalDateTimeStringWithPattern(convertDateToLocalDate(date), "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    }

    public String getExpectedDateFormattedString(Date date) throws Exception {
        SimpleDateFormat expDTFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        expDTFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        return expDTFormat.format(dateFormatterProvider.parse(getDateFormattedString(date)));
    }

    public String getExpectedDateTimeFormattedString(Date date) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format.format(date);
    }
}
