---
title: "streamx project clusters enable"
sidebar_label: "enable"
sidebar_position: 2
description: "Enable one more cluster for a project"
---

# `streamx project clusters enable`

Enable one more cluster for a project

```bash
streamx project clusters enable [options] <clusterId>
```

## Arguments

| Argument | Required | Description |
| --- | --- | --- |
| `<clusterId>` | yes | Cluster ID, as shown by 'streamx org clusters list' |

## Options

| Option | Value | Description |
| --- | --- | --- |
| `--org` | `<orgId>` | Organization ID (defaults to the current organization) |
| `--project` | `<projectId>` | Project ID (defaults to the current project) |
| `-v`, `--verbose` |  | Print debug information |

---

Every command also accepts the [global options](../../global-options.md).
