export const QUERY_V1_BASE = '/query/v1';
export const QUERY_ADMIN_V1_BASE = '/query/admin/v1';

export function queryV1Base(admin: boolean): string {
    return admin ? QUERY_ADMIN_V1_BASE : QUERY_V1_BASE;
}
