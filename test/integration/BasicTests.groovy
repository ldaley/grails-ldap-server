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
	
	void testLoadFixtures() {
		d2LdapServer.loadFixture("testou")
		assertTrue(d2LdapServer.exists("ou=test,dc=d2"))
		d2LdapServer.clean()
		
		d2LdapServer.loadFixtures("testou", "country")
		assertTrue(d2LdapServer.exists("c=au,dc=d2"))
	}
	
	void testLoadTemplatedFixtures() {
		d2LdapServer.loadFixture("personTemplate", cn: "cn1", sn: "sn1")
		assertTrue(d2LdapServer.exists("cn=cn1,dc=d2"))
		
		d2LdapServer.loadFixtures("personTemplate", "ouTemplate", cn: "cn2", sn: "sn2", ou: "ou1")
		assertTrue(d2LdapServer.exists("cn=cn2,dc=d2"))
		assertTrue(d2LdapServer.exists("ou=ou1,dc=d2"))
		
		d2LdapServer.loadFixture("personTemplate", [cn: "cn3", sn: "sn3"])
		assertTrue(d2LdapServer.exists("cn=cn3,dc=d2"))
		
		// Commented out because Groovy doesn't support this syntax properly.
/*		d2LdapServer.loadFixtures("personTemplate", "ouTemplate", [cn: "cn4", sn: "sn4", ou: "ou2"])
		assertTrue(d2LdapServer.exists("cn=cn4,dc=d2"))
		assertTrue(d2LdapServer.exists("ou=ou2,dc=d2"))*/
		
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
		d2LdapServer.loadFixture("testou")
		assertTrue(d2LdapServer.exists("ou=test,dc=d2"))
		d2LdapServer.clean()
		assertFalse(d2LdapServer.exists("ou=test,dc=d2"))
	}
	
	void testGetAt() {
		d2LdapServer.loadFixture("some")
		def entry = d2LdapServer["cn=cn2,dc=d2"]
		assertTrue(entry instanceof Map)
		
		assertTrue(entry.cn instanceof List)
		assertEquals("cn2", entry.cn.first())
		
		assertTrue(entry.usercertificate.first() instanceof byte[])

		d2LdapServer.loadFixture("country")
		entry = d2LdapServer["c=au,dc=d2"]
		assertEquals("au", entry.c)
	}
	
	void tearDown() {
		[d1LdapServer, d2LdapServer]*.clean()
	}
}