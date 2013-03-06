What is this?
=============

This is a plugin for IntelliJ to gather historical data from VCS for java projects.
It's a remake of [Delta Flora](https://github.com/michaelfeathers/delta-flora) by Michael Feathers.

It decomposes project history down to method level and saves it in csv format (csv because it's easier to read analyze afterwards).
Looks like this:
```
ConfigImportHelper::getLaunchFilesCandidates,4fd6cc48b6cd9ce020756ed75704dde17ebfc63c,Kirill Safonov <kirill.safonov@jetbrains.com>,2013-03-01 17:49:45 +0000,ConfigImportHelper.java,changed,275,276,10955,11043,"WEB-6724 Installation home of a previous WebStorm version is not accepted during start-up settings import"
ConfigImportHelper::isInstallationHomeOrConfig,4fd6cc48b6cd9ce020756ed75704dde17ebfc63c,Kirill Safonov <kirill.safonov@jetbrains.com>,2013-03-01 17:49:45 +0000,ConfigImportHelper.java,changed,416,417,15867,15952,"WEB-6724 Installation home of a previous WebStorm version is not accepted during start-up settings import"
ConfigImportSettings,4fd6cc48b6cd9ce020756ed75704dde17ebfc63c,Kirill Safonov <kirill.safonov@jetbrains.com>,2013-03-01 17:49:45 +0000,ConfigImportSettings.java,added,32,32,993,995,"WEB-6724 Installation home of a previous WebStorm version is not accepted during start-up settings import"
ConfigImportSettings::getExecutableName,4fd6cc48b6cd9ce020756ed75704dde17ebfc63c,Kirill Safonov <kirill.safonov@jetbrains.com>,2013-03-01 17:49:45 +0000,ConfigImportSettings.java,added,32,34,995,1076,"WEB-6724 Installation home of a previous WebStorm version is not accepted during start-up settings import"
ConfigImportSettings,4fd6cc48b6cd9ce020756ed75704dde17ebfc63c,Kirill Safonov <kirill.safonov@jetbrains.com>,2013-03-01 17:49:45 +0000,ConfigImportSettings.java,added,34,36,1076,1080,"WEB-6724 Installation home of a previous WebStorm version is not accepted during start-up settings import"
ConfigImportSettings::getMainJarName,4fd6cc48b6cd9ce020756ed75704dde17ebfc63c,Kirill Safonov <kirill.safonov@jetbrains.com>,2013-03-01 17:49:45 +0000,ConfigImportSettings.java,added,36,38,1080,1158,"WEB-6724 Installation home of a previous WebStorm version is not accepted during start-up settings import"
ConfigImportSettings,4fd6cc48b6cd9ce020756ed75704dde17ebfc63c,Kirill Safonov <kirill.safonov@jetbrains.com>,2013-03-01 17:49:45 +0000,ConfigImportSettings.java,added,38,40,1158,1160,"WEB-6724 Installation home of a previous WebStorm version is not accepted during start-up settings import"
,60c05b6aa0d8a71c8fa7c8d7d6a90b4ad50b2c37,Konstantin Bulenkov <kb@jetbrains.com>,2013-03-01 17:36:07 +0000,NavBarListener.java,changed,1,2,3,43,"IDEA-96105 Put focus to the editor after file deletion from navigation bar"
,60c05b6aa0d8a71c8fa7c8d7d6a90b4ad50b2c37,Konstantin Bulenkov <kb@jetbrains.com>,2013-03-01 17:36:07 +0000,NavBarListener.java,added,20,21,768,808,"IDEA-96105 Put focus to the editor after file deletion from navigation bar"
```

The idea is to later analyze this csv to discover something about the project.
E.g. "what are the most often changed parts of the project?", "which classes change together?", etc.

There is no actual analysis here so far.. just csv output.


How to use?
===========
To use this plugin you'll need to install host-plugin [intellij-eval](https://github.com/dkandalov/intellij_eval).
In "Plugins" toolwindow use "Add -> Plugin from Git" and then run it.
It'll start analyzing history for currently open project.
Analysis goes from now into the past and is incremental (you can stop it and next time it'll continue from the last processed commit).

After analysis finished or was cancelled it will show where produced csv is stored
(on mac it's "~/Library/Application Support/IntelliJIdea12/delta-flora").

To configure how much history to analyze or change output format please look at source code :)


What can be improved
====================
 - in theory should work with any version control setup in IntelliJ project but was only
 tested with Git. Potential problems might be that it's too slow with SVN or API works in a different way.
 - it's a bit slow even with Git and the bottleneck is.. Git. There must be a way to speed it up.
 - at the moment it only looks at Java (Groovy) syntax tree to figure out method name for a change.
 It shouldn't be difficult to support other languages (those that have IntelliJ plugins).