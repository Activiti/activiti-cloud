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

import { exec } from 'child_process';
import { promisify } from 'util';
import { readFileSync, unlinkSync, existsSync } from 'fs';
import * as path from 'path';

const execAsync = promisify(exec);

// Path to the PID file created by global-setup
const PID_FILE = path.join(__dirname, '..', 'port-forward.pid');

async function globalTeardown() {
    console.log('🧹 Starting Playwright Global Teardown...');

    // Check if we're running in CI - skip port-forward cleanup if so
    if (process.env.CI || process.env.GITHUB_ACTIONS) {
        console.log('🚀 Running in CI environment - skipping port-forward cleanup');
        return;
    }

    await cleanupPortForwarding();
    console.log('✅ Global teardown completed');
}

async function cleanupPortForwarding(): Promise<void> {
    console.log('🧹 Cleaning up port-forwarding processes...');

    try {
        await cleanupSpecificProcess();
        await cleanupRemainingProcesses();
        console.log('✅ Port-forwarding cleanup completed');
    } catch (error) {
        console.log(`⚠️  Port-forwarding cleanup encountered an issue: ${error instanceof Error ? error.message : 'Unknown error'}`);
        console.log('   This is usually not critical - processes may have already terminated');
    }
}

async function cleanupSpecificProcess(): Promise<void> {
    if (!existsSync(PID_FILE)) {
        return;
    }

    try {
        const pidString = readFileSync(PID_FILE, 'utf-8').trim();
        const pid = parseInt(pidString, 10);

        if (!isNaN(pid)) {
            console.log(`🎯 Attempting to terminate port-forward process with PID: ${pid}`);
            await terminateProcess(pid);
        }

        // Clean up the PID file
        unlinkSync(PID_FILE);
        console.log('🗑️  Removed PID file');
    } catch (error) {
        console.log(`⚠️  Could not read/process PID file: ${error instanceof Error ? error.message : 'Unknown error'}`);
    }
}

async function terminateProcess(pid: number): Promise<void> {
    try {
        process.kill(pid, 'SIGTERM');
        console.log(`✅ Successfully terminated process ${pid}`);

        // Wait a moment for graceful shutdown
        await new Promise(resolve => setTimeout(resolve, 1000));

        // Check if it's still running, if so use SIGKILL
        if (isProcessRunning(pid)) {
            console.log(`⚠️  Process ${pid} still running, using SIGKILL...`);
            process.kill(pid, 'SIGKILL');
        }
    } catch (error) {
        console.log(`⚠️  Could not terminate process ${pid}: ${error instanceof Error ? error.message : 'Unknown error'}`);
        console.log('   Process may have already terminated or access denied');
    }
}

function isProcessRunning(pid: number): boolean {
    try {
        process.kill(pid, 0); // This will throw if process doesn't exist
        return true;
    } catch {
        console.log(`✅ Process ${pid} terminated successfully`);
        return false;
    }
}

async function cleanupRemainingProcesses(): Promise<void> {
    console.log('🧹 Cleaning up any remaining kubectl port-forward processes...');
    try {
        await execAsync('pkill -f "kubectl port-forward.*:80"');
        console.log('✅ Cleaned up remaining port-forward processes');
    } catch (error) {
        // It's okay if no processes were found - log and continue
        console.log(`   No additional port-forward processes found: ${error instanceof Error ? error.message : 'Unknown error'}`);
    }
}

export default globalTeardown;
