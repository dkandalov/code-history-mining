package history.events

@SuppressWarnings("GroovyUnusedDeclaration")
@groovy.transform.Immutable
class ElementChangeInfo {
	static ElementChangeInfo EMPTY = new ElementChangeInfo("", "", 0, 0, 0, 0)

	String elementName
	String changeType
	int fromLine      // TODO change to linesBefore/After
	int toLine
	int fromOffset
	int toOffset
}
