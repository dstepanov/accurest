package io.codearte.accurest.stubrunner

import groovy.transform.CompileStatic
import groovy.transform.EqualsAndHashCode
/**
 * Structure representing executed stubs. Contains the configuration of each stub
 * together with the port on which its executed.
 */
@EqualsAndHashCode
@CompileStatic
class RunningStubs {
	final Map<StubConfiguration, Integer> namesAndPorts = [:]

	RunningStubs(Map<StubConfiguration, Integer> map) {
		this.namesAndPorts.putAll(map)
	}

	RunningStubs(Collection<RunningStubs> runningStubs) {
		runningStubs.each {
			this.namesAndPorts.putAll(it.namesAndPorts)
		}
	}

	Integer getPort(String artifactId) {
		return getEntry(artifactId)?.value
	}

	Map.Entry<StubConfiguration, Integer> getEntry(String artifactId) {
		return namesAndPorts.entrySet().find {
			it.key.matchesIvyNotation(artifactId)
		}
	}

	Integer getPort(String groupId, String artifactId) {
		return namesAndPorts.entrySet().find {
			it.key.matchesIvyNotation("$groupId:$artifactId")
		}?.value
	}

	boolean isPresent(String artifactId) {
		return getEntry(artifactId)
	}

	boolean isPresent(String groupId, String artifactId) {
		return namesAndPorts.entrySet().find {
			it.key.matchesIvyNotation("$groupId:$artifactId")
		}
	}

	Set<StubConfiguration> getAllServices() {
		return namesAndPorts.keySet()
	}

	Set<String> getAllServicesNames() {
		return namesAndPorts.keySet().collect { it.artifactId } as Set
	}

	Map<String, Integer> toIvyToPortMapping() {
		return namesAndPorts.collectEntries { [(it.key.toColonSeparatedDependencyNotation()) : it.value]  } as Map<String, Integer>
	}

	@Override
	String toString() {
		return namesAndPorts.collect {
			"Stub [${it.key.toColonSeparatedDependencyNotation()}] is running on port [${it.value}]"
		}.join("\n")
	}
}