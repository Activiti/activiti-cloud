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

package org.activiti.cloud.service.common.batch.core.jobexecution;

import static java.util.stream.Collectors.toList;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import org.activiti.cloud.service.common.batch.domain.JobExecution;
import org.activiti.cloud.service.common.batch.util.DateUtil;

public class JobExecutionConverter {
    public static JobExecution fromSpring(org.springframework.batch.core.JobExecution jobExecution) {
        return JobExecution.builder()
                           .jobId(jobExecution.getJobId())
                           .id(jobExecution.getId())
                           .jobName(jobExecution.getJobInstance().getJobName())
                           .startTime(DateUtil.localDateTime(jobExecution.getStartTime()))
                           .endTime(DateUtil.localDateTime(jobExecution.getEndTime()))
                           .exitCode(jobExecution.getExitStatus() == null ? null : jobExecution.getExitStatus().getExitCode())
                           .exitDescription(jobExecution.getExitStatus() == null ? null : jobExecution.getExitStatus().getExitDescription())
                           .status(jobExecution.getStatus())
                           .exceptions(mapExceptions(jobExecution.getFailureExceptions()))
                           .build();
    }

    public static List<String> mapExceptions(List<Throwable> exceptions) {
        return exceptions.stream()
                         .map(e -> e.getMessage() + ": " + JobExecutionConverter.getStackTraceAsString(e))
                         .collect(toList());
    }

    public static String getStackTraceAsString(Throwable throwable) {
        StringWriter stackTrace = new StringWriter();
        throwable.printStackTrace(new PrintWriter(stackTrace));
        return stackTrace.toString();
    }
}
