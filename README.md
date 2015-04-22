### Code History Mining IntelliJ Plugin

This is a plugin for [IntelliJ](https://github.com/JetBrains/intellij-community) IDEs to visualize project source code history.
Analysis is based on file and therefore programming language-agnostic.
You can install it from IDE Settings -> Plugins or download from [plugin repository](http://plugins.jetbrains.com/plugin/7273).

Some examples of code history visualizations:
[JUnit](http://dkandalov.github.io/code-history-mining/JUnit.html),
[TestNG](http://dkandalov.github.io/code-history-mining/TestNG.html),
[Cucumber](http://dkandalov.github.io/code-history-mining/Cucumber.html),
[Scala](http://dkandalov.github.io/code-history-mining/Scala.html),
[Clojure](http://dkandalov.github.io/code-history-mining/Clojure.html),
[Kotlin](http://dkandalov.github.io/code-history-mining/Kotlin.html),
[Groovy](http://dkandalov.github.io/code-history-mining/Groovy.html),
[CoffeeScript](http://dkandalov.github.io/code-history-mining/CoffeeScript.html),
[Go](http://dkandalov.github.io/code-history-mining/Go.html),
[Erlang](http://dkandalov.github.io/code-history-mining/Erlang.html),
[Maven](http://dkandalov.github.io/code-history-mining/Maven.html),
[Gradle](http://dkandalov.github.io/code-history-mining/Gradle.html),
[Ruby](http://dkandalov.github.io/code-history-mining/Ruby.html),
[Ruby on Rails](http://dkandalov.github.io/code-history-mining/Rails.html),
[Node.js](http://dkandalov.github.io/code-history-mining/NodeJS.html),
[GWT](http://dkandalov.github.io/code-history-mining/GWT.html),
[jQuery](http://dkandalov.github.io/code-history-mining/jQuery.html),
[Bootstrap](http://dkandalov.github.io/code-history-mining/Bootstrap.html),
[Aeron](http://dkandalov.github.io/code-history-mining/Aeron.html),
[GHC](http://dkandalov.github.io/code-history-mining/GHC.html),
[IntelliJ](http://dkandalov.github.io/code-history-mining/IntelliJ.html)
.
Csv files with VCS data for the above visualizations
are available [on google drive](https://googledrive.com/host/0B5PfR1lF8o5SZE1xMXZIWGxBVzQ).

See also [command line version of code-history-mining](http://dkandalov.github.io/code-history-mining-cli).


### Why?
There is a lot of interesting data captured in version control systems, yet we rarely look into it.
This is an attempt to make analysis of project code history easy enough so that it can be done regularly.

See also [Your Code as a Crime Scene book](https://pragprog.com/book/atcrime/your-code-as-a-crime-scene).


### How to use?
 - **grab project history from version control into csv file** -
 Grab Project History action will use VCS roots configured in current project for checked out VCS branches.
 The main reason for separate grabbing step is that code history often contains some noise (e.g. automatically updated build system files).
 Having code history in csv file should make it easier to process it with some scripts before visualization.
 - **visualize code history from csv file** -
 at this step code history is consumed from csv file and visualized in browser.
 All visualizations are self-contained one file html pages so that they can be saved and shared without external dependencies.

#### Grab VCS history
Use "Main menu -> VCS -> Code History Mining" or "alt+shift+H".

You should see this window:
<img src="https://raw.github.com/dkandalov/code-history-mining/master/grab-history-screenshot.png" alt="screenshot" title="screenshot" align="center"/>
 - **From/To** - desired date range to be grabbed from VCS. Commits are loaded from version control only if they are not already in csv file.
 - **Save to** - csv file to save history to.
 - **Grab history on VCS update** - grab history on update from VCS (but not more often than once a day).
 This is useful to grab history in small chunks so that when you run visualization grabbed history is already up-to-date.
 - **Grab change size in lines/characters and amount of TODOs** - grab amount of lines and characters before/after commit and size of change.
 This is used by some of visualizations and is optional.
 Note that it requires loading file content and can slow down grabbing history and IDE responsiveness.

#### Visualize
By default cvs files with history are saved to "[\<plugins folder\>](http://devnet.jetbrains.com/docs/DOC-181)/code-history-mining" folder.
Files from this folder are displayed in plugin menu.
Each csv file will have sub-menu with visualizations:

<img src="https://raw.github.com/dkandalov/code-history-mining/master/popup-screenshot.png" alt="screenshot" title="screenshot" align="center"/>

When opened in browser visualizations will have help button with short description,
e.g. see visualizations for [JUnit](http://dkandalov.github.io/code-history-mining/JUnit.html).


### Misc notes
 - any VCS supported by IntelliJ should work (tested with svn/git/hg)
 - merged commits are grabbed with date and author of the original commit, merge commit itself is skipped
 - visualisations use SVG and require browser with SVG support (any not outdated browser)
 - some of visualisations might be slow for long history of a big project
 (e.g. building treemap view of commits for project with 1M LOC for 10 years might take forever).
 In this case, filtering or splitting history into smaller chunks can help.


### Code history csv format
Each commit is broken down into several lines. One line corresponds to one file changed in commit.
Commits are stored ordered by time from present to past.
For example two commits from JUnit csv:
```
2001-10-02 20:38:22 +0100,0bb3dfe2939cc214ee5e77556a48d4aea9c6396a,kbeck,,IMoney.java,,/junit/samples/money,MODIFICATION,Cleaning up MoneyBag construction,38,42,4,0,0,817,888,71,0,0,0,0
2001-10-02 20:38:22 +0100,0bb3dfe2939cc214ee5e77556a48d4aea9c6396a,kbeck,,Money.java,,/junit/samples/money,MODIFICATION,Cleaning up MoneyBag construction,70,73,3,1,0,1595,1684,86,32,0,0,0
2001-10-02 20:38:22 +0100,0bb3dfe2939cc214ee5e77556a48d4aea9c6396a,kbeck,,MoneyBag.java,,/junit/samples/money,MODIFICATION,Cleaning up MoneyBag construction,140,131,8,4,23,3721,3594,214,154,511,0,0
2001-10-02 20:38:22 +0100,0bb3dfe2939cc214ee5e77556a48d4aea9c6396a,kbeck,,MoneyTest.java,,/junit/samples/money,MODIFICATION,Cleaning up MoneyBag construction,156,141,0,34,0,5187,4785,0,1594,0,0,0
2001-07-09 23:51:53 +0100,ce0bb8f59ea7de1ac3bb4f678f7ddf84fe9388ed,egamma,,.classpath,,,NEW,added .classpath for eclipse,0,6,6,0,0,0,240,240,0,0,0,0
2001-07-09 23:51:53 +0100,ce0bb8f59ea7de1ac3bb4f678f7ddf84fe9388ed,egamma,,.vcm_meta,,,MODIFICATION,added .classpath for eclipse,6,7,1,0,0,199,221,21,0,0,0,0
```
Columns:
 - __revisionDate__ - in "yyyy-MM-dd HH:mm:ss Z" format with local timezone (see [javadoc](http://docs.oracle.com/javase/7/docs/api/java/text/SimpleDateFormat.html) for details).
 - __revision__ - unique commit id, format depends on VCS.
 - __author__ - committer name from VCS.
 - __fileNameBefore__ - file name before change, empty if file was added or name didn't change.
 - __fileName__ - file name after change, empty if file was deleted.
 - __packageNameBefore__ - file path before change, empty if file was added, path didn't change or file is in root folder.
 - __packageName__ - file path after change, empty if files was deleted or is in root folder.
 - __fileChangeType__ - "NEW", "MODIFICATION", "MOVED" or "DELETED". Renamed or moved files are "MOVED" even if file content has changed.
 - __commitMessage__ - commit message, new line breaks are replaced with "\\n".
 - __linesBefore__ - number of lines in file before change;
     "-1" if file is binary or "Grab change size" checkbox is not selected in "Grab Project History" dialog;
     "-2" if file is too big for IntelliJ to diff.
 - __linesAfter__ - similar to the above.
 - __<rest of the columns>__ - similar to the above.

Output csv format should be compatible with [RFC4180](http://www.apps.ietf.org/rfc/rfc4180.html).


### Acknowledgments
 - inspired by Michael Feathers [workshop](http://codehistorymining.eventbrite.co.uk/)
 and [Delta Flora](https://github.com/michaelfeathers/delta-flora) project.
 - all visualizations are based on awesome [d3.js examples](https://github.com/mbostock/d3/wiki/Gallery).


### Similar projects
 - https://github.com/adamtornhill/code-maat for any language
 - https://github.com/michaelfeathers/delta-flora for Ruby (with commit breakdown to method level)
