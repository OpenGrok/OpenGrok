# Table of contents

<!-- toc -->

- [Using project groups](#using-opengrok-groupings)
  * [Compatibility](#compatibility)
  * [Simple configuration](#simple-configuration)
  * [Display](#display)
    + [Listing](#listing)
    + [Select Box](#select-box)
  * [Group class](#group-class)
    + [Method summary](#method-summary)
    + [Property summary](#property-summary)
  * [Tools](#tools)
    + [Usage](#usage)
    + [Example](#example)
  * [Other sample configuration](#other-sample-configuration)

<!-- tocstop -->

# Using OpenGrok groupings

This is a howto to introduce new feature in OpenGrok which allows
you to group projects into groups by using regexps and display them on the
index page.

## Compatibility

If no configuration about groups is provided then groups are not used. Instead there is a 
list of repositories on the main page and list of repositories and projects in the select box (as it used to be).

## Simple configuration

Suppose we have couple of projects:
- ctags-5.6
- ctags-5.6-stable
- ctags-5.7
- ctags-5.8
- apache-ant-1.9
- opengrok-master
- opengrok-0.12-stable

The new feature allows us to create a structure like this (example):

```
ctags
| -- ctags 5.6
|	 | -- ctags-5.6
|	 | -- ctags-5.6-stable
| -- ctags 5.7
|	 | -- ctags-5.7
| -- ctags 5.8
|	 | -- ctags-5.8
apache
| -- apache-ant-1.9
opengrok
| -- opengrok-master
| -- opengrok-0.12-stable
```

and display that structure on the main page below the search form.

Configuration for this structure could look like this:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<java version="1.8.0_65" class="java.beans.XMLDecoder">
 <object class="org.opengrok.indexer.configuration.Configuration" id="Configuration0">
    <void method="addGroup">
        <object class="org.opengrok.indexer.configuration.Group">
            <void property="name">
                <string>ctags</string>
            </void>
            <void property="pattern">
                <string></string>
            </void>
            <void method="addGroup">
                <object class="org.opengrok.indexer.configuration.Group">
                    <void property="name">
                        <string>ctags 5.6</string>
                    </void>
                    <void property="pattern">
                        <string>ctags 5.6.*</string>
                    </void>
                </object>
            </void>
            <void method="addGroup">
                <object class="org.opengrok.indexer.configuration.Group">
                    <void property="name">
                        <string>ctags 5.7</string>
                    </void>
                    <void property="pattern">
                        <string>ctags 5.7</string>
                    </void>
                </object>
            </void>
            <void method="addGroup">
                <object class="org.opengrok.indexer.configuration.Group">
                    <void property="name">
                        <string>ctags 5.8</string>
                    </void>
                    <void property="pattern">
                        <string>ctags 5.8</string>
                    </void>
                </object>
            </void>
        </object>
    </void>
    <void method="addGroup">
        <object class="org.opengrok.indexer.configuration.Group">
            <void property="name">
                <string>apache</string>
            </void>
            <void property="pattern">
                <string>apache-.*</string>
            </void>
        </object>
    </void>
    <void method="addGroup">
        <object class="org.opengrok.indexer.configuration.Group">
            <void property="name">
                <string>opengrok</string>
            </void>
            <void property="pattern">
                <string>opengrok.*</string>
            </void>
        </object>
    </void>
 </object>
</java>
```

Configuration is provided in the read only configuration (`-R` parameter of indexer). This configuration is merged by the indexer with the instance configuration in `/var/opengrok/etc/configuration.xml` (default) so the groups are available for the webapp.

**NOTE:**
There is no other way to persistently provide the group configuration than the `-R` option for each run of the indexer. If you run the indexer 
without the read only configuration, your groups will vanish from the `configuration.xml`.
For more information see [#1065](https://github.com/OpenGrok/OpenGrok/issues/1065). More about this process how the get the persistent group configuration is in the [Advanced Configuration](https://github.com/OpenGrok/OpenGrok/wiki/OpenGrok-Advanced-Configuration)


## Display

Groups, projects and repositories should be displayed in alphabetical order.

### Listing

Groups are displayed in accordion on the main page, accordion can be expanded to see all subgroups and repositories
in that group. It is necessary to mention that in the accordion there are only repositories, i. e. projects with 
repository information.

If no group matches the project the special section 'Other' is maintained with all such projects and repositories.

All favourite groups are expanded. A group is considered as favourite when one of its projects is considered favourite.  Favourite project is a project which is contained in the `OpenGrokProject` cookie (see https://github.com/oracle/opengrok/wiki/Cookies), i. e. it has been searched or viewed by the user recently. This applies also for subgroups of the given group.

There is a tunable threshold to control if other than favourite groups should be expanded by default. The name of the configuration property is `groupsCollapseThreshold` and it controls the maximum number of projects contained in the group (without traversing the subgroups) when the group should be expanded or not. This threshold is by default equal to 4.

### Select Box

Using `<optgroup>` in select box displays all groups with their projects and repositories in the select box. 
Special section 'Other' is also included for non-matched projects. Double click on the project leads to its xref
as usual (not working in IE) and double click in group name selects all projects in that group.

## Group class

### Method summary

- `Configuration.addGroup(Group group)`

This method adds a group to the set of groups in the configuration. If the group name is not
unique accross the set, it throws an IOException and does not add that group.

- `Group.addGroup(Group group)`

This method adds a group to the other group as its subgroup. It correctly sets the parent group
so that the whole structure can be traversed.

### Property summary

- `String Group::name` - displayed group name

- `String Group::pattern` - regexp which is tested for each project.
                            empty pattern implies that no project matches this group thus
                            it can be used as a superior group without repeating the projects
                            from subgroups


## Tools

There is `groups.py` script which is suitable for creating the group structure and for easy manipulation with the group tree.

The script has documented usage and all subcommands have their own usage.

### Example

This example is taken from the authorization tutorial - [setting up the groups](https://github.com/OpenGrok/OpenGrok/wiki/OpenGrok-Authorization#setting-up-the-groupings)

```
$ ./tools/Groups empty > "$OPENGROK_READ_XML_CONFIGURATION"
$ ./tools/Groups add admins  "test-project-1|test-project-2|test-project-3|test-project-4" -u
$ ./tools/Groups add users   "test-project-5|test-project-6|test-project-7|test-project-8" -u
$ ./tools/Groups add plugins "test-project-9|test-project-10" -p users -u
```

The group names correspond to the roles defined in `tomcat-users.xml` earlier.
The final group structure should look like this now:
```
$ ./tools/Groups list
admins ~ "test-project-1|test-project-2|test-project-3|test-project-4"
users ~ "test-project-5|test-project-6|test-project-7|test-project-8"
    plugins ~ "test-project-9|test-project-10"
```

**The script has also an option to update the configuration IN PLACE. That should be used carefully because it can lead to loss of your data.**

## Other sample configuration

It is possible to include the same project in multiple groups. We could create also a structure like this:

```
ctags
| -- ctags 5.6
|	 | -- ctags-5.6
|	 | -- ctags-5.6-stable
| -- ctags 5.7
|	 | -- ctags-5.7
| -- ctags 5.8
|	 | -- ctags-5.8
apache
| -- apache-ant-1.9
opengrok
| -- opengrok-master
| -- opengrok-0.12-stable
java
| -- apache-ant-1.9
| -- opengrok-master
| -- opengrok-0.12-stable
C language
| -- ctags-5.6
| -- ctags-5.6-stable
| -- ctags-5.7
| -- ctags-5.8
```

with configuration like this:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<java version="1.8.0_65" class="java.beans.XMLDecoder">
 <object class="org.opengrok.indexer.configuration.Configuration" id="Configuration0">
    <void method="addGroup">
        <object class="org.opengrok.indexer.configuration.Group">
            <void property="name">
                <string>ctags</string>
            </void>
            <void property="pattern">
                <string></string>
            </void>
            <void method="addGroup">
                <object class="org.opengrok.indexer.configuration.Group">
                    <void property="name">
                        <string>ctags 5.6</string>
                    </void>
                    <void property="pattern">
                        <string>ctags 5.6.*</string>
                    </void>
                </object>
            </void>
            <void method="addGroup">
                <object class="org.opengrok.indexer.configuration.Group">
                    <void property="name">
                        <string>ctags 5.7</string>
                    </void>
                    <void property="pattern">
                        <string>ctags 5.7</string>
                    </void>
                </object>
            </void>
            <void method="addGroup">
                <object class="org.indexer.opengrok.configuration.Group">
                    <void property="name">
                        <string>ctags 5.8</string>
                    </void>
                    <void property="pattern">
                        <string>ctags 5.8</string>
                    </void>
                </object>
            </void>
        </object>
    </void>
    <void method="addGroup">
        <object class="org.opengrok.indexer.configuration.Group">
            <void property="name">
                <string>apache</string>
            </void>
            <void property="pattern">
                <string>apache-.*</string>
            </void>
        </object>
    </void>
    <void method="addGroup">
        <object class="org.opengrok.indexer.configuration.Group">
            <void property="name">
                <string>opengrok</string>
            </void>
            <void property="pattern">
                <string>opengrok.*</string>
            </void>
        </object>
    </void>
    <void method="addGroup">
        <object class="org.opengrok.indexer.configuration.Group">
            <void property="name">
                <string>java</string>
            </void>
            <void property="pattern">
                <string>opengrok.*|apache-.*</string>
            </void>
        </object>
    </void>
    <void method="addGroup">
        <object class="org.opengrok.indexer.configuration.Group">
            <void property="name">
                <string>C language</string>
            </void>
            <void property="pattern">
                <string>ctags.*</string>
            </void>
        </object>
    </void>
 </object>
</java>
```