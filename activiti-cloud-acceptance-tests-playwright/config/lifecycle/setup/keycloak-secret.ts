import { exec } from 'child_process';
import { existsSync } from 'fs';
import * as path from 'path';
import { promisify } from 'util';
import { acceptanceLog } from '../../../helpers/acceptance-progress';

const execAsync = promisify(exec);

function kubectlEnv(): NodeJS.ProcessEnv {
    const env = { ...process.env };
    if (env.KUBECONFIG?.trim()) {
        return env;
    }

    const candidates = [
        env.ACTIVITI_KUBECONFIG?.trim(),
        path.join(env.HOME || '', '.kube', 'config'),
        path.join(env.HOME || '', 'Downloads', 'activiti.yaml'),
        path.join(env.HOME || '', 'Downloads', 'develop.yaml'),
    ].filter((value): value is string => Boolean(value));

    for (const kubeconfigPath of candidates) {
        if (existsSync(kubeconfigPath)) {
            env.KUBECONFIG = kubeconfigPath;
            break;
        }
    }

    return env;
}

/**
 * Load activiti client secret from the preview namespace (Helm chart secret).
 * Local runs use npm run test:setup; CI sets KEYCLOAK_CLIENT_SECRET via load-preview-keycloak-secret.
 */
export async function ensureKeycloakClientSecretFromCluster(): Promise<void> {
    const isCi = process.env.CI === 'true' || process.env.GITHUB_ACTIONS === 'true';
    // CI: always refresh from cluster (Helm uuid per install; workflow env can be stale).
    if (!isCi && process.env.KEYCLOAK_CLIENT_SECRET?.trim()) {
        return;
    }

    const namespace = process.env.PREVIEW_NAME?.trim();
    if (!namespace) {
        return;
    }

    if (process.env.CLUSTER_PROFILE === 'develop' || process.env.CLUSTER_NAME === 'develop') {
        return;
    }

    const cmd =
        `kubectl get secret activiti-keycloak-client -n "${namespace}" ` +
        `-o jsonpath='{.data.clientSecret}' 2>/dev/null | base64 -d`;

    try {
        const { stdout } = await execAsync(cmd, {
            env: kubectlEnv(),
            shell: '/bin/bash',
        });
        const secret = stdout.trim();
        if (secret) {
            process.env.KEYCLOAK_CLIENT_SECRET = secret;
            acceptanceLog('discovery', `✓ KEYCLOAK_CLIENT_SECRET loaded from ${namespace}/activiti-keycloak-client`);
            return;
        }
    } catch (error) {
        const message = error instanceof Error ? error.message : String(error);
        if (process.env.CI === 'true' || process.env.GITHUB_ACTIONS === 'true') {
            acceptanceLog('discovery', `⚠ Could not load KEYCLOAK_CLIENT_SECRET from cluster: ${message}`);
        }
    }
}
