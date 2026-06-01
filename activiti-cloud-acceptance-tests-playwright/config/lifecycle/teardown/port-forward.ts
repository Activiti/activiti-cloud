import { exec } from 'child_process';
import { existsSync, readFileSync, unlinkSync } from 'fs';
import { promisify } from 'util';
import { cleanupLog, cleanupPhase, cleanupStep, cleanupVibe } from '../../../helpers/acceptance-progress';
import { paths } from '../../paths';

const execAsync = promisify(exec);

function isProcessRunning(pid: number): boolean {
    try {
        process.kill(pid, 0);
        return true;
    } catch {
        return false;
    }
}

async function terminateProcess(pid: number): Promise<void> {
    try {
        process.kill(pid, 'SIGTERM');
        cleanupLog('traefik', `✓ Process ${pid} received SIGTERM`);

        await new Promise((resolve) => setTimeout(resolve, 1000));

        if (isProcessRunning(pid)) {
            cleanupStep('traefik', `PID ${pid} still up — escalating to SIGKILL`);
            process.kill(pid, 'SIGKILL');
            cleanupLog('traefik', `✓ Process ${pid} force-stopped`);
        }
    } catch (error) {
        const detail = error instanceof Error ? error.message : String(error);
        cleanupLog('traefik', `⚠ Could not terminate ${pid}: ${detail}`);
    }
}

async function cleanupSpecificProcess(): Promise<void> {
    const pidFile = paths.portForwardPidFile;
    if (!existsSync(pidFile)) {
        cleanupStep('traefik', 'No PID file — nothing we started is registered');
        return;
    }

    try {
        const pidString = readFileSync(pidFile, 'utf-8').trim();
        const pid = parseInt(pidString, 10);

        if (!isNaN(pid)) {
            cleanupStep('traefik', `SIGTERM port-forward PID ${pid}`);
            cleanupVibe('traefik');
            await terminateProcess(pid);
        }

        unlinkSync(pidFile);
        cleanupLog('traefik', '✓ Removed port-forward.pid');
    } catch (error) {
        const detail = error instanceof Error ? error.message : String(error);
        cleanupLog('traefik', `⚠ Could not process PID file: ${detail}`);
    }
}

async function cleanupRemainingProcesses(localPort: string): Promise<void> {
    cleanupStep('traefik', `pkill remaining kubectl port-forward on :${localPort}`);
    try {
        await execAsync(`pkill -f "kubectl port-forward.*${localPort}:80"`);
        cleanupLog('traefik', '✓ Stray port-forward processes cleared');
    } catch {
        cleanupLog('traefik', 'No extra port-forward processes found — already tidy');
    }
}

export async function cleanupPortForwarding(): Promise<void> {
    cleanupPhase('traefik', 'Port-forward cleanup');
    const localPort = process.env.LOCAL_PORT || '8080';

    try {
        await cleanupSpecificProcess();
        await cleanupRemainingProcesses(localPort);
        cleanupLog('traefik', '✓ Port-forward tunnel closed — localhost:8080 is free again');
    } catch (error) {
        const detail = error instanceof Error ? error.message : String(error);
        cleanupLog('traefik', `⚠ Teardown hiccup: ${detail}`);
        cleanupLog('traefik', 'Usually harmless — kubectl may have already left the building');
    }
}
