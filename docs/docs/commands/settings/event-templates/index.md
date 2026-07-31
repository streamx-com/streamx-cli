---
title: "streamx settings event-templates"
sidebar_label: "event-templates"
sidebar_position: 0
description: "Manage StreamX event templates"
---

# `streamx settings event-templates`

Manage StreamX event templates

```bash
streamx settings event-templates <command>
```

## Subcommands

| Command | Description |
| --- | --- |
| [`copy`](./copy) | Copy an existing event template under a new ID |
| [`create`](./create) | Create a new event template (interactive wizard) |
| [`delete`](./delete) | Delete a user-created event template |
| [`edit`](./edit) | Open an event template in $EDITOR |
| [`get`](./get) | Show the content of an event template |
| [`list`](./list) | Display all available event templates |
| [`placeholders`](./placeholders) | List the placeholders that may be used inside event templates |
| [`register`](./register) | Register an event template file under a template ID (writes to settings) |
| [`rename`](./rename) | Rename an event template |
| [`reset-default-templates`](./reset-default-templates) | Delete and repopulate the &lt;streamxHome&gt;/default-event-templates folder |
| [`unregister`](./unregister) | Unregister a settings-registered event template (default templates are not affected) |
| [`validate`](./validate) | Validate one or all event templates |
| [`which`](./which) | Print the resolved file path for a template ID |

---

Every command also accepts the [global options](../../global-options.md).
