While by itself OpenGrok does not provide a way how to synchronize repositories (yet, there are some stubs for that in the Repository handling code) it is shipped with a set of Python scripts that make it easy to both synchronize and reindex.

These scripts assume that OpenGrok is setup with projects. See https://github.com/OpenGrok/OpenGrok/wiki/Per-project-management for more details on per-project management.

There are 2 main scripts:
  - `sync.py` - provides a way how to run a sequence of commands for a set of projects (in parallel).
  - `mirror.py` - performs synchronization of given Source Code Management repository with its upstream.

In the source these scripts live under the [tools/sync](https://github.com/OpenGrok/OpenGrok/tree/master/tools/sync) directory.

Both scripts take configuration either in JSON or YAML.

# sync.py

Use e.g. like this:

```bash
  $ sync.py -c /scripts/sync.conf -d /ws-local/ -p
```

where the `sync.conf` file contents might look like this:

```json
  {
     "commands": [["/usr/opengrok/bin/Messages", "-c", "info", "-e", "+1 hour",
                   "-n", "normal", "-t", "ARG", "resync + reindex in progress"],
                  ["sudo", "-u", "wsmirror", "/usr/opengrok/bin/mirror.py",
                    "-c", "/opengrok/etc/mirror-config.yml", "-b",
                    "--messages", "/usr/opengrok/bin/Messages"],
                  ["sudo", "-u", "webservd", "/usr/opengrok/bin/reindex-project.ksh",
                   "/opengrok/etc/opengrok.conf", "/usr/opengrok/bin"],
                  ["/usr/opengrok/bin/Messages", "-n", "abort", "-t"],
                  ["/scripts/check-indexer-logs.ksh"]],
     "ignore_errors": ["NetBSD-current", "linux-mainline-next"],
     "cleanup": ["/usr/opengrok/bin/Messages", "-n", "abort", "-t"]
  }
```

The above `sync.py` command will basically take all directories under `/ws-local` and for each it will run the sequence of commands specified in the `sync.conf` file. This will be done in parallel - on project level. The level of parallelism can be specified using the the `--workers` option (by default it will use as many workers as there are CPUs in the system).

Another variant of how to specify the list of projects to be synchronized is to use the `--indexed` option of `sync.py` that will query the webapp configuration for list of indexed projects and will use that list. Otherwise, the `--projects` option can be specified to process just specified projects.

The commands above will basically:
  - mark the project with alert (to let the users know it is being synchronized/indexed) using the first `Messages` command
  - pull the changes from all the upstream repositories that belong to the project using the `mirror.py` command
  - reindex the project using `reindex-project.ksh`
  - clear the alert using the second `Messages` command
  - execute the `/scripts/check-indexer-logs.ksh` script to perform some pattern matching in the indexer logs to see if there were any serious failures there

The `sync.py` script will print any errors to the console and uses file level locking to provide exclusivity of run so it is handy to run from `crontab` periodically.

If any of the commands in `"commands"` fail, the `"cleanup"` command will be run. This is handy in this case since the first `Messages` command will mark the project with alert in the WEB UI so if any of the commands that follow fails, the cleanup `Messages` command will be run to clear the alert.

Some project can be notorious for producing spurious errors so their errors are ignored via the `"ignore_errors"` section.

The `sync.conf` configuration can be also represented as YAML.

In the above example it is assumed that `sync.py` is run as `root` and synchronization and reindexing are done under different users. This is done so that the web application cannot tamper with source code even if compromised.

The commands got appended project name unless one of their arguments is equal
to 'ARG', in which case it is substituted with project name and no append is
done.

For per-project reindexing to work properly, `reindex-project.ksh` uses
the `logging.properties.template` to make sure each project has its own
log directory.

# mirror.py

The script synchronized the repositories of projects by running appropriate commands (e.g. `git pull` for Git). While it can run perfectly fine standalone, it is meant to be run from within `sync.py` (see above).

## Configuration example

The configuration file contents in YML can look e.g. like this:

```YML
#
# Commands (or paths - for specific repository types only)
#
commands:
  hg: /usr/bin/hg
  svn: /usr/bin/svn
  teamware: /ontools/onnv-tools-i386/teamware/bin
#
# The proxy environment variables will be set for a project's repositories
# if the 'proxy' property is True.
#
proxy:
  http_proxy: proxy.example.com:80
  https_proxy: proxy.example.com:80
  ftp_proxy: proxy.example.com:80
  no_proxy: example.com,foo.example.com
hookdir: /tmp/hooks
# per-project hooks relative to 'hookdir' above
logdir: /tmp/logs
command_timeout: 300
hook_timeout: 1200
#
# Per project configuration.
#
projects:
  http:
    proxy: true
  history:
    disabled: true
  userland:
    proxy: true
    hook_timeout: 3600
    hooks:
      pre: userland-pre.ksh
      post: userland-post.ksh
  opengrok-master:
    ignored_repos:
      - /opengrok-master/testdata/repositories/rcs_test
  jdk.*:
    proxy: true
    hooks:
      post: jdk_post.sh
```

In the above config, the `userland` project will be run with environment variables in the `proxy` section, plus it will also run scripts specified in the `hook` section before and after all its repositories are synchronized. The hook scripts will be run with the current working directory set to that of the project.

The `opengrok-master` project contains a RCS repository that would make the mirroring fail (since `mirror.py` does not support RCS yet) so it is marked as ignored.

## Project matching

Multiple projects can share the same configuration using regular expressions as demonstrated with the `jdk.*` pattern in the above configuration. The patterns are matched from top to the bottom of the configuration file, first match wins.

## Disabling project mirroring

The `history` project is marked as disabled. This means that the `mirror.py` script will exit with special value of 2 that is interpreted by the `sync.py` script to avoid any reindex. It is not treated as an error.

## Batch mode

In batch mode, messages will be logged to a log file under the `logdir` directory specified in the configuration and rotated for each run, up to default count (8) or count specified using the `--backupcount` option.

## Hooks

If pre and post mirroring hooks are specified, they are run before and after project synchronization. If any of the hooks fail, the program is immediately terminated. However, if the synchronization (that is run in between the hook scripts) fails, the post hook will be executed anyway. This is done so that the project is in sane state - usually the post hook which is used to apply extract source archives and apply patches. If the pre hook is used to clean up the extracted work and project synchronization failed, the project would be left barebone.

## Timeouts

Both repository synchronization commands and hooks can have a timeout. By default there is no timeout, unless specified in the configuration file. There are global and per project timeouts, the latter overriding the former. For instance, in the above configuration file, the `userland` project overrides global hook timeout to 1 hour while inheriting the command timeout.