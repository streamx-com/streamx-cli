---
title: Getting started
sidebar_label: Getting started
sidebar_position: 1
slug: /
---

# StreamX CLI

`streamx` drives StreamX from the terminal: authenticate, manage organizations and projects,
connect git repositories, and run a mesh locally.

## Install

```bash
brew install streamx-com/tap/streamx
```

Verify the install and see where the CLI is reading its configuration from:

```bash
streamx info
```

## Contexts

A **context** bundles everything that belongs to one environment - endpoints, settings, event
templates and your login - under `~/.streamx/contexts/<name>/`. Several contexts can exist side
by side, so a production login never mixes with a development one.

```bash
streamx context create dev
streamx context configure     # endpoints, then log in, then pick org and project
```

Selection precedence, highest first:

1. `--context` / `-C` on the command line
2. the `STREAMX_CONTEXT` environment variable
3. the `current-context` file, set by `streamx context use`
4. `default`

## Log in

```bash
streamx auth login
```

This opens a browser and completes the login there. Over SSH, or with `--no-browser`, the CLI
prints a code to enter on another device instead. Tokens are stored per context and refreshed
automatically.

```bash
streamx auth whoami
```

## Work with organizations and projects

Most commands act on an organization and a project. Set them once on the context and omit the
flags afterwards:

```bash
streamx context org use acme
streamx context project use website

streamx org list
streamx project list
streamx project get website
```

An explicit `--org` / `--project` always wins, and `STREAMX_ORG` / `STREAMX_PROJECT` override the
context pointers - useful in CI.

## Automation with personal access tokens

Scripts cannot open a browser, so mint a long-lived token instead:

```bash
streamx auth token create ci-github-actions
```

The token is printed **once**. Store it as a secret and pass it back through the environment:

```bash
export STREAMX_PLATFORM_TOKEN=sxp_v1_...
streamx org list
```

A token acts as the user who created it. It cannot create or revoke tokens, and it cannot change
that user's account - those actions require an interactive login. Revoke a token when the job that
used it is retired:

```bash
streamx auth token list
streamx auth token revoke <id>
```

## Where to next

- [Command reference](./commands/) - every command, generated from the CLI itself
- [Global options](./commands/global-options) - flags accepted everywhere
