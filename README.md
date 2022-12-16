# activiti-cloud-application

[![Join Us in Gitter](https://badges.gitter.im/Activiti/Activiti7.svg)](https://gitter.im/Activiti/Activiti7?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![CI](https://github.com/Activiti/activiti-cloud-application/actions/workflows/main.yml/badge.svg)](https://github.com/Activiti/activiti-cloud-application/actions/workflows/main.yml)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/3fc72a72b158430faeee15ce2db29f5a)](https://www.codacy.com/gh/Activiti/activiti-cloud-application?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=Activiti/activiti-cloud-application&amp;utm_campaign=Badge_Grade)
[![ASL 2.0](https://img.shields.io/hexpm/l/plug.svg)](https://github.com/Activiti/activiti-cloud-application/blob/master/LICENSE.txt)
[![CLA](https://cla-assistant.io/readme/badge/Activiti/activiti-cloud-application)](https://cla-assistant.io/Activiti/activiti-cloud-application)
[![security status](https://www.meterian.com/badge/gh/Activiti/activiti-cloud-application/security)](https://www.meterian.com/report/gh/Activiti/activiti-cloud-application)
[![stability status](https://www.meterian.com/badge/gh/Activiti/activiti-cloud-application/stability)](https://www.meterian.com/report/gh/Activiti/activiti-cloud-application)
[![licensing status](https://www.meterian.io/badge/gh/Activiti/activiti-cloud-application/licensing)](https://www.meterian.io/report/gh/Activiti/activiti-cloud-application)

Activiti Cloud Application example and acceptance test suite.

## Formatting

The local `.editorconfig` file is leveraged for automated formatting.

Pre-commit hooks run on CI include:

- local hooks
- shared hooks leveraging prettier

See documentation at [pre-commit](https://github.com/Alfresco/alfresco-build-tools/tree/master/docs#pre-commit) and [pre-commit-default](https://github.com/Alfresco/alfresco-build-tools/tree/master/docs#pre-commit-default)

To run all hooks locally:

```sh
pre-commit run -a
pre-commit run -a --config /path/to/config/format-config.yaml
pre-commit run -a --config /path/to/config/github-config.yaml
```
