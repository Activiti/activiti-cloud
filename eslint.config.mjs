import tseslint from 'typescript-eslint';

const playwrightFiles = ['activiti-cloud-acceptance-tests-playwright/**/*.ts'];

export default tseslint.config(
    {
        ignores: ['**/node_modules/**', '**/dist/**', '**/test-results/**', '**/playwright-report/**'],
    },
    {
        files: playwrightFiles,
        languageOptions: {
            parser: tseslint.parser,
            parserOptions: {
                projectService: true,
                tsconfigRootDir: import.meta.dirname,
            },
        },
        rules: {
            'no-restricted-syntax': [
                'error',
                {
                    selector:
                        'CallExpression[callee.type="MemberExpression"][callee.property.name="poll"][callee.object.type="MemberExpression"][callee.object.property.name="expect"]',
                    message: 'Use expectPoll() from helpers/expect-poll.ts instead of expect.poll().',
                },
            ],
        },
    },
    {
        files: ['activiti-cloud-acceptance-tests-playwright/helpers/expect-poll.ts'],
        rules: {
            'no-restricted-syntax': 'off',
        },
    }
);
