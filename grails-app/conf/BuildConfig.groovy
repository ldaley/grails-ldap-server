grails.project.dependency.resolution = {
	inherits( "global" )
	repositories {
		grailsCentral()
		grailsHome()
		mavenLocal()
		mavenCentral()
		grailsRepo "http://grails.org/plugins"
	}
	
	plugins {
		build (":release:1.0.1") {
			export = false
		}
	}
}
