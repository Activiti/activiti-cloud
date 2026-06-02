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

/** Start variables aligned with Serenity ProcessRuntimeBundleSteps.startProcess(..., variables=true). */
export function buildConnectorStartVariables(): Record<string, unknown> {
    return {
        test_variable_name: 'test-variable-value',
        test_bigdecimal_variable_name: { type: 'bigdecimal', value: '12345678.90' },
        test_date_variable_name: { type: 'date', value: '1970-01-01T00:00:00.000Z' },
        test_long_variable_name: { type: 'long', value: '1234567890' },
        test_int_variable_name: 7,
        test_bool_variable_name: true,
        test_json_variable_name: { 'test-json-variable-element1': 'test-json-variable-value1' },
        test_long_json_variable_name: { verylongjson: 'a'.repeat(4000) },
    };
}
