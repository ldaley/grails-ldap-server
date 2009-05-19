import grails.ldap.server.TransientGrailsLdapServer

class LdapServerGrailsPlugin {

    def version = "0.1"
    def grailsVersion = "1.1 > *"
    def dependsOn = [:]
    def pluginExcludes = []

    def author = "Luke Daley"
    def authorEmail = "ld@ldaley.com"
    def title = "Embedded LDAP Server Plugin"
    def description = 'Allows the embedding of an LDAP directory (via ApacheDS) for testing purposes'

    def documentation = "http://grails.org/plugin/grails-ldap-server"

	def servers = []
	
    def doWithSpring = {
		createServers(application.config.ldapServers, delegate)
    }

    def onChange = { event ->
        // TODO Implement code that is executed when any artefact that this plugin is
        // watching is modified and reloaded. The event contains: event.source,
        // event.application, event.manager, event.ctx, and event.plugin.
    }

    def onConfigChange = { event ->
        // TODO Implement code that is executed when the project configuration changes.
        // The event is the same as for 'onChange'.
    }

	def createServers(config, beanBuilder) {
		config.each { name, props ->
			beanBuilder."$name"(TransientGrailsLdapServer) {
				["port", "base", "indexed"].each {
					if (props[it])
						delegate."$it" = props[it]
				}
			}
		}
	}
}
