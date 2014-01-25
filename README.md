### What is this?

This is a plugin for [IntelliJ](https://github.com/JetBrains/intellij-community) IDEs to grab
and analyze project source code history. It's probably better to understand by looking at example:
 - [JUnit code history](http://dkandalov.github.io/code-history-mining/junit.html)
 - [IntelliJ code history](http://dkandalov.github.io/code-history-mining/idea.html)
 - [Scala code history](http://dkandalov.github.io/code-history-mining/scala.html)
 - [Clojure code history](http://dkandalov.github.io/code-history-mining/clojure.html)
 - [Ruby code history](http://dkandalov.github.io/code-history-mining/ruby.html)
 - [Ruby on Rails code history](http://dkandalov.github.io/code-history-mining/rails.html)
 - [Node.js code history](http://dkandalov.github.io/code-history-mining/nodejs.html)
 - [GHC code history](http://dkandalov.github.io/code-history-mining/ghc.html)

The plugin has two parts:
 - reading project history from version control and saving it as csv file
 - reading history from csv, analyzing and visualizing it with [d3.js](http://d3js.org/) (needs a browser with SVG support)


### Why?
There seems to be a lot of interesting data captured in version control systems, yet we don't use it that much.
This is an attempt to make looking at project history easier.

 - converting history to csv is useful because it's easy to read and process in any language (or even in a spreadsheet)
 - interactive visualization is cool because it's fun to play with and can hopefully give deeper insight into project history


### How to install
From IntelliJ plugin repository http://plugins.jetbrains.com/plugin/7273


### Grabbing VCS history into csv
Main menu -> Tools -> Code History Mining or alt + shift + H.
<img src="https://raw.github.com/dkandalov/code-history-mining/master/grab-history-screenshot.png" alt="screenshot" title="screenshot" align="center"/>
 - **From/To** - desired dates for project history. Commits are loaded from version control only if they are not already in csv file.
 - **Save to** - file to save history to.
 - **Grab change size in lines/characters** - grabs amount of lines and characters before/after commit and size of change.
 Please note that this requires loading file content and can slow down grabbing history.

### Visualizations
Files in "<[plugins folder](http://devnet.jetbrains.com/docs/DOC-181)>/code-history-mining" are displayed in plugin drop-down menu.
After you choose one plugin reads csv files and build visualisation
All visualizations are self-contained one file pages (i.e. you should be able to save them as standalone file).
<img src="https://raw.github.com/dkandalov/code-history-mining/master/popup_screenshot.png" alt="screenshot" title="screenshot" align="center"/>

### Limitations
 - all VCS supported by IntelliJ should work but I only tried Git and Svn
 - visualisations are only tested Chrome and Safari; they work in Firefox but might have minor glitches; mostly broken in IE9
 - some of visulisations are too slow for long history of a big project.
 E.g. building treemap view of commits for project with 1M LOC for 10 years will take forever.


### Code history csv format
Each commit is broken down into several lines. One line corresponds to one file changed in commit.
Commits are stored ordered by time from present to past as they are read from VCS
(although there might be exceptions because VCSs don't guarantee this order).
```
2001-10-02 20:38:22 +0100,0bb3dfe2939cc214ee5e77556a48d4aea9c6396a,kbeck <kbeck>,,IMoney.java,,/junit/samples/money,MODIFICATION,38,42,4,0,0,817,888,71,0,0,Cleaning up MoneyBag construction
2001-10-02 20:38:22 +0100,0bb3dfe2939cc214ee5e77556a48d4aea9c6396a,kbeck <kbeck>,,Money.java,,/junit/samples/money,MODIFICATION,70,73,3,1,0,1595,1684,86,32,0,Cleaning up MoneyBag construction
2001-10-02 20:38:22 +0100,0bb3dfe2939cc214ee5e77556a48d4aea9c6396a,kbeck <kbeck>,,MoneyBag.java,,/junit/samples/money,MODIFICATION,140,131,8,4,23,3721,3594,214,154,511,Cleaning up MoneyBag construction
2001-10-02 20:38:22 +0100,0bb3dfe2939cc214ee5e77556a48d4aea9c6396a,kbeck <kbeck>,,MoneyTest.java,,/junit/samples/money,MODIFICATION,156,141,0,34,0,5187,4785,0,1594,0,Cleaning up MoneyBag construction
2001-07-09 23:51:53 +0100,ce0bb8f59ea7de1ac3bb4f678f7ddf84fe9388ed,egamma <egamma>,,.classpath,,,NEW,0,6,6,0,0,0,241,241,0,0,added .classpath for eclipse
2001-07-09 23:51:53 +0100,ce0bb8f59ea7de1ac3bb4f678f7ddf84fe9388ed,egamma <egamma>,,.vcm_meta,,,MODIFICATION,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,added .classpath for eclipse
```
Columns:
 - __commit date__ - in "yyyy-MM-dd HH:mm:ss Z" format (see [javadoc](http://docs.oracle.com/javase/7/docs/api/java/text/SimpleDateFormat.html) for details).
 - __revision__ - format depends on underlying VCS.
 - __author__ - committer name from VCS.
 - __file name before change__ - empty if file was added or name didn't change.
 - __file name after change__ - empty if file was deleted.
 - __path to file before change__ - empty if file was added, path didn't change or file is in root folder.
 - __path to file after change__ - empty if files was deleted or is in root folder.
 - __change type__ - can be "NEW", "MODIFICATION", "MOVED" or "DELETED". Renaming or moving file is "MOVED" even if file content has changed.
 - __number of lines in file before change__ - "-1" if file is binary or "Grab change size" checkbox is not selected in "Grab Project History" dialog;
   "-2" if file is too big for IntelliJ to diff.
 - __number of lines in file after change__ - similar to above.
 - __number of lines added__ - similar to above.
 - __number of lines modified__ - similar to above.
 - __number of lines removed__ - similar to above.
 - __number of chars in file before change__ - similar to above.
 - __number of chars in file after change__ - similar to above.
 - __number of chars added__ - similar to above.
 - __number of chars modified__ - similar to above.
 - __number of chars removed__ - similar to above.
 - __commit message__ - new line breaks are replaced with "\\n".


Output csv format should be compatible with [RFC4180](http://www.apps.ietf.org/rfc/rfc4180.html).


### Acknowledgments
 - inspired by Michael Feathers [workshop](http://codehistorymining.eventbrite.co.uk/)
 and [Delta Flora](https://github.com/michaelfeathers/delta-flora) project.
 - all visualizations are based on awesome [d3.js examples](https://github.com/mbostock/d3/wiki/Gallery).
