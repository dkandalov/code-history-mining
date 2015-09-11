package codehistoryminer.plugin

interface GroovyScriptRunnerListener {
	final static GroovyScriptRunnerListener none = new GroovyScriptRunnerListener() {
		@Override void loadingError(String message) {}
		@Override void loadingError(Throwable e) {}
		@Override void runningError(Throwable e) {}
	}

	public void loadingError(String message)
	public void loadingError(Throwable e)
	public void runningError(Throwable e)
}
