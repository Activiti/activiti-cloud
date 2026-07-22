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

import { CloudProcessDefinition } from '../../../models/process-definition.models';

export function pickHighestVersionByKey(
    definitions: CloudProcessDefinition[],
    key: string
): CloudProcessDefinition {
    const matches = definitions.filter((def) => def.key === key);
    if (matches.length === 0) {
        throw new Error(`No process definition found matching key ${key}`);
    }
    return matches.reduce((best, current) => {
        const bestVersion = parseInt(String(best.appVersion ?? '0'), 10);
        const currentVersion = parseInt(String(current.appVersion ?? '0'), 10);
        return currentVersion > bestVersion ? current : best;
    });
}
