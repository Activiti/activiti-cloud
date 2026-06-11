/*
 * Copyright 2005-2023 Alfresco Software, Ltd. All rights reserved.
 * License rights for this program may be obtained from Alfresco Software, Ltd.
 * pursuant to a written agreement and any use of this program without such an
 * agreement is prohibited.
 */

import chalk from 'chalk';
import { logger } from './logger-builder';

type MessageType = 'ERROR' | 'DEBUG' | 'WARN' | 'INFO';
export class Logger {
    static info(...message: any[]) {
        this.log('INFO', message);
    }

    static error(...message: any[]) {
        this.log('ERROR', message);
    }

    static warn(...message: any[]) {
        this.log('WARN', message);
    }

    static debug(...message: any[]) {
        this.log('DEBUG', message);
    }

    private static log(messageType: MessageType, ...message: any[]) {
        const colors = {
            DEBUG: (text: string) => text,
            ERROR: chalk.red,
            WARN: chalk.yellow,
            INFO: chalk.cyan
        };
        const color = colors[messageType] || chalk.blue;

        const logMessage = color(`${message}`);

        switch (messageType.toLowerCase()) {
            case 'error':
                logger.error(logMessage);
                break;
            case 'warn':
                logger.warn(logMessage);
                break;
            case 'info':
                logger.info(logMessage);
                break;
            case 'debug':
                logger.debug(logMessage);
                break;
            default:
                logger.info(logMessage);
        }
    }
}
