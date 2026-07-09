import { request } from '@playwright/test';
import { exec, spawn } from 'child_process';
import { writeFileSync } from 'fs';
import { promisify } from 'util';
import {
    acceptanceLog,
    acceptancePhase,
    acceptanceStep,
    acceptanceVibe,
} from '../../../helpers/acceptance-progress';
import {
    getPortForwardHelpCommand,
    getPortForwardKubectlArgs,
    getPortForwardTarget,
} from '../../connection/port-forward-target';
import { getTestConfiguration } from '../../runtime/test-configuration';
import { timeouts } from '../../runtime/timeouts';
import { paths } from '../../paths';

const execAsync = promisify(exec);
const LOCAL_LOOPBACK = '127.0.0.1';

export async function checkKubectlAvailable(): Promise<void> {
    try {
        const { stdout } = await execAsync('kubectl version --client');
        const version = stdout.split('\n')[0].trim();
        acceptanceLog('discovery', `✓ kubectl: ${version}`);
    } catch (error) {
        throw new Error(`kubectl is not available or not in PATH. Install kubectl first.\nError: ${error}`);
    }
}

async function checkPortForwardingActive(localPort: string, throwOnError: boolean): Promise<boolean> {
    try {
        const { stdout } = await execAsync(
            `ps aux | grep "kubectl port-forward" | grep "${localPort}:80" | grep -v grep`
        );
        if (stdout.trim()) {
            acceptanceLog('traefik', '✓ kubectl port-forward process detected');
        } else {
            acceptanceLog('traefik', `No port-forward process in ps — probing ${LOCAL_LOOPBACK} anyway`);
        }
    } catch {
        acceptanceLog('traefik', `Could not detect port-forward process — probing ${LOCAL_LOOPBACK} anyway`);
    }

    try {
        const context = await request.newContext();
        const response = await context.get(`http://${LOCAL_LOOPBACK}:${localPort}`, {
            timeout: timeouts.http.localPortProbe,
            ignoreHTTPSErrors: true,
        });
        acceptanceLog('traefik', `✓ ${LOCAL_LOOPBACK}:${localPort} reachable (HTTP ${response.status()})`);
        await context.dispose();
        return true;
    } catch (error) {
        const pfCmd = getPortForwardHelpCommand(localPort);
        const errorMessage = `Local port ${localPort} is not accessible.

Manual fallback:
   ${pfCmd}

Error: ${error}`;

        if (throwOnError) {
            throw new Error(errorMessage);
        }
        acceptanceLog('traefik', `${LOCAL_LOOPBACK} not reachable yet — will start port-forward automatically`);
        return false;
    }
}

async function startPortForwarding(localPort: string): Promise<void> {
    try {
        acceptanceStep('traefik', 'Clearing stale kubectl port-forward on :' + localPort);
        try {
            await execAsync(`pkill -f "kubectl port-forward.*${localPort}:80"`);
            await new Promise((resolve) => setTimeout(resolve, 1000));
        } catch {
            acceptanceLog('traefik', 'No stale port-forward found — clean slate');
        }

        const target = getPortForwardTarget();
        acceptanceStep('traefik', `kubectl port-forward — ${target.label}`);

        const portForwardProcess = spawn('kubectl', getPortForwardKubectlArgs(localPort), {
            detached: true,
            stdio: ['ignore', 'pipe', 'pipe'],
        });

        if (!portForwardProcess.pid) {
            throw new Error('Failed to get process PID for port-forwarding');
        }

        writeFileSync(paths.portForwardPidFile, portForwardProcess.pid.toString());
        acceptanceLog('traefik', `✓ Port-forward PID ${portForwardProcess.pid} (saved to port-forward.pid)`);
        portForwardProcess.unref();

        await new Promise<void>((resolve, reject) => {
            let output = '';
            let errorOutput = '';

            const timeout = setTimeout(() => {
                reject(new Error('Port-forwarding setup timed out'));
            }, timeouts.http.portForwardReady);

            portForwardProcess.stdout?.on('data', (data: Buffer) => {
                output += data.toString();
                if (output.includes('Forwarding from')) {
                    clearTimeout(timeout);
                    resolve();
                }
            });

            portForwardProcess.stderr?.on('data', (data: Buffer) => {
                errorOutput += data.toString();
            });

            portForwardProcess.on('error', (error: Error) => {
                clearTimeout(timeout);
                reject(new Error(`Failed to start port-forwarding: ${error.message}`));
            });

            portForwardProcess.on('exit', (code) => {
                if (code !== 0 && code !== null) {
                    clearTimeout(timeout);
                    reject(new Error(`Port-forwarding exited with code ${code}: ${errorOutput}`));
                }
            });
        });
    } catch (error) {
        throw new Error(`Failed to start port-forwarding automatically:

   ${getPortForwardHelpCommand(localPort)}

Error: ${error}`);
    }
}

async function checkGatewayConnectivity(localPort: string, expectedGatewayHost: string): Promise<void> {
    try {
        const hostWithoutPort = expectedGatewayHost.replace(/:\d+$/, '');
        const context = await request.newContext();
        const response = await context.get(`http://${LOCAL_LOOPBACK}:${localPort}`, {
            timeout: timeouts.http.healthCheck,
            ignoreHTTPSErrors: true,
            headers: { Host: hostWithoutPort },
        });

        acceptanceLog(
            'traefik',
            `✓ Gateway reachable via tunnel (HTTP ${response.status()}) — ${hostWithoutPort} → ${LOCAL_LOOPBACK}:${localPort}`
        );
        await context.dispose();
    } catch (error) {
        const pfCmd = getPortForwardHelpCommand(localPort);
        throw new Error(`Cannot reach gateway through port-forward.

   ${pfCmd}
   kubectl get pods -n ${process.env.PREVIEW_NAME || '<PREVIEW_NAME>'}
   curl -H "Host: ${expectedGatewayHost.replace(/:\d+$/, '')}" http://${LOCAL_LOOPBACK}:${localPort}/rb/actuator/health

Error: ${error}`);
    }
}

export async function setupPortForwarding(): Promise<void> {
    const testConfig = getTestConfiguration();
    const localPort = testConfig.localPort || '8080';
    const expectedGatewayHost = process.env.GATEWAY_HOST;

    if (!expectedGatewayHost) {
        throw new Error('GATEWAY_HOST environment variable is not set');
    }

    const hostWithoutPort = expectedGatewayHost.replace(/:\d+$/, '');
    acceptancePhase('traefik', 'Local gateway tunnel');
    acceptanceStep(
        'traefik',
        `API traffic: http://${LOCAL_LOOPBACK}:${localPort} + Host: ${hostWithoutPort} (auto port-forward — no second terminal)`
    );

    await checkKubectlAvailable();

    const isPortForwardActive = await checkPortForwardingActive(localPort, false);
    if (!isPortForwardActive) {
        acceptanceStep('traefik', 'Port-forward not active — starting kubectl in background');
        acceptanceVibe('traefik');
        await startPortForwarding(localPort);
        await new Promise((resolve) => setTimeout(resolve, 3000));
        await checkPortForwardingActive(localPort, true);
    }

    await checkGatewayConnectivity(localPort, expectedGatewayHost);
}
