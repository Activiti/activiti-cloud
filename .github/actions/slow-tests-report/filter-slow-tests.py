"""Copy JUnit XML reports and retain only slow test cases in the copy."""

import os
import shutil
import sys
import xml.etree.ElementTree as ET

THRESHOLD = float(os.environ.get("SLOW_THRESHOLD", "5.0"))
source_reports_dir = os.environ.get("REPORTS_DIR", "reports")
filtered_reports_dir = os.environ.get("FILTERED_REPORTS_DIR", source_reports_dir)

if os.path.abspath(source_reports_dir) != os.path.abspath(filtered_reports_dir):
    if os.path.isdir(source_reports_dir):
        shutil.copytree(source_reports_dir, filtered_reports_dir)
    else:
        os.makedirs(filtered_reports_dir)

reports_dir = filtered_reports_dir

retained = 0
removed = 0

for root_dir, _, files in os.walk(reports_dir):
    for fname in files:
        if not (fname.startswith("TEST-") and fname.endswith(".xml")):
            continue
        fpath = os.path.join(root_dir, fname)
        try:
            ET.register_namespace("", "")
            tree = ET.parse(fpath)
            suite = tree.getroot()
            to_remove = []
            for tc in suite.findall("testcase"):
                time_str = tc.get("time", "0")
                try:
                    t = float(time_str)
                except ValueError:
                    t = 0.0
                if t <= THRESHOLD:
                    to_remove.append(tc)
                    removed += 1
                else:
                    retained += 1
            for tc in to_remove:
                suite.remove(tc)
            slow_cases = suite.findall("testcase")
            for tc in slow_cases:
                suite.remove(tc)
            slow_cases.sort(
                key=lambda elem: float(elem.get("time", "0") or "0"), reverse=True
            )
            for tc in slow_cases:
                suite.append(tc)
            tree.write(fpath, encoding="unicode", xml_declaration=True)
        except ET.ParseError as error:
            print(f"Warning: could not parse {fpath}: {error}", file=sys.stderr)

print(f"Retained {retained} slow test case(s) (>{THRESHOLD}s), removed {removed} fast test case(s)")
