'use strict';

const assert = require('node:assert/strict');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const postSlowTestsReport = require('./comment-slow-tests');
const { buildSlowClassesTable, formatSlowClassesTable } = postSlowTestsReport;

const tests = [
  { classname: 'CumulativeClass', name: 'first', time: 4 },
  { classname: 'CumulativeClass', name: 'second', time: 4 },
  { classname: 'CumulativeClass', name: 'third', time: 4 },
  { classname: 'MixedClass', name: 'slow', time: 7 },
  { classname: 'MixedClass', name: 'fast', time: 3 },
];

const classes = buildSlowClassesTable(tests, 9, 5);
assert.deepEqual(classes.map(testClass => testClass.classname), ['CumulativeClass', 'MixedClass']);

const table = formatSlowClassesTable(classes, 5);
assert.match(table, /\| Total Time \| # \| All Tests \| Time > 5s \| Tests > 5s \| Class \|/);
assert.match(table, /\| 12\.00s \| 1 \| 3 \| 0\.00s \| 0 \| `CumulativeClass` \|/);

async function verifyGeneratedCommentUsesAllTestCases() {
  const reportsDir = fs.mkdtempSync(path.join(os.tmpdir(), 'slow-tests-report-'));
  const reportDir = path.join(reportsDir, 'artifact', 'target', 'failsafe-reports');
  fs.mkdirSync(reportDir, { recursive: true });
  fs.writeFileSync(
    path.join(reportDir, 'TEST-cumulative.xml'),
    '<testsuite>' +
      '<testcase classname="CumulativeClass" name="first" time="4" />' +
      '<testcase classname="CumulativeClass" name="second" time="4" />' +
      '<testcase classname="CumulativeClass" name="third" time="4" />' +
      '</testsuite>',
  );

  let commentBody = '';
  const github = {
    rest: {
      issues: {
        listComments: async () => ({ data: [] }),
        createComment: async (/** @type {{ body: string }} */ comment) => { commentBody = comment.body; },
      },
    },
  };
  const context = {
    repo: { owner: 'example', repo: 'repository' },
    payload: { pull_request: { number: 1 } },
  };
  const core = { info: () => {} };

  const previousReportsDir = process.env.REPORTS_DIR;
  const previousSlowThreshold = process.env.SLOW_THRESHOLD;
  const previousClassThreshold = process.env.SLOW_CLASS_THRESHOLD;
  try {
    process.env.REPORTS_DIR = reportsDir;
    process.env.SLOW_THRESHOLD = '5';
    process.env.SLOW_CLASS_THRESHOLD = '9';
    await postSlowTestsReport({ github, context, core });
  } finally {
    if (previousReportsDir === undefined) delete process.env.REPORTS_DIR;
    else process.env.REPORTS_DIR = previousReportsDir;
    if (previousSlowThreshold === undefined) delete process.env.SLOW_THRESHOLD;
    else process.env.SLOW_THRESHOLD = previousSlowThreshold;
    if (previousClassThreshold === undefined) delete process.env.SLOW_CLASS_THRESHOLD;
    else process.env.SLOW_CLASS_THRESHOLD = previousClassThreshold;
    fs.rmSync(reportsDir, { recursive: true, force: true });
  }

  assert.match(commentBody, /No integration tests slower than 5 seconds/);
  assert.match(commentBody, /\| 12\.00s \| 1 \| 3 \| 0\.00s \| 0 \| `CumulativeClass` \|/);
}

verifyGeneratedCommentUsesAllTestCases()
  .then(() => console.log('slow-tests-report aggregation tests passed'))
  .catch(error => {
    console.error(error);
    process.exit(1);
  });