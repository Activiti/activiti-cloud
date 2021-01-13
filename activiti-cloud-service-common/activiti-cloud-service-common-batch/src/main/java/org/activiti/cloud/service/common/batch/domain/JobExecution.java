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

import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collection;

import org.activiti.cloud.service.common.batch.util.DateUtil;
import org.springframework.batch.core.BatchStatus;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.base.Throwables;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class JobExecution implements Comparable<JobExecution> {

    private static final String EXIT_CODE = "exitCode";
    private static final String EXIT_DESCRIPTION = "exitDescription";

    public static JobExecution fromSpring(org.springframework.batch.core.JobExecution je) {
        return JobExecution.builder()
                           .jobId(je.getJobId())
                           .id(je.getId())
                           .jobName(je.getJobInstance().getJobName())
                           .startTime(DateUtil.localDateTime(je.getStartTime()))
                           .endTime(DateUtil.localDateTime(je.getEndTime()))
                           .exitCode(je.getExitStatus() == null ? null : je.getExitStatus().getExitCode())
                           .exitDescription(je.getExitStatus() == null ? null : je.getExitStatus().getExitDescription())
                           .status(je.getStatus())
                           .exceptions(je.getFailureExceptions()
                                         .stream()
                                         .map(e -> e.getMessage() + ": " + Throwables.getStackTraceAsString(e))
                                         .collect(toList()))
                           .build();
    }

    private long id;
    private long jobId;
    private String jobName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String exitCode;
    private String exitDescription;
    @JsonSerialize(using = BatchStatusSerializer.class)
    private BatchStatus status;

    private Collection<String> exceptions;

    @Override
    public int compareTo(JobExecution o) {
        int result = this.getJobName() != null ? this.getJobName().compareToIgnoreCase(o.getJobName()) : 0;
        if (result == 0)
            result = Long.compare(id, o.id);
        if (result == 0)
            result = Long.compare(jobId, o.jobId);
        return result;
    }

    static class BatchStatusSerializer extends JsonSerializer<BatchStatus> {

        @Override
        public void serialize(BatchStatus batchStatus, JsonGenerator jsonGen, SerializerProvider serializerProvider)
                                                                                                                     throws IOException {
            jsonGen.writeString(batchStatus.name());
        }
    }
}
