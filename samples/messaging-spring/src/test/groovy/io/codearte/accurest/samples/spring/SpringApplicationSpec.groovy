package io.codearte.accurest.samples.spring

import com.jayway.jsonpath.DocumentContext
import com.jayway.jsonpath.JsonPath
import com.toomuchcoding.jsonassert.JsonAssertion
import io.codearte.accurest.dsl.GroovyDsl
import io.codearte.accurest.messaging.AccurestMessage
import io.codearte.accurest.messaging.AccurestMessaging
import io.codearte.accurest.messaging.AccurestObjectMapper
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
@ContextConfiguration(classes = [SpringMessagingApplication], loader = SpringApplicationContextLoader)
public class SpringApplicationSpec extends Specification {

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
					sentTo('output')
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
			def response = accurestMessaging.receiveMessage('output')
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
					messageFrom('input')
					messageBody([
					        bookName: 'foo'
					])
					messageHeaders {
						header('sample', 'header')
					}
				}
				outputMessage {
					sentTo('output')
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
			accurestMessaging.send(inputMessage, 'input')
		then:
			def response = accurestMessaging.receiveMessage('output')
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
					messageFrom('delete')
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
			accurestMessaging.send(inputMessage, 'delete')
		then:
			noExceptionThrown()
			bookWasDeleted()
	}

	// BASE CLASS WOULD HAVE THIS:

	@Autowired BookService bookService
	@Autowired BookListener bookListener

	void bookReturnedTriggered() {
		bookService.returnBook(new BookReturned("foo"))
	}

	PollingConditions pollingConditions = new PollingConditions()

	void bookWasDeleted() {
		pollingConditions.eventually {
			assert bookListener.bookSuccessfulyDeleted.get()
		}
	}

}