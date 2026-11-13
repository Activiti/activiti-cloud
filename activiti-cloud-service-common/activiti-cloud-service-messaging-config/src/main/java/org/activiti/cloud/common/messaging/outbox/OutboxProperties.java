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
package org.activiti.cloud.common.messaging.outbox;

import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.util.Arrays;
import java.util.regex.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("activiti.cloud.messaging.outbox")
public class OutboxProperties {

    private boolean enabled = false;

    @NotNull
    private Pattern[] excludes = { Pattern.compile("^$") };

    private Boolean autoStartup; // null by default

    @NotNull
    private Duration reconcilePeriod = Duration.ofSeconds(1);

    @NotNull
    private Duration pollerActivePeriod = Duration.ofMillis(1);

    @NotNull
    private Duration pollerIdlePeriod = Duration.ofMillis(50);

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Pattern[] getExcludes() {
        return excludes;
    }

    public void setExcludes(Pattern[] excludes) {
        this.excludes = excludes;
    }

    public Boolean getAutoStartup() {
        return autoStartup;
    }

    public void setAutoStartup(Boolean autoStartup) {
        this.autoStartup = autoStartup;
    }

    public Duration getReconcilePeriod() {
        return reconcilePeriod;
    }

    public void setReconcilePeriod(Duration reconcilePeriod) {
        this.reconcilePeriod = reconcilePeriod;
    }

    public Duration getPollerActivePeriod() {
        return pollerActivePeriod;
    }

    public void setPollerActivePeriod(Duration pollerActivePeriod) {
        this.pollerActivePeriod = pollerActivePeriod;
    }

    public Duration getPollerIdlePeriod() {
        return pollerIdlePeriod;
    }

    public void setPollerIdlePeriod(Duration pollerIdlePeriod) {
        this.pollerIdlePeriod = pollerIdlePeriod;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("OutboxProperties{");
        sb.append("enabled=").append(enabled);
        sb.append(", excludes=").append(excludes == null ? "null" : Arrays.asList(excludes).toString());
        sb.append(", autoStartup=").append(autoStartup);
        sb.append(", reconcilePeriod=").append(reconcilePeriod);
        sb.append(", pollerActivePeriod=").append(pollerActivePeriod);
        sb.append(", pollerIdlePeriod=").append(pollerIdlePeriod);
        sb.append('}');
        return sb.toString();
    }
}
