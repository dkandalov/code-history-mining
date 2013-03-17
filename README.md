What is this?
=============

This is a plugin for IntelliJ to analyze project source code history.
Original idea is taken from [Delta Flora](https://github.com/michaelfeathers/delta-flora) by Michael Feathers.

This plugin has two parts:
 - transforming VCS history into .csv format (csv because it's easy to read and analyze afterwards)
 - analyzing history and displaying results using d3.js (requires a browser)

It is **work-in-progress**.


Why?
====
There seems to be a lot of interesting data captured in version control systems, yet we don't tend to use it that much.
This is an attempt to make looking at project history easier.
(It is not meant to be used as "metrics" but rather as a tool to get insight/alternative view on project and how it evolves over time.)

Things in project history that might be interesting to look at:
 - people interaction over time
 - how code changes over time
 - how people interact with code

Reasons to do it in IntelliJ:
 - it has API for most popular VCSs so in theory this plugin should work with any VCS supported by IntelliJ
 - it has good support for many languages what makes analyzing source code AST really easy


Please see examples and more on GitHub Pages
============================================
Yes.. the rest of readme is [here](http://dkandalov.github.com/delta-flora-for-intellij)
(because it has SVG bits which cannot be added to markdown).
