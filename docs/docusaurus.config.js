// @ts-check
// Docs site for the StreamX CLI. The command reference under docs/commands is generated -
// see README.md; do not hand-edit those files.

/** @type {import('@docusaurus/types').Config} */
const config = {
  title: 'StreamX CLI',
  tagline: 'Command reference and guides',

  // Defaults are for streamx.com, where the site lives at /cli/. CI overrides both for GitHub
  // Pages, where every version is served from its own sub-directory - see
  // .github/workflows/gen-docs-reference.yml.
  url: process.env.DOCS_URL || 'https://streamx.com',
  baseUrl: process.env.DOCS_BASE_URL || '/cli/',
  organizationName: 'streamx-com',
  projectName: 'streamx-cli',

  favicon: 'img/favicon.svg',

  onBrokenLinks: 'warn',
  markdown: {hooks: {onBrokenMarkdownLinks: 'warn'}},

  i18n: {defaultLocale: 'en', locales: ['en']},

  presets: [
    [
      'classic',
      /** @type {import('@docusaurus/preset-classic').Options} */
      ({
        docs: {
          routeBasePath: '/',
          sidebarPath: require.resolve('./sidebars.js'),
          editUrl: 'https://github.com/streamx-com/streamx-cli/tree/main/docs/',
        },
        blog: false,
        theme: {customCss: require.resolve('./src/css/custom.css')},
      }),
    ],
  ],

  themeConfig:
    /** @type {import('@docusaurus/preset-classic').ThemeConfig} */
    ({
      colorMode: {
        defaultMode: 'dark',
        respectPrefersColorScheme: false,
      },
      navbar: {
        title: 'CLI',
        logo: {
          alt: 'StreamX',
          // Docusaurus swaps these with the colour mode: `src` on light, `srcDark` on dark.
          src: 'img/streamx-logo-light-bg.svg',
          srcDark: 'img/streamx-logo-dark-bg.svg',
          href: 'https://www.streamx.com',
          target: '_self',
          height: 28,
        },
        items: [
          {type: 'docSidebar', sidebarId: 'docs', position: 'left', label: 'Docs'},
          {to: '/commands/', label: 'Commands', position: 'left'},
          // Index of all published versions; only exists on the multi-version (GitHub Pages) deployment.
          ...(process.env.DOCS_VERSIONS_URL
            ? [{href: process.env.DOCS_VERSIONS_URL, label: 'Versions', position: 'right', target: '_self'}]
            : []),
          {href: 'https://www.streamx.com', label: 'streamx.com', position: 'right'},
          {
            href: 'https://github.com/streamx-com/streamx-cli',
            label: 'GitHub',
            position: 'right',
          },
        ],
      },
      footer: {
        style: 'dark',
        links: [
          {
            title: 'Docs',
            items: [
              {label: 'Getting started', to: '/'},
              {label: 'Command reference', to: '/commands/'},
              {label: 'Global options', to: '/commands/global-options'},
            ],
          },
          {
            title: 'StreamX',
            items: [
              {label: 'streamx.com', href: 'https://www.streamx.com'},
              {label: 'GitHub', href: 'https://github.com/streamx-com/streamx-cli'},
            ],
          },
        ],
        copyright: `Copyright © ${new Date().getFullYear()} StreamX`,
      },
      prism: {
        additionalLanguages: ['bash', 'json', 'yaml'],
      },
    }),
};

module.exports = config;
