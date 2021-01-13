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

import static java.util.stream.Collectors.toList;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.explore.JobExplorer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class DefaultJobExecutionProvider implements JobExecutionProvider {

    private final JobExplorer jobExplorer;

    @Override
    public Collection<JobExecution> getJobExecutions(Optional<String> jobNameRegexp,
                                                     Optional<String> exitCode,
                                                     int limitPerJob) {
        log.debug("Getting job executions from JobExplorer for jobNameRegexp={}, exitCode={}, limitPerJob={}",
                  jobNameRegexp,
                  exitCode,
                  limitPerJob);

        Optional<Pattern> maybeJobNamePattern = jobNameRegexp.map(Pattern::compile);

        List<String> jobNames = jobExplorer.getJobNames()
                                           .stream()
                                           .filter(n -> maybeJobNamePattern.map(p -> p.matcher(n).matches())
                                                                           .orElse(true))
                                           .collect(toList());

        TreeSet<JobExecution> result = new TreeSet<>(byDescendingTime());

        for (String jobName : jobNames)
            jobExplorer.getJobInstances(jobName, 0, limitPerJob)
                       .stream()
                       .flatMap(ji -> jobExplorer.getJobExecutions(ji).stream())
                       .filter(e -> exitCode.map(c -> e.getExitStatus().getExitCode().equals(c)).orElse(true))
                       .sorted(byDescendingTime())
                       .limit(limitPerJob)
                       .forEach(result::add);

        log.debug("Found {} job execution(s) for jobNameRegexp={}, exitCode={}, limitPerJob={}",
                  jobNameRegexp,
                  exitCode,
                  limitPerJob,
                  result.size());
        return result;
    }
}
