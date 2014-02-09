package vcsaccess
import liveplugin.PluginUtil
import vcsaccess._private.CommitReaderGitTest

import static util.DateTimeUtil.dateTime

class CommitMunging_Playground {
	static playOnIt() {
		def project = CommitReaderGitTest.findProject("junit")
		def commitReader = new CommitReader(project, 5)
		def commitFilesMunger = new CommitFilesMunger(project, true)
		def eventsReader = new ChangeEventsReader(project, commitReader, commitFilesMunger.&mungeCommit)

		PluginUtil.doInBackground("playground") {
			// TODO run the request below and fix this
			// 2013-05-26 10:43:42,299 [37126055]  ERROR - ff.impl.fragments.LineFragment - Assertion failed:java.lang.Throwable
//			at com.intellij.openapi.diagnostic.Logger.assertTrue(Logger.java:98)
//			at com.intellij.openapi.diagnostic.Logger.assertTrue(Logger.java:105)
//			at com.intellij.openapi.diff.impl.fragments.LineFragment.checkChildren(LineFragment.java:192)
//			at com.intellij.openapi.diff.impl.fragments.LineFragment.setChildren(LineFragment.java:175)
//			at com.intellij.openapi.diff.impl.processing.TextCompareProcessor.process(TextCompareProcessor.java:55)
//			at com.intellij.openapi.diff.impl.processing.TextCompareProcessor$process.call(Unknown Source)
//			at org.codehaus.groovy.runtime.callsite.CallSiteArray.defaultCall(CallSiteArray.java:45)
//			at org.codehaus.groovy.runtime.callsite.AbstractCallSite.call(AbstractCallSite.java:108)
//			at org.codehaus.groovy.runtime.callsite.AbstractCallSite.call(AbstractCallSite.java:120)
//			at history.CommitFilesMunger.fileChangeInfoOf(CommitFilesMunger.groovy:65)
//			at history.CommitFilesMunger$fileChangeInfoOf.callStatic(Unknown Source)
//			at org.codehaus.groovy.runtime.callsite.CallSiteArray.defaultCallStatic(CallSiteArray.java:53)
//			at org.codehaus.groovy.runtime.callsite.AbstractCallSite.callStatic(AbstractCallSite.java:157)
//			at org.codehaus.groovy.runtime.callsite.AbstractCallSite.callStatic(AbstractCallSite.java:173)
//			at history.CommitFilesMunger$_mungeCommit_closure1.doCall(CommitFilesMunger.groovy:37)
//			at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
//			at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:39)
//			at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:25)
//			at java.lang.reflect.Method.invoke(Method.java:597)
//			at org.codehaus.groovy.reflection.CachedMethod.invoke(CachedMethod.java:90)
//			at groovy.lang.MetaMethod.doMethodInvoke(MetaMethod.java:233)
			eventsReader.readPresentToPast(dateTime("19:00 16/11/2006"), dateTime("19:50 16/11/2006")) { changeEvents ->
				changeEvents.collect{it}
			}

//			eventsReader.readPresentToPast(dateTime("15:42 03/10/2007"), dateTime("15:43 03/10/2007")) { changeEvents ->
//				PluginUtil.show(changeEvents.collect{it}.join("\n"))
//			}
//			PluginUtil.show("----------")
//			eventsReader.request(dateTime("10:00 23/02/2013"), dateTime("17:02 27/02/2013")) { changeEvents ->
//				PluginUtil.show(changeEvents.collect{it.revisionDate}.join("\n"))
//			}
		}
	}
}
