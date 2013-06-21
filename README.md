### What is this?

This is a plugin for [IntelliJ](https://github.com/JetBrains/intellij-community)-based IDEs to grab
and analyze project source code history.

It has has two parts:
 - reading project history from version control and saving it as csv file
 - reading history from csv, analyzing and visualizing it with [d3.js](http://d3js.org/) (needs a browser with SVG support)

Warnings:
 - all VCS supported by IntelliJ should work but I only tried Git and Svn
 - all browsers with SVG support should work but I only tried Chrome and Safari


### Why?
There seems to be a lot of interesting data captured in version control systems, yet we don't use it that much.
This is an attempt to make looking at project history easier.

 - converting history to csv is useful because it's easy to read and process in any language (or even in a spreadsheet)
 - interactive visualization is cool because it's fun to play with and can hopefully give deeper insight into project history


### Screenshots / Examples
Screenshot of grab history dialog:
<img src="https://raw.github.com/dkandalov/code-history-mining/master/grab-history-screenshot.png" alt="screenshot" title="screenshot" align="center"/>

For visualization examples please see [GitHub pages](http://dkandalov.github.com/code-history-mining/junit.html).
(It's a separate page to keep readme small and not to put SVG into markdown.)


### How to use
Not released yet. Will be soon in IntelliJ plugin repository.


### Output csv format
Each commit is broken down into several lines. One line corresponds to one file changed in commit.
Commits are stored ordered by time from present to past as they are read from VCS
(although there might be exceptions because VCSs don't guarantee this order).
```
2001-10-02 20:38:22 +0100,0bb3dfe2939cc214ee5e77556a48d4aea9c6396a,kbeck <kbeck>,IMoney.java,,/junit/samples/money,,MODIFICATION,38,42,4,0,0,817,888,71,0,0,"Cleaning up MoneyBag construction"
2001-10-02 20:38:22 +0100,0bb3dfe2939cc214ee5e77556a48d4aea9c6396a,kbeck <kbeck>,Money.java,,/junit/samples/money,,MODIFICATION,70,73,3,1,0,1595,1684,86,32,0,"Cleaning up MoneyBag construction"
2001-10-02 20:38:22 +0100,0bb3dfe2939cc214ee5e77556a48d4aea9c6396a,kbeck <kbeck>,MoneyBag.java,,/junit/samples/money,,MODIFICATION,140,131,8,4,23,3721,3594,214,154,511,"Cleaning up MoneyBag construction"
2001-10-02 20:38:22 +0100,0bb3dfe2939cc214ee5e77556a48d4aea9c6396a,kbeck <kbeck>,MoneyTest.java,,/junit/samples/money,,MODIFICATION,156,141,0,34,0,5187,4785,0,1594,0,"Cleaning up MoneyBag construction"
2001-07-09 23:51:53 +0100,ce0bb8f59ea7de1ac3bb4f678f7ddf84fe9388ed,egamma <egamma>,.classpath,,,,NEW,0,6,6,0,0,0,241,241,0,0,"added .classpath for eclipse"
2001-07-09 23:51:53 +0100,ce0bb8f59ea7de1ac3bb4f678f7ddf84fe9388ed,egamma <egamma>,.vcm_meta,,,,MODIFICATION,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,"added .classpath for eclipse"
```
Columns:
 - commit date - in "yyyy-MM-dd HH:mm:ss Z" format (see [javadoc](http://docs.oracle.com/javase/7/docs/api/java/text/SimpleDateFormat.html) for details).
 - revision - format depends on underlying VCS.
 - author - committer name from VCS.
 - file name after change - empty if file was deleted.
 - file name before change - empty if file was added or didn't change.
 - path to file after change - empty if files was deleted or is in root folder.
 - path to file before change - empty if file was added, didn't change or is in root folder.
 - change type - can be "NEW", "MODIFICATION", "MOVED" or "DELETED". Renaming or moving file is "MOVED" even if file content has changed.
 - number of lines in file before change - "-1" if file is binary or "Grab change size" checkbox is disabled in "Grab Project History" dialog;
   "-2" if file was too big diff.
 - number of lines in file after change
 - number of lines added
 - number of lines modified
 - number of lines removed
 - number of chars in file before change
 - number of chars in file after change
 - number of chars added
 - number of chars modified
 - number of chars removed
 - commit message - new line breaks are replaced with "\\n".


Output csv format should be compatible with [RFC4180](http://www.apps.ietf.org/rfc/rfc4180.html).


### Acknowledgments
 - inspired by Michael Feathers workshop and [Delta Flora](https://github.com/michaelfeathers/delta-flora) project
 - all visualizations are based on awesome [d3.js examples](https://github.com/mbostock/d3/wiki/Gallery)
