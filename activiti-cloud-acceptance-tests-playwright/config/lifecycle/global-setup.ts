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

import { bootstrapAcceptanceEnv } from '../bootstrap';
import { validateEnvironmentVariables } from '../validation/environment-validator';
import { getTestConfiguration } from '../runtime/test-configuration';
import { acceptanceLog, acceptancePhase, acceptanceStep } from '../../helpers/acceptance-progress';
import { applyClusterPrereqsIfNeeded } from './setup/cluster-prereqs';
import { ensureKeycloakClientSecretFromCluster } from './setup/keycloak-secret';
import { ensureKubeconfig, resolvePreviewNamespace } from './setup/kubeconfig';
import { verifyProcessCatalogIfEnabled } from './setup/process-catalog';
import { setupPortForwarding } from './setup/port-forward';

async function globalSetup(): Promise<void> {
    acceptancePhase('discovery', 'Playwright global setup');

    await ensureKubeconfig();
    await resolvePreviewNamespace();
    await ensureKeycloakClientSecretFromCluster();

    bootstrapAcceptanceEnv();
    acceptanceStep('traefik', `Gateway host: ${process.env.GATEWAY_HOST ?? '(unset)'}`);

    const envCheck = validateEnvironmentVariables('all');
    for (const warning of envCheck.warnings) {
        acceptanceLog('discovery', `⚠ ${warning}`);
    }
    if (!envCheck.ok) {
        for (const err of envCheck.errors) {
            acceptanceLog('discovery', `✗ ${err}`);
        }
        throw new Error(
            `Environment configuration invalid:\n${envCheck.errors.map((e) => `  • ${e}`).join('\n')}\n` +
                'Fix activiti-cloud-acceptance-tests-playwright/.env — see .env.example and README.md'
        );
    }
    acceptanceLog('discovery', '✓ Environment variables validated');

    const testConfig = getTestConfiguration();

    await applyClusterPrereqsIfNeeded();

    if (testConfig.isCI) {
        acceptancePhase('discovery', 'CI mode');
        acceptanceLog('traefik', 'Direct HTTPS gateway — skipping port-forward');
        await verifyProcessCatalogIfEnabled();
        acceptanceLog('discovery', '✓ All preconditions passed — tests can proceed');
        return;
    }

    await setupPortForwarding();
    await verifyProcessCatalogIfEnabled();
    acceptanceLog('discovery', '✓ All preconditions passed — tests can proceed');
}

export default globalSetup;
