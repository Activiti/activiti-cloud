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
package gatling.utils;

public final class SystemPropertiesUtil {

    public static String getAsStringOrElse(String key, String fallback) {
        String value = System.getProperty(key);
        if (value == null) {
            return fallback;
        }
        return value;
    }

    public static double getAsDoubleOrElse(String key, double fallback) {
        String value = System.getProperty(key);
        if (value == null) {
            return fallback;
        }
        return Double.parseDouble(value);
    }

    public static int getAsIntOrElse(String key, int fallback) {
        String value = System.getProperty(key);
        if (value == null) {
            return fallback;
        }
        return Integer.parseInt(value);
    }

    public static long getAsLongOrElse(String key, long fallback) {
        String value = System.getProperty(key);
        if (value == null) {
            return fallback;
        }
        return Long.parseLong(value);
    }

    public static boolean getAsBooleanOrElse(String key, boolean fallback) {
        String value = System.getProperty(key);
        if (value == null) {
            return fallback;
        }
        return Boolean.parseBoolean(value);
    }
}
