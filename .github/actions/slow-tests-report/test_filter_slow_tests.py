import os
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest
import xml.etree.ElementTree as ET


class FilterSlowTestsTest(unittest.TestCase):
    def test_filters_a_copy_and_preserves_source_reports(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            source = Path(temp_dir, "reports")
            filtered = Path(temp_dir, "filtered")
            report = source / "module" / "TEST-example.xml"
            report.parent.mkdir(parents=True)
            report.write_text(
                '<testsuite>'
                '<testcase classname="Example" name="fast" time="4.9" />'
                '<testcase classname="Example" name="slow" time="6.1" />'
                '</testsuite>',
                encoding="utf-8",
            )

            env = os.environ.copy()
            env.update(
                {
                    "REPORTS_DIR": str(source),
                    "FILTERED_REPORTS_DIR": str(filtered),
                    "SLOW_THRESHOLD": "5",
                }
            )
            subprocess.run(
                [sys.executable, str(Path(__file__).with_name("filter-slow-tests.py"))],
                env=env,
                check=True,
                capture_output=True,
                text=True,
            )

            source_cases = ET.parse(report).getroot().findall("testcase")
            filtered_cases = ET.parse(
                filtered / "module" / "TEST-example.xml"
            ).getroot().findall("testcase")

            self.assertEqual([case.get("name") for case in source_cases], ["fast", "slow"])
            self.assertEqual([case.get("name") for case in filtered_cases], ["slow"])


if __name__ == "__main__":
    unittest.main()
