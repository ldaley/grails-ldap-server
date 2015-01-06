grails.project.dependency.resolution = {
	inherits( "global" )
	repositories {
		mavenCentral()
		grailsCentral()
	}

	dependencies {
		compile 'org.apache.directory.server:apacheds-core:1.5.4'
		compile 'org.apache.directory.server:apacheds-protocol-ldap:1.5.4'
		// the following transitive dep is included to workaround a failure during a clean compile
		compile 'org.apache.directory.shared:shared-ldap:0.9.12'
	}
	
	plugins {
		build(":release:3.0.1",
			":rest-client-builder:1.0.3") {
		  export = false
	  }
	}
}
