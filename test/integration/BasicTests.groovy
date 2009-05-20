import grails.ldap.server.TransientGrailsLdapServer
import org.apache.directory.shared.ldap.name.LdapDN

class BasicTests extends GroovyTestCase {

	static transactional = false
	
	def d1LdapServer
	def d2LdapServer
	
	def grailsApplication
	
	void testServerBeansExist() {
		[d1LdapServer, d2LdapServer].each {
			assertNotNull(it)
		}
	}
	
	void testConfig() {
		[d1LdapServer, d2LdapServer].each { server ->
			def conf = grailsApplication.config.ldapServers[server.beanName - "LdapServer"]
			def defaultServer = new TransientGrailsLdapServer()
			TransientGrailsLdapServer.configOptions.each {
				def value = conf[it]
				if ((value instanceof ConfigObject) == false) {
					assertEquals("${server.beanName}: $it does not match config value", value, server."$it")
				} else {
					assertEquals("${server.beanName}: $it does not match default value", defaultServer."$it", server."$it")
				}
			}
		}
	}
	
	void testStopStartRestart() {
		[d1LdapServer, d2LdapServer].each { server ->
			assertTrue(server.running)
			assertTrue(server.directoryService.started)

			server.stop()
			assertFalse(server.running)
			assertFalse(server.directoryService.started)
			
			server.restart()
			assertTrue(server.running)
			assertTrue(server.directoryService.started)
		}
	}
	
	void testLoadData() {
		assertTrue(d1LdapServer.exists("ou=test,dc=d1"))
	}
	
	void testLoadFixture() {
		d2LdapServer.loadFixture("some")
		assertTrue(d2LdapServer.exists("ou=test,dc=d2"))
	}

	void testLoadLdif() {
		d2LdapServer.loadLdif("""
dn: cn=cn3,dc=d2
cn: cn3
sn: sn
objectClass: person
objectClass: top
objectClass: organizationalPerson		
""")
		assertTrue(d2LdapServer.exists("cn=cn3,dc=d2"))
	}
	
	void testClean() {
		d2LdapServer.loadFixture("some")
		assertTrue(d2LdapServer.exists("ou=test,dc=d2"))
		d2LdapServer.clean()
		assertFalse(d2LdapServer.exists("ou=test,dc=d2"))
	}
	
	void tearDown() {
		[d1LdapServer, d2LdapServer]*.clean()
	}
}