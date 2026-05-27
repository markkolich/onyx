const js = require('@eslint/js');
const globals = require('globals');

module.exports = [
    js.configs.recommended,
    {
        files: ['js/onyx/**/*.js'],
        languageOptions: {
            ecmaVersion: 2015,
            sourceType: 'script',
            globals: {
                ...globals.browser,
                ...globals.jquery,
                Onyx: 'writable',
                _: 'writable',
                marked: 'writable',
                DOMPurify: 'writable',
            },
        },
        rules: {
            'comma-dangle': 'error',
            'curly': 'error',
            'eol-last': 'error',
            'eqeqeq': 'error',
            'max-len': ['error', {
                'code': 120,
                'tabWidth': 4,
            }],
            'indent': ['error', 4, {
                'SwitchCase': 1,
            }],
            'no-tabs': 'error',
            'no-eval': 'error',
            'no-implicit-globals': 'error',
            'no-trailing-spaces': 'error',
            'no-unused-expressions': ['error', {
                'allowShortCircuit': true,
            }],
            'no-unused-vars': ['error', {
                'varsIgnorePattern': '^(path|self)$',
                'argsIgnorePattern': '^(e|path|window|document|res|status|xhr|data|success|error|complete|key|options)$',
            }],
            'semi': 'error',
            'space-before-blocks': 'error',
            'space-before-function-paren': ['error', {
                'anonymous': 'never',
                'named': 'never',
            }],
            'space-infix-ops': 'error',
            'keyword-spacing': ['error', {
                'before': true,
            }],
            'strict': 'error',
            'quotes': ['error', 'single'],
        },
    },
];
