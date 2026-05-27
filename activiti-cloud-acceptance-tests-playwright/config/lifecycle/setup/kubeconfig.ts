import { exec } from 'child_process';
import { existsSync } from 'fs';
import * as path from 'path';
import { promisify } from 'util';
import { acceptanceLog } from '../../../helpers/acceptance-progress';

const execAsync = promisify(exec);

export async function ensureKubeconfig(): Promise<void> {
    if (process.env.KUBECONFIG?.trim()) {
        acceptanceLog('discovery', '✓ KUBECONFIG already set');
        return;
    }

    const candidates = [
        process.env.ACTIVITI_KUBECONFIG?.trim(),
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

    try {
        const { stdout } = await execAsync('kubectl get ns -o json');
        const ns = JSON.parse(stdout) as {
            items: Array<{ metadata: { name: string; creationTimestamp?: string } }>;
        };
        const candidates = ns.items
            .map((i) => i.metadata)
            .filter((m) => /^pr-.*-rabbit-n-d$/.test(m.name))
            .sort((a, b) => (b.creationTimestamp || '').localeCompare(a.creationTimestamp || ''));

        for (const c of candidates) {
            const name = c.name;
            const check = await execAsync(
                `kubectl get deployment "${name}-runtime-bundle" -n "${name}" --ignore-not-found -o name`
            );
            if (check.stdout.trim()) {
                process.env.PREVIEW_NAME = name;
                acceptanceLog('discovery', `✓ Auto-detected PREVIEW_NAME: ${name}`);
                return;
            }
        }
    } catch {
        // fall back to env validation errors
    }
}
