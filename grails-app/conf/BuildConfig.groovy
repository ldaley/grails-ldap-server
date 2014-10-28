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
		build (":release:1.0.1", ":svn:1.0.2") {
			export = false
		}
		test (":hibernate:$grailsVersion", ":tomcat:$grailsVersion") {
			export = false
		}
	}
}
