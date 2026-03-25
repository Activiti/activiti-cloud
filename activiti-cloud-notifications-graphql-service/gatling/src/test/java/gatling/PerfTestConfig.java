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
package gatling;

import static gatling.utils.SystemPropertiesUtil.getAsDoubleOrElse;
import static gatling.utils.SystemPropertiesUtil.getAsIntOrElse;
import static gatling.utils.SystemPropertiesUtil.getAsStringOrElse;

import java.time.Duration;

public final class PerfTestConfig {

    public static final String BASE_URL = getAsStringOrElse("baseUrl", "http://localhost:8080");
    public static final double REQUEST_PER_SECOND = getAsDoubleOrElse("requestPerSecond", 10f);
    public static final Duration DURATION_SEC = Duration.ofSeconds(getAsIntOrElse("durationMin", 30));
    public static final int P95_RESPONSE_TIME_MS = getAsIntOrElse("p95ResponseTimeMs", 1000);
}
