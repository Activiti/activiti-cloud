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

package org.activiti.cloud.common.error.attributes;

import java.util.Arrays;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


public class ErrorAttributesMessageSanitizer implements ErrorAttributesCustomizer {

    public static final String MESSAGE = "message";
    public static final String ERROR_NOT_DISCLOSED_MESSAGE = "A technical error occurred. Additional information is not disclosed for security reasons.";
    public static final String[] TECHNICAL_INFO_BLACKLIST = {
        "java.",
        "javax.",
        "jakarta.",
        "java_cup.",
        "org.",
        "com.",
        "net.",
        "io.",
        "dev.",
        "de."
    };
    public static final Pattern VALIDATION_REGEX = compileValidationRegex();


    @Override
    public Map<String, Object> customize(Map<String, Object> errorAttributes, Throwable error) {
        if (errorAttributes.containsKey(MESSAGE)) {
            final String message = (String) errorAttributes.getOrDefault(MESSAGE, "");
            final String censoredMessage = containsTechnicalInfo(message) ? ERROR_NOT_DISCLOSED_MESSAGE : message;
            errorAttributes.put(MESSAGE, censoredMessage);
        }

        return errorAttributes;
    }

    private boolean containsTechnicalInfo(String message) {
        return VALIDATION_REGEX.matcher(message).find();
    }

    private static Pattern compileValidationRegex(){
        String pipedBlacklist = Arrays.stream(TECHNICAL_INFO_BLACKLIST)
            .map(s -> s.replaceAll("\\.",""))
            .collect(Collectors.joining("|"));

        String regex = "\\W(".concat(pipedBlacklist).concat(")\\.\\w");

        //RETURNS \W(java|javax|jakarta|java_cup|org|com|net|io|dev|de)\.\w

        return Pattern.compile(regex);
    }

}
