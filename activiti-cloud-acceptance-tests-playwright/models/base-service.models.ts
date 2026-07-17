/*
 * Copyright 2005-2023 Alfresco Software, Ltd. All rights reserved.
 * License rights for this program may be obtained from Alfresco Software, Ltd.
 * pursuant to a written agreement and any use of this program without such an
 * agreement is prohibited.
 */

import * as fs from 'fs';

export interface SearchPageParams {
    skipCount?: number;
    maxItems?: number;
    sort?: string[];
}

export interface HttpStatusCheck<TService> {
    readonly label: string;
    readonly run: (service: TService) => Promise<number>;
}

export interface Options {
    data?: string | Buffer | any;
    failOnStatusCode?: boolean;
    form?: { [key: string]: string | number | boolean };
    headers?: { [key: string]: string };
    ignoreHTTPSErrors?: boolean;
    maxRedirects?: number;
    params?: { [key: string]: string | number | boolean };
    multipart?: {
        [key: string]:
            | string
            | number
            | boolean
            | fs.ReadStream
            | {
                  name: string;
                  mimeType: string;
                  buffer: Buffer;
              };
    };
    timeout?: number;
}
