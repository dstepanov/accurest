package io.codearte.accurest.samples.camel

import com.jayway.jsonpath.DocumentContext
import com.jayway.jsonpath.JsonPath
import com.toomuchcoding.jsonassert.JsonAssertion
import io.codearte.accurest.dsl.GroovyDsl
import io.codearte.accurest.messaging.AccurestMessage
import io.codearte.accurest.messaging.AccurestMessaging
import io.codearte.accurest.messaging.AccurestObjectMapper
import org.apache.camel.model.ModelCamelContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.SpringApplicationContextLoader
import org.springframework.test.context.ContextConfiguration
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import javax.inject.Inject
/**
 * SPIKE ON TESTS FROM NOTES IN MessagingSpec
 */
// Context configuration would end up in base class
@ContextConfiguration(classes = [CamelMessagingApplication], loader = SpringApplicationContextLoader)
public class CamelMessagingApplicationSpec extends Specification {

	// ALL CASES
	@Inject AccurestMessaging accurestMessaging
	AccurestObjectMapper accurestObjectMapper = new AccurestObjectMapper()

	def "should work for triggered based messaging"() {
		given:
			def dsl = GroovyDsl.make {
				label 'some_label'
				input {
					triggeredBy('bookReturnedTriggered()')
				}
				outputMessage {
					sentTo('activemq:output')
					body('''{ "bookName" : "foo" }''')
					headers {
						header('BOOK-NAME', 'foo')
					}
				}
			}
		// generated test should look like this:
		when:
			bookReturnedTriggered()
		then:
			def response = accurestMessaging.receiveMessage('activemq:output')
			response.headers.get('BOOK-NAME')  == 'foo'
		and:
			DocumentContext parsedJson = JsonPath.parse(accurestObjectMapper.writeValueAsString(response.payload))
			JsonAssertion.assertThat(parsedJson).field('bookName').isEqualTo('foo')
	}

	def "should generate tests triggered by a message"() {
		given:
			def dsl = GroovyDsl.make {
				label 'some_label'
				input {
					messageFrom('jms:input')
					messageBody([
					        bookName: 'foo'
					])
					messageHeaders {
						header('sample', 'header')
						header('Content-Type', 'application/json')
					}
				}
				outputMessage {
					sentTo('jms:output')
					body([
					        bookName: 'foo'
					])
					headers {
						header('BOOK-NAME', 'foo')
					}
				}
			}

		// generated test should look like this:

		//given:
		AccurestMessage inputMessage = accurestMessaging.create(
				accurestObjectMapper.writeValueAsString([bookName: 'foo']),
				[sample: 'header']
		)
		when:
			accurestMessaging.send(inputMessage, 'jms:input')
		then:
			def response = accurestMessaging.receiveMessage('jms:output')
			response.headers.get('BOOK-NAME')  == 'foo'
		and:
			DocumentContext parsedJson = JsonPath.parse(accurestObjectMapper.writeValueAsString(response.payload))
			JsonAssertion.assertThat(parsedJson).field('bookName').isEqualTo('foo')
	}

	def "should generate tests without destination, triggered by a message"() {
		given:
			def dsl = GroovyDsl.make {
				label 'some_label'
				input {
					messageFrom('jms:delete')
					messageBody([
					        bookName: 'foo'
					])
					messageHeaders {
						header('sample', 'header')
					}
					assertThat('bookWasDeleted()')
				}
			}

		// generated test should look like this:

		//given:
		AccurestMessage inputMessage = accurestMessaging.create(
				accurestObjectMapper.writeValueAsString([bookName: 'foo']),
				[sample: 'header']
		)
		when:
			accurestMessaging.send(inputMessage, 'jms:delete')
		then:
			noExceptionThrown()
			bookWasDeleted()
	}

	// BASE CLASS WOULD HAVE THIS:

	@Autowired ModelCamelContext camelContext
	@Autowired BookDeleter bookDeleter

	void bookReturnedTriggered() {
		camelContext.createProducerTemplate().sendBody('direct:start', '''{"bookName" : "foo" }''')
	}

	PollingConditions pollingConditions = new PollingConditions()

	void bookWasDeleted() {
		pollingConditions.eventually {
			assert bookDeleter.bookSuccessfulyDeleted.get()
		}
	}

}