export const RB_V1_BASE = '/rb/v1';
export const RB_ADMIN_V1_BASE = '/rb/admin/v1';

export function rbV1Base(runtimeBasePath: string = '/rb'): string {
    return `${runtimeBasePath.replace(/\/$/, '')}/v1`;
}
