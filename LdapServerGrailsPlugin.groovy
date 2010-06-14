import grails.ldap.server.TransientGrailsLdapServer

class LdapServerGrailsPlugin {

	static beanNameSuffix = "LdapServer"

	def version = "0.1.8"
	def grailsVersion = "1.1 > *"
	def dependsOn = [:]
	def watchedResources = ["file:./grails-app/ldap-servers/*/data/*.ldif", "file:./grails-app/ldap-servers/*/schema/*.ldif"]

	def author = "Luke Daley"
	def authorEmail = "ld@ldaley.com"
	def title = "Embedded LDAP Server Plugin"
	def description = 'Allows the embedding of an LDAP directory (via ApacheDS) for testing purposes'
	def documentation = "http://grails.org/plugin/grails-ldap-server"

	def pluginExcludes = ["grails-app/ldap-servers/**"]

	def servers = []

	def doWithSpring = {
		createServers(application.config.ldapServers, delegate)
	}

	def onChange = { event ->
		handleChange.delegate = delegate
		handleChange(event)
	}

	def onConfigChange = { event ->
		handleChange.delegate = delegate
		handleChange(event)
	}

	def handleChange = { event ->
		servers.each {
			event.ctx.getBean(it).stop()
			event.ctx.removeBeanDefinition(it)
		}
		servers.clear()

		def beanDefinitions = beans {
			createServers(event.application.config.ldapServers, delegate)
		}

		servers.each {
			event.ctx.registerBeanDefinition(it, beanDefinitions.getBeanDefinition(it))
			event.ctx.getBean(it).afterPropertiesSet()
		}
	}

	def createServers(config, beanBuilder) {
		config.each { name, props ->
			def beanName = name + beanNameSuffix
			beanBuilder."$beanName"(TransientGrailsLdapServer) {
				TransientGrailsLdapServer.configOptions.each {
					if ((props[it] instanceof ConfigObject) == false)
						delegate."$it" = props[it]
				}
			}
			servers << beanName
		}
	}
}
