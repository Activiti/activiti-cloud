import { acceptanceLog, acceptancePhase, acceptanceStep } from '../../../helpers/acceptance-progress';
import { runCommandInherit } from './run-shell';

export async function applyClusterPrereqsIfNeeded(): Promise<void> {
    if (process.env.ACCEPTANCE_CI_OVERLAY_APPLIED === 'true') {
        acceptanceLog('policies', '✓ Cluster overlay already applied in CI workflow — skipping');
        return;
    }

    const enabled = process.env.AUTO_CLUSTER_PREREQS?.trim().toLowerCase();
    if (enabled === 'false') {
        return;
    }

    // CI: kubectl + Helm overlay run in prepare-preview-for-playwright (workflow), not from Node.
    if (process.env.CI === 'true' || process.env.GITHUB_ACTIONS === 'true') {
        acceptanceLog(
            'policies',
            'CI mode — cluster overlay expected from workflow (set AUTO_CLUSTER_PREREQS=false or run prepare-preview-for-playwright)'
        );
        return;
    }

    const ns = process.env.PREVIEW_NAME?.trim();
    if (!ns) {
        return;
    }

    acceptancePhase('policies', `Cluster prerequisites — namespace ${ns}`);
    acceptanceStep('policies', 'Handing off to apply-cluster-prereqs.sh (live output below)');
    await runCommandInherit('bash', [
        'activiti-cloud-acceptance-tests-playwright/scripts/apply-cluster-prereqs.sh',
        ns,
    ]);
    acceptanceLog('policies', '✓ Cluster prerequisites applied');
}
