---
title: Getting started
sidebar_label: Getting started
sidebar_position: 1
slug: /
---

# StreamX CLI

`streamx` drives StreamX from the terminal: run a mesh locally, publish events into it, and
manage the CLI's settings and event templates.

## Install

```bash
brew install streamx-com/tap/streamx
```

## Run a mesh locally

Start the mesh described by a definition file:

```bash
streamx local run -f mesh.yaml
```

## Publish events

A single event, from a payload file and an event template:

```bash
streamx publish event <templateId> payload.json
```

Many events at once from a directory structure with `.eventtemplate` files, or a continuous
stream:

```bash
streamx publish events ./events
streamx publish stream ./source
```

## Settings and event templates

CLI settings and event templates live under `~/.streamx` (or the directory given with
`--streamx-home` / `STREAMX_HOME`):

```bash
streamx settings list
streamx settings event-templates list
```

## Where to next

- [Command reference](./commands/) - every command, generated from the CLI itself
- [Global options](./commands/global-options) - flags accepted everywhere
