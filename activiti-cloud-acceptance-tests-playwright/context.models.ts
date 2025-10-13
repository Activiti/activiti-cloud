/*
 * Copyright 2005-2023 Alfresco Software, Ltd. All rights reserved.
 * License rights for this program may be obtained from Alfresco Software, Ltd.
 * pursuant to a written agreement and any use of this program without such an
 * agreement is prohibited.
 */

import { APIRequestContext } from '@playwright/test';
import { JwtPayload } from 'jwt-decode';

export interface UserData {
    username: string;
    password: string;
}

export interface AuthFormData {
    form: {
        username?: string;
        password?: string;
        grant_type?: string;
        client_id?: string;
        client_secret?: string;
        scope?: string;
    };
}

export interface TokenDetails {
    access_token: string;
    expires_in: string;
}

export interface CustomAPIRequest extends APIRequestContext {
    token: string;
    expires_in: Date;
    username?: string;
}

export interface ExtendedJwtPayload extends JwtPayload {
    idp: string;
    hxp_account: string;
    preferred_username: string;
    email: string;
    email_verified: boolean;
}
