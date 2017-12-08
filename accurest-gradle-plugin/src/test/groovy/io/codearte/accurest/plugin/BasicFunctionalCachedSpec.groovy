package io.codearte.accurest.plugin

import org.gradle.testkit.runner.UnexpectedBuildFailure
import spock.lang.Stepwise

import static org.gradle.testkit.runner.TaskOutcome.FAILED
import static org.gradle.testkit.runner.TaskOutcome.FROM_CACHE
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

@Stepwise
class BasicFunctionalCachedSpec extends AccurestIntegrationSpec {
	private static final String GENERATED_TEST = "module-tests//build//extra-generated-sources//accurest//com//ofg//twitter_places_analyzer//PairIdSpec.groovy"
	private static final String GENERATED_CLIENT_JSON_STUB = "module-tests//build//production//module-tests-stubs//repository//mappings//com//ofg//twitter-places-analyzer//pairId//collerate_PlacesFrom_Tweet.json"
	private static final String GROOVY_DSL_CONTRACT = "module-tests//repository//mappings//com//ofg//twitter-places-analyzer//pairId//collerate_PlacesFrom_Tweet.groovy"
	private static final String TEST_EXECUTION_XML_REPORT = "build/test-results/test/TEST-accurest.com.ofg.twitter_places_analyzer.PairIdSpec.xml"
	private static final String CONTROLLER_SRC = "module-impl//src//main//java//com//ofg//twitter//place//PairIdController.java"

	def setup() {
		setupForProject("functionalTest/bootSimpleCached")
		runTasksSuccessfully('clean') //delete accidental output when previously importing SimpleBoot into Idea to tweak it
	}

	def "tasks should be up-to-date when appropriate"() {
		given:
			assert !fileExists(GENERATED_CLIENT_JSON_STUB)
			assert !fileExists(TEST_EXECUTION_XML_REPORT)

		when:
			runTasksSuccessfully(':module-tests:generateWireMockClientStubs', ':module-tests:generateAccurest')

		then:
			fileExists(GENERATED_CLIENT_JSON_STUB)
			file(GENERATED_TEST).exists()

		when:
			def result = run('clean', ':module-tests:generateWireMockClientStubs', ':module-tests:generateAccurest')
			validateTasksOutcome(result, FROM_CACHE, 'module-tests:generateWireMockClientStubs', 'module-tests:generateAccurest')

		then:
			fileExists(GENERATED_CLIENT_JSON_STUB)
			fileExists(GENERATED_TEST)

		when:
			def testResult = run('clean', 'test')

		then:
			validateTasksOutcome(testResult, SUCCESS, 'module-tests:test')

		when:
			testResult = run('clean', 'test')

		then:
			validateTasksOutcome(testResult, FROM_CACHE, 'module-tests:test')
			file(".gradle").deleteDir()

		when:
			def controller = file(CONTROLLER_SRC)
			controller.text = controller.text.replace("Warsaw", "Prague")
			run('clean', ':module-tests:generateAccurest', 'test')

		then:
			def e = thrown(UnexpectedBuildFailure)
			validateTasksOutcome(e.buildResult, FROM_CACHE, 'module-tests:generateAccurest')
			validateTasksOutcome(e.buildResult, FAILED, 'module-tests:test')
	}

}
