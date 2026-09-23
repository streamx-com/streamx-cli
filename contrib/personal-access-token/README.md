# Creating a PAT without having dashboard UI for managing PATs or streamx auth login command

⚠️TODO - remove after both UI and `streamx auth login` will be implemented.

- Point the CLI at the platform: `streamx settings set streamx.platform.url http://localhost:8080`.
- Log in to the dashboard in your browser, e.g. `http://localhost:8080`.
- Open the developer tools, Network tab, right-click any request to the dashboard and
  copy the Cookie header to a file, e.g.: `/tmp/streamx-cookie`.
- Create the token and use it:

   ```bash
   export STREAMX_PLATFORM_TOKEN=$(./create-pat.sh /tmp/streamx-cookie)
   streamx org list
   ```

   The script prints the token and takes the platform URL from `streamx settings get
   streamx.platform.url`; pass another URL as a second argument.
