/*
 * Central expect.poll wrapper — always uses profile-based timeouts from config/runtime/timeouts.
 */

import { expect } from '@playwright/test';
import { PollProfile, pollOptions } from '../config/runtime/timeouts';

type PollOverrides = Partial<ReturnType<typeof pollOptions>>;

export function expectPoll<T>(
    pollFn: () => Promise<T>,
    profile: PollProfile = 'querySync',
    overrides?: PollOverrides
): ReturnType<typeof expect.poll> {
    return expect.poll(pollFn, { ...pollOptions(profile), ...overrides });
}
