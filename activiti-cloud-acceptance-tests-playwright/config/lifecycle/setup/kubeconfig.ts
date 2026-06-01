import { existsSync } from 'fs';
import * as path from 'path';
import { previewNameFromEnvName } from '../../connection/preview-name';
import { acceptanceLog } from '../../../helpers/acceptance-progress';

export async function ensureKubeconfig(): Promise<void> {
    if (process.env.KUBECONFIG?.trim()) {
        acceptanceLog('discovery', '✓ KUBECONFIG already set');
        return;
    }

    const candidates = [
        process.env.ACTIVITI_KUBECONFIG?.trim(),
        path.join(process.env.HOME || '', '.kube', 'config'),
        path.join(process.env.HOME || '', 'Downloads', 'activiti.yaml'),
        path.join(process.env.HOME || '', 'Downloads', 'develop.yaml'),
    ].filter((value): value is string => Boolean(value));

    for (const kubeconfigPath of candidates) {
        if (existsSync(kubeconfigPath)) {
            process.env.KUBECONFIG = kubeconfigPath;
            acceptanceLog('discovery', `✓ Using kubeconfig: ${kubeconfigPath}`);
            return;
        }
    }
}

export async function resolvePreviewNamespace(): Promise<void> {
    if (process.env.PREVIEW_NAME?.trim()) {
        return;
    }

    const envName = process.env.ACCEPTANCE_ENV_NAME?.trim();
    if (envName) {
        process.env.PREVIEW_NAME = previewNameFromEnvName(envName);
        acceptanceLog('discovery', `✓ PREVIEW_NAME from ACCEPTANCE_ENV_NAME: ${process.env.PREVIEW_NAME}`);
        return;
    }

    acceptanceLog(
        'discovery',
        '⚠ PREVIEW_NAME not set — run npm run test:setup -- --install (writes .env with your env name)'
    );
}
