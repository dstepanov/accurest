package io.codearte.accurest.builder

import groovy.transform.PackageScope
import groovy.transform.TypeChecked
import groovy.transform.TypeCheckingMode
import io.codearte.accurest.dsl.GroovyDsl
import io.codearte.accurest.dsl.internal.Header
import io.codearte.accurest.dsl.internal.QueryParameter
import io.codearte.accurest.dsl.internal.Request
import io.codearte.accurest.dsl.internal.UrlPath

import java.util.regex.Pattern

@PackageScope
@TypeChecked
class MockMvcSpockMethodBodyBuilder extends SpockMethodBodyBuilder {

	MockMvcSpockMethodBodyBuilder(GroovyDsl stubDefinition) {
		super(stubDefinition)
	}

	@Override
	protected void given(BlockBuilder bb) {
		bb.addLine('def request = given()')
		bb.indent()
		request.headers?.collect { Header header ->
			bb.addLine(".header('${header.name}', '${header.serverValue}')")
		}
		if (request.body) {
			bb.addLine(".body('$bodyAsString')")
		}
		bb.unindent()
	}

	@Override
	protected void when(BlockBuilder bb) {
		bb.addLine('def response = given().spec(request)')
		bb.indent()

		String url = buildUrl(request)
		String method = request.method.serverValue.toString().toLowerCase()

		bb.addLine(/.${method}("$url")/)
		bb.unindent()
	}

	@Override
	protected void validateResponseCodeBlock(BlockBuilder bb) {
		bb.addLine("response.statusCode == $response.status.serverValue")
	}

	@Override
	protected void validateResponseHeadersBlock(BlockBuilder bb) {
		response.headers?.collect { Header header ->
			bb.addLine("response.header('$header.name') ${convertHeaderComparison(header.serverValue)}")
		}
	}

	private String convertHeaderComparison(Object headerValue) {
		return " == '$headerValue'"
	}

	private String convertHeaderComparison(Pattern headerValue) {
		return "==~ java.util.regex.Pattern.compile('$headerValue')"
	}

	@Override
	protected String getResponseAsString() {
		return 'response.body.asString()'
	}

}
