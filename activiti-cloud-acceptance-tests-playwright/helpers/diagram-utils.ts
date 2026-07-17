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

export function isDiagramShown(diagram: string): boolean {
    const trimmed = diagram?.trim() ?? '';
    return trimmed.length > 0 && /<svg/i.test(trimmed);
}

export function isDiagramEmpty(diagram: string): boolean {
    return !diagram?.trim();
}

export function normalizeSvg(svg: string): string {
    return svg
        .replace(/<path\b[^>]*\/>/g, '')
        .replace(/<path\b[^>]*>[\s\S]*?<\/path>/g, '')
        .replace(/\sstyle="[^"]*"/g, '')
        .replace(/>\s+</g, '><')
        .replace(/\s+/g, ' ')
        .trim();
}
