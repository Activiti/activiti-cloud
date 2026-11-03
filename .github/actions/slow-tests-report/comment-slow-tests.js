/** Parse surefire XML reports and post/update a PR comment with slow test results. */

const fs = require('fs');
const path = require('path');

/**
 * @param {string} dir
 * @returns {string[]}
 */
function findXmlFiles(dir) {
  let results = [];
  if (!fs.existsSync(dir)) return results;
  const entries = fs.readdirSync(dir, { withFileTypes: true });
  for (const entry of entries) {
    const fullPath = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      results = results.concat(findXmlFiles(fullPath));
    } else if (entry.name.startsWith('TEST-') && entry.name.endsWith('.xml')) {
      results.push(fullPath);
    }
  }
  return results;
}

/**
 * @param {string} xmlContent
 */
function parseTestCases(xmlContent) {
  const testCases = [];
  const regex = /<testcase\s+([^>]*)[\/]?>|<testcase\s+([^>]*)>/g;
  for (const match of xmlContent.matchAll(regex)) {
    const attrs = match[1] || match[2];
    const nameMatch = attrs.match(/name="([^"]*)"/);
    const classMatch = attrs.match(/classname="([^"]*)"/);
    const timeMatch = attrs.match(/time="([^"]*)"/);
    if (nameMatch && classMatch && timeMatch) {
      const time = parseFloat(timeMatch[1]);
      if (!isNaN(time)) {
        testCases.push({ name: nameMatch[1], classname: classMatch[1], time });
      }
    }
  }
  return testCases;
}

/**
 * @param {string} filePath
 * @returns {'unit' | 'integration'}
 */
function classifyTestFile(filePath) {
  if (filePath.includes('failsafe-reports')) {
    return 'integration';
  }
  return 'unit';
}

function collectTests(dir) {
  const xmlFiles = findXmlFiles(dir);
  let tests = [];
  for (const file of xmlFiles) {
    const content = fs.readFileSync(file, 'utf8');
    tests = tests.concat(parseTestCases(content));
  }
  return tests;
}

/**
 * Collect tests split by type (unit vs integration).
 * @param {string} dir
 * @returns {{ unit: Array, integration: Array }}
 */
function collectTestsByType(dir) {
  const xmlFiles = findXmlFiles(dir);
  const unit = [];
  const integration = [];
  for (const file of xmlFiles) {
    const content = fs.readFileSync(file, 'utf8');
    const cases = parseTestCases(content);
    if (classifyTestFile(file) === 'integration') {
      integration.push(...cases);
    } else {
      unit.push(...cases);
    }
  }
  return { unit, integration };
}

function buildSlowTestsTable(tests, threshold) {
  return tests
    .filter(testCase => testCase.time > threshold)
    .sort((a, b) => b.time - a.time)
    .slice(0, 20);
}

function buildSlowClassesTable(tests, classThreshold, slowTestThreshold) {
  const classMap = {};
  for (const testCase of tests) {
    if (!classMap[testCase.classname]) {
      classMap[testCase.classname] = {
        classname: testCase.classname,
        totalTime: 0,
        totalCount: 0,
        slowTime: 0,
        slowCount: 0,
      };
    }
    classMap[testCase.classname].totalTime += testCase.time;
    classMap[testCase.classname].totalCount += 1;
    if (testCase.time > slowTestThreshold) {
      classMap[testCase.classname].slowTime += testCase.time;
      classMap[testCase.classname].slowCount += 1;
    }
  }
  return Object.values(classMap)
    .filter(testClass => testClass.totalTime > classThreshold)
    .sort((a, b) => b.totalTime - a.totalTime)
    .slice(0, 10);
}

function formatSlowClassesTable(classes, slowTestThreshold) {
  const rows = classes.map((testClass, index) =>
    `| ${testClass.totalTime.toFixed(2)}s | ${index + 1} | ${testClass.totalCount} | ${testClass.slowTime.toFixed(2)}s | ${testClass.slowCount} | \`${escapeMarkdown(testClass.classname)}\` |`
  ).join('\n');
  return `| Total Time | # | All Tests | Time > ${slowTestThreshold}s | Tests > ${slowTestThreshold}s | Class |\n|------------|---|-----------|------------------|-------------------|-------|\n${rows}`;
}

function escapeMarkdown(value) {
  return value.replace(/[|`\\]/g, '\\$&');
}

async function postSlowTestsReport({ github, context, core }) {
  const SLOW_THRESHOLD_SECONDS = parseInt(process.env.SLOW_THRESHOLD || '5', 10);
  const SLOW_CLASS_THRESHOLD_SECONDS = parseInt(process.env.SLOW_CLASS_THRESHOLD || '30', 10);
  const reportsDir = process.env.REPORTS_DIR || 'reports';
  const { unit: unitTests, integration: integrationTests } = collectTestsByType(reportsDir);

  core.info(`Found ${unitTests.length} unit test cases`);
  core.info(`Found ${integrationTests.length} integration test cases`);

  const slowUnitTests = buildSlowTestsTable(unitTests, SLOW_THRESHOLD_SECONDS);
  const slowUnitClasses = buildSlowClassesTable(unitTests, SLOW_CLASS_THRESHOLD_SECONDS, SLOW_THRESHOLD_SECONDS);
  const slowIntegrationTests = buildSlowTestsTable(integrationTests, SLOW_THRESHOLD_SECONDS);
  const slowIntegrationClasses = buildSlowClassesTable(integrationTests, SLOW_CLASS_THRESHOLD_SECONDS, SLOW_THRESHOLD_SECONDS);
  const marker = '<!-- slow-tests-report -->';
  let body = `${marker}\n## 🐢 Slow Tests Report\n`;

  body += `\n### 🧪 Unit Tests\n`;
  if (slowUnitTests.length === 0) {
    body += `\n✅ No unit tests slower than ${SLOW_THRESHOLD_SECONDS} seconds.`;
  } else {
    const rows = slowUnitTests.map((testCase, index) =>
      `| ${testCase.time.toFixed(2)}s | ${index + 1} | \`${escapeMarkdown(testCase.classname)}\` | \`${escapeMarkdown(testCase.name)}\` |`
    ).join('\n');
    body += `\nThe following ${slowUnitTests.length} unit test(s) took more than ${SLOW_THRESHOLD_SECONDS} seconds:\n\n| Time | # | Class | Test |\n|------|---|-------|------|\n${rows}`;
  }

  if (slowUnitClasses.length > 0) {
    body += `\n\n#### 🏋️ Slowest Unit Test Classes (aggregated)\n\nThe following ${slowUnitClasses.length} class(es) took more than ${SLOW_CLASS_THRESHOLD_SECONDS} seconds in total:\n\n${formatSlowClassesTable(slowUnitClasses, SLOW_THRESHOLD_SECONDS)}`;
  }

  body += `\n\n---\n\n### 🔗 Integration Tests\n`;
  if (slowIntegrationTests.length === 0) {
    body += `\n✅ No integration tests slower than ${SLOW_THRESHOLD_SECONDS} seconds.`;
  } else {
    const rows = slowIntegrationTests.map((testCase, index) =>
      `| ${testCase.time.toFixed(2)}s | ${index + 1} | \`${escapeMarkdown(testCase.classname)}\` | \`${escapeMarkdown(testCase.name)}\` |`
    ).join('\n');
    body += `\nThe following ${slowIntegrationTests.length} integration test(s) took more than ${SLOW_THRESHOLD_SECONDS} seconds:\n\n| Time | # | Class | Test |\n|------|---|-------|------|\n${rows}`;
  }

  if (slowIntegrationClasses.length > 0) {
    body += `\n\n#### 🏋️ Slowest Integration Test Classes (aggregated)\n\nThe following ${slowIntegrationClasses.length} class(es) took more than ${SLOW_CLASS_THRESHOLD_SECONDS} seconds in total:\n\n${formatSlowClassesTable(slowIntegrationClasses, SLOW_THRESHOLD_SECONDS)}`;
  }

  const reportUrl = process.env.REPORT_URL;
  if (reportUrl) {
    body += `\n\n---\n\n📊 [Full Slow Test Report](${reportUrl})`;
  }

  const { owner, repo } = context.repo;
  const pullRequest = context.payload.pull_request;
  if (!pullRequest) {
    core.info('Not a pull request context, skipping comment');
    return;
  }

  const issueNumber = pullRequest.number;
  const { data: comments } = await github.rest.issues.listComments({
    owner,
    repo,
    issue_number: issueNumber,
    per_page: 100,
  });
  const existing = comments.find(comment => comment.body && comment.body.includes(marker));

  if (existing) {
    await github.rest.issues.updateComment({ owner, repo, comment_id: existing.id, body });
    core.info('Updated existing slow tests comment');
  } else {
    await github.rest.issues.createComment({ owner, repo, issue_number: issueNumber, body });
    core.info('Created new slow tests comment');
  }
}

module.exports = postSlowTestsReport;
module.exports.buildSlowClassesTable = buildSlowClassesTable;
module.exports.formatSlowClassesTable = formatSlowClassesTable;
