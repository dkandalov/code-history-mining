package codemining.plugin
import org.junit.Test

@SuppressWarnings("GrMethodMayBeStatic")
/**
 * Very basic stubbing for Groovy.
 *
 * The idea is that public methods of a class are its interface
 * (and could be extracted into interface in java, but it might be too much for dynamic language like groovy).
 *
 * Did this because mockito didn't work in groovy even with mockito-groovy-support.
 * Wasn't happy with standard groovy frameworks.
 */
class GroovyStubber {
	@Test void "should stub public interface of a class"() {
		def sampleStub = stub(SampleClass)

		sampleStub.voidMethod()
		assert sampleStub.defMethod() == null
		assert !sampleStub.booleanMethod()
		assert sampleStub.intMethod() == 0
		assert sampleStub.listMethod() == []
		assert sampleStub.objectArrayMethod() == []
	}

	/**
	 * @param aClass class to stub, must have default constructor
	 */
	static <T> T stub(Class<T> aClass, Map overrides = [:]) {
		def actionByReturnType = [
				(Void.TYPE): doesNothing,
				(Object): returns(null),
				(Boolean.TYPE): returns(false),
				(Boolean): returns(false),
				(Integer.TYPE): returns(0),
				(Integer): returns(0),
				(Collection): returns([]),
				(List): returns([])
		].withDefault{ type ->
			if (type.array) returns([].toArray()) else doesNothing
		}

		Map map = aClass.methods.inject([:]){ Map map, method ->
			map.put(method.name, actionByReturnType[method.returnType])
			map
		}
		map.putAll(overrides)
		map.asType(aClass)
	}

	static <T> Closure<T> returns(T value) { { Object... args -> value } }
	static <T> Closure<T> does(Closure closure) { { Object... args -> closure() } }
	static final Closure doesNothing = { Object... args -> }

	static class SampleClass {
		void voidMethod() { throw new IllegalStateException("Not stubbed") }
		def defMethod() { throw new IllegalStateException("Not stubbed") }
		boolean booleanMethod() { throw new IllegalStateException("Not stubbed") }
		int intMethod() { throw new IllegalStateException("Not stubbed") }
		List listMethod() { throw new IllegalStateException("Not stubbed") }
		Object[] objectArrayMethod() { throw new IllegalStateException("Not stubbed") }
	}

}
