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

import '../load-env';
import { cleanupLog, cleanupPhase } from '../../helpers/acceptance-progress';
import { printNamespaceCleanupHint } from './teardown/namespace-cleanup-hint';
import { cleanupPortForwarding } from './teardown/port-forward';

async function globalTeardown(): Promise<void> {
    cleanupPhase('coordinator', 'Playwright global teardown');

    if (process.env.CI === 'true' || process.env.GITHUB_ACTIONS === 'true') {
        cleanupLog('coordinator', 'CI run — skipping port-forward cleanup (direct gateway)');
        cleanupLog('coordinator', '✓ Global teardown completed');
        return;
    }

    await cleanupPortForwarding();
    printNamespaceCleanupHint();
    cleanupLog('coordinator', '✓ Global teardown completed');
}

export default globalTeardown;
