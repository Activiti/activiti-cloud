// Generated by delombok at Wed Jan 13 19:00:08 PST 2021
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
package org.activiti.cloud.service.common.batch.domain;

import java.beans.ConstructorProperties;

import org.springframework.hateoas.RepresentationModel;

public final class Job extends RepresentationModel<Job> {
    private final String name;

    @ConstructorProperties({"name"})
    public Job(final String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    @Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof Job)) return false;
        final Job other = (Job) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$name = this.getName();
        final java.lang.Object other$name = other.getName();
        if (this$name == null ? other$name != null : !this$name.equals(other$name)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof Job;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $name = this.getName();
        result = result * PRIME + ($name == null ? 43 : $name.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return "Job(name=" + this.getName() + ")";
    }

}
