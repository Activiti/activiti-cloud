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
package org.activiti.cloud.api.process.model;

import java.util.List;

public class DetailedIntegrationError {

  private final String error;
  private final List<StackTraceElement> stackTraceElementList;

  public DetailedIntegrationError(String error, List<StackTraceElement> stackTraceElementList) {
    this.error = error;
    this.stackTraceElementList = stackTraceElementList;
  }

  public String getError() {
    return error;
  }

  public List<StackTraceElement> getStackTraceElementList() {
    return stackTraceElementList;
  }

  @Override
  public String toString() {
    return "IntegrationError{" +
        "error='" + error + '\'' +
        ", stackTraceElementList=" + stackTraceElementList +
        '}';
  }
}
