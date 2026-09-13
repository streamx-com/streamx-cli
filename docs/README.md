# StreamX CLI docs

Docusaurus site for the CLI. Theme follows streamx.com: background `#0a0a0b`, text `#ecf5ff`,
surfaces `#232323`, accent `#7714ff`, type "Be Vietnam Pro" - see `src/css/custom.css`.

## Layout

    docs/
      intro.md              hand-written
      commands/             GENERATED, git-ignored - do not edit

## Generating the command reference

The reference is produced from the live picocli command tree, so it can never drift from the
CLI's actual behaviour. It is written by a hidden command:

```bash
mvn -q package -DskipTests
java -jar target/streamx-cli-*-runner.jar __generate-docs docs/docs/commands
```

or, from this directory, `npm run generate`. The output is not committed: CI generates it on every
build, and locally you run it once before `npm start` (and again after changing a command, option,
argument or help text).

What the generator emits, per command:

- front matter (`title`, `sidebar_label`, `sidebar_position`, `description`)
- the synopsis
- subcommand table with links, for groups
- arguments table, with a required column
- options table, listing only the options specific to that command

Options accepted by *every* command are collected once into `commands/global-options.md`;
each page links to it rather than repeating them. That set is derived as the intersection of the
options across all leaf commands, so it stays correct as commands come and go. Options that most
- but not all - commands share (`--output`, `--verbose`) stay on the individual pages, because
claiming they work everywhere would be wrong.

Hidden commands (`__complete-*`, `__generate-docs`) are skipped.

## Running the site

```bash
npm install
npm run generate   # writes docs/commands from the CLI
npm start          # dev server with live reload
npm run build      # static build into build/
```

## Publishing

`.github/workflows/gen-docs-reference.yml` builds the site and publishes it to GitHub Pages at
`https://<owner>.github.io/<repo>/`, one sub-directory per version, with an index page at the root:

    main/       every push to main
    <tag>/      every release tag (X.Y.Z and X.Y.Z-rc.*), kept forever
    latest/     the highest X.Y.Z release
    pr-<n>/     every pull request, removed when it is closed

The published files live in the `gh-pages` branch (Pages source: "Deploy from a branch",
`gh-pages`, `/`); each run replaces only its own sub-directory (`.github/scripts/deploy-docs.sh`).
`DOCS_URL`, `DOCS_BASE_URL` and `DOCS_VERSIONS_URL` in `docusaurus.config.js` are set per version by
the workflow. Pull requests from forks are not published: the Maven build needs the private
registry credentials that fork PRs do not get.
