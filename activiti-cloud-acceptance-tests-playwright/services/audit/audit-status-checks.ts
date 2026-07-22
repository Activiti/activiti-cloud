/*
 * Copyright 2017-2026 Hyland Software, Inc. and its affiliates.
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

import { HttpStatusCheck } from '../../models/base-service.models';
import { BaseService } from '../base.service';
import type { AuditService } from './audit.service';
import { AUDIT_ADMIN_V1_BASE, AUDIT_V1_BASE } from './endpoints/index';

export class AuditStatusChecks {
    buildUnauthenticatedGetStatusChecks(): readonly HttpStatusCheck<AuditService>[];
    buildUnauthenticatedGetStatusChecks(fakeResourceId: string): readonly HttpStatusCheck<AuditService>[];
    buildUnauthenticatedGetStatusChecks(fakeResourceId?: string): readonly HttpStatusCheck<AuditService>[] {
        if (fakeResourceId === undefined) {
            return [
                BaseService.getStatusCheck<AuditService>('events list', `${AUDIT_ADMIN_V1_BASE}/events`),
                BaseService.getStatusCheck<AuditService>(
                    'events export',
                    `${AUDIT_ADMIN_V1_BASE}/events/export/pw-unauth-export.csv?from=2020-01-01&to=2020-01-31`
                ),
            ];
        }
        return [
            BaseService.getStatusCheck<AuditService>('events list', `${AUDIT_V1_BASE}/events`),
            BaseService.getStatusCheck<AuditService>('event by id', `${AUDIT_V1_BASE}/events/${encodeURIComponent(fakeResourceId)}`),
        ];
    }

    buildBadRequestGetStatusChecks(): readonly HttpStatusCheck<AuditService>[] {
        return [
            BaseService.getStatusCheck<AuditService>(
                'events export with invalid dates',
                `${AUDIT_ADMIN_V1_BASE}/events/export/pw-invalid-export.csv?from=not-a-date&to=also-invalid`
            ),
        ];
    }

    buildForbiddenGetStatusChecks(): readonly HttpStatusCheck<AuditService>[];
    buildForbiddenGetStatusChecks(eventId: string): readonly HttpStatusCheck<AuditService>[];
    buildForbiddenGetStatusChecks(eventId?: string): readonly HttpStatusCheck<AuditService>[] {
        if (eventId === undefined) {
            return [
                BaseService.getStatusCheck<AuditService>('events list', `${AUDIT_ADMIN_V1_BASE}/events`),
                BaseService.getStatusCheck<AuditService>(
                    'events export',
                    `${AUDIT_ADMIN_V1_BASE}/events/export/pw-forbidden-export.csv?from=2020-01-01&to=2020-01-31`
                ),
            ];
        }
        return [BaseService.getStatusCheck<AuditService>('event by id', `${AUDIT_V1_BASE}/events/${encodeURIComponent(eventId)}`)];
    }
}
