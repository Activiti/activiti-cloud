import { timeouts } from '../../../config/runtime/timeouts';

export function normalizeBearer(token: string): string {
    return token.startsWith('Bearer ') ? token : `Bearer ${token}`;
}

export async function settleAfterSubscribe(): Promise<void> {
    const settleMs = timeouts.engineEvents.subscriptionSettle;
    if (settleMs <= 0) {
        return;
    }
    await new Promise((resolve) => setTimeout(resolve, settleMs));
}
