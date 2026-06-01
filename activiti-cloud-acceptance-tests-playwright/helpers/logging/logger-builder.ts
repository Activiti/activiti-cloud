/*
 * Copyright 2005-2023 Alfresco Software, Ltd. All rights reserved.
 * License rights for this program may be obtained from Alfresco Software, Ltd.
 * pursuant to a written agreement and any use of this program without such an
 * agreement is prohibited.
 */

import { mkdirSync } from 'fs';
import * as path from 'path';
import * as winston from 'winston';
import { createLogger, format } from 'winston';
import '../../config/load-env';
import { paths } from '../../config/paths';

const level = process.env.LOG_LEVEL || 'debug';
const logDir = paths.testResults;
mkdirSync(logDir, { recursive: true });
const combinedLogPath = path.join(logDir, 'combined.log');
const colorizer = winston.format.colorize();
const myFormat = format.printf((info) => colorizer.colorize(info.level, `${info.timestamp} [${info.level}]: `) + info.message);

export const logger = createLogger({
    level,
    transports: [
        new winston.transports.File({ filename: combinedLogPath, level: 'debug' }),
        new winston.transports.Console({
            format: format.combine(
                format.timestamp({
                    format: 'YYYY-MM-DD HH:mm:ss'
                }),
                format.simple(),
                myFormat
            )
        })
    ]
});
