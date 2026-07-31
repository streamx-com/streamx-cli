---
title: "streamx project repo ssh-key"
sidebar_label: "ssh-key"
sidebar_position: 0
description: "Manage the SSH deploy key of the connected repository"
---

# `streamx project repo ssh-key`

Manage the SSH deploy key of the connected repository

```bash
streamx project repo ssh-key <command>
```

## Subcommands

| Command | Description |
| --- | --- |
| [`generate`](./generate) | Generate a new SSH key pair server-side and save it to files |
| [`remove`](./remove) | Remove the SSH deploy key from the repository connection |
| [`set`](./set) | Set the SSH private key used to access the repository |
| [`show`](./show) | Print the public key of the configured SSH deploy key |

---

Every command also accepts the [global options](../../../global-options.md).
