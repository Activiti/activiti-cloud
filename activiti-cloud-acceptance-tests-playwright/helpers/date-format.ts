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

export function formatDefaultUtc(date: Date): string {
    const pad = (n: number, w = 2) => String(n).padStart(w, '0');
    const y = date.getUTCFullYear();
    const M = pad(date.getUTCMonth() + 1);
    const d = pad(date.getUTCDate());
    const h = pad(date.getUTCHours());
    const m = pad(date.getUTCMinutes());
    const s = pad(date.getUTCSeconds());
    const ms = pad(date.getUTCMilliseconds(), 3);
    return `${y}-${M}-${d}T${h}:${m}:${s}.${ms}+0000`;
}
