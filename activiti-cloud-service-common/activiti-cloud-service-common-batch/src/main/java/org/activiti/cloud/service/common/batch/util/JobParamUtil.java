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

package org.activiti.cloud.service.common.batch.util;

import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toMap;

import java.util.Date;
import java.util.Map;
import java.util.Optional;

import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;

public class JobParamUtil {

	public static JobParameters convertRawToJobParams(Map<String, Object> properties) {
		return new JobParameters(convertRawToParamMap(properties));
	}

	public static Map<String, JobParameter> convertRawToParamMap(Map<String, Object> properties) {
		return Optional.ofNullable(properties).orElse(emptyMap()).entrySet().stream()
				.collect(toMap(Map.Entry::getKey, e -> createJobParameter(e.getValue())));
	}

	public static JobParameter createJobParameter(Object value) {
		if (value instanceof Date)
			return new JobParameter((Date) value);
		else if (value instanceof Long)
			return new JobParameter((Long) value);
		else if (value instanceof Double)
			return new JobParameter((Double) value);
		else
			return new JobParameter("" + value);
	}
}
