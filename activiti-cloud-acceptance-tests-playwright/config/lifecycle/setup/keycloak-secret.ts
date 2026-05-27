import { exec } from 'child_process';
import { promisify } from 'util';
import { acceptanceLog } from '../../../helpers/acceptance-progress';

const execAsync = promisify(exec);

/**
 * Load activiti client secret from the preview namespace (Helm chart secret).
 * Local runs use npm run test:setup; CI passes PREVIEW_NAME but not the secret.
 */
export async function ensureKeycloakClientSecretFromCluster(): Promise<void> {
    if (process.env.KEYCLOAK_CLIENT_SECRET?.trim()) {
        return;
    }

    const namespace = process.env.PREVIEW_NAME?.trim();
    if (!namespace) {
        return;
    }

    if (process.env.CLUSTER_PROFILE === 'develop' || process.env.CLUSTER_NAME === 'develop') {
        return;
    }

    try {
        const { stdout } = await execAsync(
            `kubectl get secret activiti-keycloak-client -n "${namespace}" -o jsonpath='{.data.clientSecret}'`
        );
        const encoded = stdout.trim();
        if (!encoded) {
            return;
        }

        const secret = Buffer.from(encoded, 'base64').toString('utf8').trim();
        if (secret) {
            process.env.KEYCLOAK_CLIENT_SECRET = secret;
            acceptanceLog('discovery', `✓ KEYCLOAK_CLIENT_SECRET loaded from ${namespace}/activiti-keycloak-client`);
        }
    } catch {
        // validateEnvironmentVariables reports a clear error if still missing
    }
}
