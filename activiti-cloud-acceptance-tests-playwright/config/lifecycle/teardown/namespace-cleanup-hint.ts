import { cleanupLog } from '../../../helpers/acceptance-progress';

/**
 * Remind developers to tear down the preview namespace after local runs (CI deletes in workflow).
 */
export function printNamespaceCleanupHint(): void {
    if (process.env.CI === 'true' || process.env.GITHUB_ACTIONS === 'true') {
        return;
    }

    const previewName = process.env.PREVIEW_NAME?.trim();
    if (!previewName) {
        return;
    }

    cleanupLog('coordinator', '');
    cleanupLog('coordinator', '────────────────────────────────────────────────────────');
    cleanupLog(
        'coordinator',
        `Preview namespace still on cluster: ${previewName}`
    );
    cleanupLog('coordinator', 'Delete it when you are finished (from repository root):');
    cleanupLog('coordinator', `  PREVIEW_NAME=${previewName} make delete`);
    cleanupLog('coordinator', 'Or:');
    cleanupLog('coordinator', `  npm run preview:delete`);
    cleanupLog('coordinator', `  kubectl delete ns ${previewName}`);
    cleanupLog('coordinator', '────────────────────────────────────────────────────────');
}
