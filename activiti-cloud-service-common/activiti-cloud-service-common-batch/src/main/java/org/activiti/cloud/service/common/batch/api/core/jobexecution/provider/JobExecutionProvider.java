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

package org.activiti.cloud.service.common.batch.api.core.jobexecution.provider;

import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;

import org.springframework.batch.core.JobExecution;

public interface JobExecutionProvider {

    Collection<JobExecution> getJobExecutions(Optional<String> jobNameRegexp,
                                              Optional<String> exitCode,
                                              int limitPerJob);

    default Comparator<JobExecution> byDescendingTime() {
        return (j1, j2) -> {
            int result;
            if (j1.getEndTime() != null && j2.getEndTime() != null)
                result = j1.getEndTime().compareTo(j2.getEndTime());
            else
                result = j1.getStartTime().compareTo(j2.getStartTime());
            return result * -1;
        };
    }
}
