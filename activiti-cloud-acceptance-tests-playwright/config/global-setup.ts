/*
 * Copyright 2017-2020 Alfresco Software, Ltd.
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

import { request } from '@playwright/test';
import { exec, spawn } from 'child_process';
import { promisify } from 'util';
import { writeFileSync } from 'fs';
import * as path from 'path';
import { getTestConfiguration } from './test-configuration';

const execAsync = promisify(exec);

// Store the port-forward process PID for cleanup
const PID_FILE = path.join(__dirname, '..', 'port-forward.pid');
const PORT_FORWARD_SERVICE_NAME = 'traefik';
const PORT_FORWARD_NAMESPACE = 'traefik';
const PORT_FORWARD_SERVICE = `svc/${PORT_FORWARD_SERVICE_NAME}`;

async function globalSetup() {
    console.log('🔧 Starting Playwright Global Setup...');

    const testConfig = getTestConfiguration();

    // Check if we're running in CI - skip port-forward checks if so
    if (testConfig.isCI) {
        console.log('🚀 Running in CI environment - skipping port-forward checks');
        return;
    }

    await setupPortForwarding();
    console.log('✅ All preconditions passed - tests can proceed');
}

async function setupPortForwarding() {
    console.log('🔍 Checking port-forwarding setup...');

    const testConfig = getTestConfiguration();
    const localPort = testConfig.localPort || '8080';
    const expectedGatewayHost = process.env.GATEWAY_HOST;

    if (!expectedGatewayHost) {
        throw new Error('❌ GATEWAY_HOST environment variable is not set');
    }

    // Check if kubectl is available
    await checkKubectlAvailable();

    // Check if port-forwarding is already active
    const isPortForwardActive = await checkPortForwardingActive(localPort, false); // Don't throw error

    if (!isPortForwardActive) {
        console.log('🚀 Port-forwarding not active, starting automatically...');
        await startPortForwarding(localPort);

        // Wait a moment for port-forwarding to stabilize
        await new Promise(resolve => setTimeout(resolve, 3000));

        // Verify it's working
        await checkPortForwardingActive(localPort, true); // Throw error if still not working
    }

    // Check if the gateway is reachable through port-forwarding
    await checkGatewayConnectivity(localPort, expectedGatewayHost);
}

async function startPortForwarding(localPort: string): Promise<void> {
    try {
        // First, clean up any existing port-forward processes
        console.log('🧹 Cleaning up any existing port-forward processes...');
        try {
            await execAsync(`pkill -f "kubectl port-forward.*${localPort}:80"`);
            await new Promise(resolve => setTimeout(resolve, 1000)); // Wait for cleanup
        } catch (error) {
            // It's okay if no processes were found to kill - log and continue
            console.log(`   No existing port-forward processes found to cleanup: ${error instanceof Error ? error.message : 'Unknown error'}`);
        }

        console.log(`🔗 Starting kubectl port-forward on port ${localPort}...`);

        // Use spawn to start port-forwarding in background
        const portForwardProcess = spawn('kubectl', [
            'port-forward',
            PORT_FORWARD_SERVICE,
            `${localPort}:80`,
            '-n',
            PORT_FORWARD_NAMESPACE
        ], {
            detached: true,
            stdio: ['ignore', 'pipe', 'pipe']
        });

        // Store the PID for cleanup
        if (portForwardProcess.pid) {
            writeFileSync(PID_FILE, portForwardProcess.pid.toString());
            console.log(`✅ Port-forwarding started with PID: ${portForwardProcess.pid}`);

            // Unref the process so it doesn't keep the parent alive
            portForwardProcess.unref();
        } else {
            throw new Error('Failed to get process PID for port-forwarding');
        }

        // Wait for the port-forwarding to be ready
        await new Promise((resolve, reject) => {
            let output = '';
            let errorOutput = '';

            const timeout = setTimeout(() => {
                reject(new Error('Port-forwarding setup timed out'));
            }, 10000);

            portForwardProcess.stdout.on('data', (data: Buffer) => {
                output += data.toString();
                if (output.includes('Forwarding from')) {
                    clearTimeout(timeout);
                    resolve(undefined);
                }
            });

            portForwardProcess.stderr.on('data', (data: Buffer) => {
                errorOutput += data.toString();
            });

            portForwardProcess.on('error', (error: Error) => {
                clearTimeout(timeout);
                reject(new Error(`Failed to start port-forwarding: ${error.message}`));
            });

            portForwardProcess.on('exit', (code: number) => {
                if (code !== 0) {
                    clearTimeout(timeout);
                    reject(new Error(`Port-forwarding exited with code ${code}: ${errorOutput}`));
                }
            });
        });

    } catch (error) {
        throw new Error(`❌ Failed to start port-forwarding automatically:

🔧 Manual setup required:
   kubectl port-forward ${PORT_FORWARD_SERVICE} ${localPort}:80 -n ${PORT_FORWARD_NAMESPACE}

🔍 Troubleshooting:
   1. Check if the service exists: kubectl get svc ${PORT_FORWARD_SERVICE_NAME} -n ${PORT_FORWARD_NAMESPACE}
   2. Verify cluster access: kubectl cluster-info
   3. Check if port ${localPort} is already in use: lsof -i :${localPort}

Error details: ${error}`);
    }
}

async function checkKubectlAvailable(): Promise<void> {
    try {
        const { stdout } = await execAsync('kubectl version --client');
        const version = stdout.split('\n')[0]; // Get first line which contains client version
        console.log(`✅ kubectl is available: ${version}`);
    } catch (error) {
        throw new Error(`❌ kubectl is not available or not in PATH. Please install kubectl first.\nError: ${error}`);
    }
}

async function checkPortForwardingActive(localPort: string, throwOnError: boolean = true): Promise<boolean> {
    try {
        // Check if the port-forward process is running
        const { stdout } = await execAsync(`ps aux | grep "kubectl port-forward" | grep "${localPort}:80" | grep -v grep`);

        if (stdout.trim()) {
            console.log('✅ kubectl port-forward process is running');
            console.log(`   Process: ${stdout.trim().split(/\s+/).slice(0, 4).join(' ')}...`);
        } else {
            console.log('⚠️  No active kubectl port-forward process found');
            console.log('   Checking if port is accessible anyway...');
        }
    } catch (error) {
        // Process detection failed, but that's okay - we'll check port accessibility instead
        console.log('⚠️  Could not detect port-forward process (this is okay if port is accessible)');
        console.log(`   Detection error: ${error instanceof Error ? error.message : 'Unknown error'}`);
    }

    // Test if local port is actually accessible
    try {
        const context = await request.newContext();
        // Try to make a simple request to the local port
        const response = await context.get(`http://localhost:${localPort}`, {
            timeout: 5000,
            ignoreHTTPSErrors: true
        });

        // Any response (even 404) means the port is accessible
        console.log(`✅ Local port ${localPort} is accessible (status: ${response.status()})`);
        await context.dispose();
        return true;
    } catch (error) {
        const errorMessage = `❌ Local port ${localPort} is not accessible. Please ensure port-forwarding is active:

🔧 Run this command in a separate terminal:
   kubectl port-forward ${PORT_FORWARD_SERVICE} ${localPort}:80 -n ${PORT_FORWARD_NAMESPACE}

⚠️  Make sure the command is running in the background before starting tests.

Error details: ${error}`;

        if (throwOnError) {
            throw new Error(errorMessage);
        } else {
            console.log('⚠️  Port not accessible, will attempt to start port-forwarding automatically');
            return false;
        }
    }
}

async function checkGatewayConnectivity(localPort: string, expectedGatewayHost: string): Promise<void> {
    try {
        // Extract the host without port from GATEWAY_HOST if it includes port
        const hostWithoutPort = expectedGatewayHost.replace(/:8080$/, '');

        const context = await request.newContext();

        // Try to reach the gateway through localhost port-forwarding
        const response = await context.get(`http://localhost:${localPort}`, {
            timeout: 10000,
            ignoreHTTPSErrors: true,
            headers: {
                'Host': hostWithoutPort // Set the Host header for proper routing
            }
        });

        console.log(`✅ Gateway connectivity verified through port-forward (status: ${response.status()})`);
        console.log(`   Gateway host: ${hostWithoutPort}`);
        console.log(`   Local endpoint: http://localhost:${localPort}`);

        await context.dispose();
    } catch (error) {
        throw new Error(`❌ Cannot reach gateway through port-forwarding:

🔧 Troubleshooting steps:
   1. Ensure port-forwarding is running:
      kubectl port-forward ${PORT_FORWARD_SERVICE} 8080:80 -n ${PORT_FORWARD_NAMESPACE}

   2. Check if the service exists:
      kubectl get svc ${PORT_FORWARD_SERVICE_NAME} -n ${PORT_FORWARD_NAMESPACE}

   3. Verify the gateway service is healthy:
      kubectl get pods -n default

   4. Test the connection manually:
      curl -H "Host: ${expectedGatewayHost.replace(/:8080$/, '')}" http://localhost:${localPort}

Error details: ${error}`);
    }
}

export default globalSetup;
