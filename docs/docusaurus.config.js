// @ts-check
// Docs site for the StreamX CLI. The command reference under docs/commands is generated -
// see README.md; do not hand-edit those files.

/** @type {import('@docusaurus/types').Config} */
const config = {
  title: 'StreamX CLI',
  tagline: 'Command reference and guides',

  url: 'https://streamx.com',
  baseUrl: '/cli/',
  organizationName: 'streamx-com',
  projectName: 'streamx-cli',

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
        title: 'StreamX CLI',
        items: [
          {type: 'docSidebar', sidebarId: 'docs', position: 'left', label: 'Docs'},
          {to: '/commands/', label: 'Commands', position: 'left'},
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
