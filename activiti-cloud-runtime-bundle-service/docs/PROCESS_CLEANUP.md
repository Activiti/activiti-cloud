# Process Instance Cleanup Configuration

## Overview

To prevent data inconsistency between Runtime Bundle and Query Service, process instances are not immediately deleted when they complete. Instead, they are kept for a configurable grace period to allow the Query Service to process all events before deletion.

## Configuration Properties

| Property | Default | Description |
|----------|---------|-------------|
| `activiti.cloud.process-cleanup.enabled` | `true` | Enable/disable scheduled cleanup |
| `activiti.cloud.process-cleanup.grace-period` | `5m` | How long to wait before deleting completed processes |
| `activiti.cloud.process-cleanup.cleanup-interval` | `1m` | How often to run the cleanup job |
| `activiti.cloud.process-cleanup.batch-size` | `100` | Maximum processes to delete per run |

## Example Configuration

```yaml
activiti:
  cloud:
    process-cleanup:
      enabled: true
      grace-period: 5m
      cleanup-interval: 1m
      batch-size: 100
```

## Disabling Cleanup

To disable automatic cleanup (e.g., for development/testing):

```yaml
activiti:
  cloud:
    process-cleanup:
      enabled: false
```

## Monitoring

The cleanup scheduler logs:
- **DEBUG**: Each cleanup execution and individual deletions
- **INFO**: Number of instances found for cleanup
- **ERROR**: Failures during cleanup

Search logs for: `ProcessInstanceCleanupScheduler`
