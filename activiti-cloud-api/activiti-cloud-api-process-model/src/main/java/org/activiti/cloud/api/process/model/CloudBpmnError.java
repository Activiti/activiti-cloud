/*
 * Copyright 2017-2020 Alfresco.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.activiti.cloud.api.process.model;

public class CloudBpmnError extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public CloudBpmnError(String errorCode) {
      super(errorCode);

      if (errorCode == null) {
          throw new IllegalArgumentException("Error Code must not be null.");
        }
        if (errorCode.length() < 1) {
          throw new IllegalArgumentException("Error Code must not be empty.");
        }

    }

    public String getErrorCode() {
      return getMessage();
    }

}
