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
package org.activiti.cloud.services.audit.jpa.events;

import java.util.Optional;

class StringUtils {

    /**
     *   Truncate a String to the given length with no warnings or error raised if it is bigger.
     *
     *   @param  value String to be truncated
     *   @param  length  Maximum length of string
     *
     *   @return Returns value if value is null or value.length() is less or equal to than length, otherwise a String representing
     *   value truncated to length.
     */
    public static String truncate(String value, Integer length) {
        return Optional
            .ofNullable(value)
            .filter(it -> it.length() > length)
            .map(it -> it.substring(0, length))
            .orElse(value);
    }
}
