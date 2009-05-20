package grails.ldap.server

import org.apache.directory.server.core.DefaultDirectoryService
import org.apache.directory.server.ldap.LdapService
import org.apache.directory.server.protocol.shared.SocketAcceptor
import org.apache.directory.shared.ldap.name.LdapDN
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmPartition
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmIndex
import org.apache.directory.shared.ldap.ldif.LdifReader
import org.apache.directory.shared.ldap.ldif.LdifUtils
import org.apache.directory.shared.ldap.exception.LdapNameNotFoundException

import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.BeanNameAware

import grails.util.BuildSettingsHolder

import groovy.text.SimpleTemplateEngine

class TransientGrailsLdapServer implements InitializingBean, BeanNameAware {
	
	final static configOptions = ["port", "base", "indexed"]
	static baseWorkingDir = new File(BuildSettingsHolder.settings?.projectWorkDir, "ldap-server")
	static baseConfigDir = new File("grails-app/ldap-servers")
	
	String beanName
	
	def port = 10389
	def base = "dc=grails,dc=org"
	def indexed = ["objectClass", "ou", "uid"]
	
	def log 
	
	final directoryService
	final ldapService
	
	final baseDn
	
	final configDir
	final dataDir
	final fixturesDir
	final schemaFile
	
	final running = false
	final initialised = false
	
	void afterPropertiesSet()
	{
		if (!initialised) {
			log = org.slf4j.LoggerFactory.getLogger(this.class)

			log.debug("${beanName} config: " + configOptions.collect { "$it = ${this.properties[it]}" }.join(', '))

			configDir = new File(baseConfigDir, beanName - "LdapServer")
			dataDir = new File(configDir, "data")
			fixturesDir = new File(configDir, "fixtures")
			schemaFile = new File(configDir, "schema.ldif")
			baseDn = new LdapDN(base)
			
			start()
			initialised = true
		}
	}
	
	void start() {
		if (!running) {
			log.info("${beanName} starting")
			startDirectoryService()

			loadSchema()
			loadData()
			
			directoryService.changeLog.tag()
			
			startLdapService()
			running = true
			log.info("${beanName} starup complete")
		}
	}
	
	void stop() {
		if (running) {
			log.info("${beanName} stopping")
			stopDirectoryService()
			stopLdapService()
			running = false
			log.info("${beanName} stopped")
		}
	}
	
	void restart() {
		stop()
		start()
	}
	
	void clean() {
		if (running) {
			directoryService.revert()
			directoryService.changeLog.tag()
		}
	}

	void loadFixture(String fixtureName) {
		loadFixtures(fixtureName)
	}

	void loadFixture(Map binding, String fixtureName) {
		loadFixtures([fixtureName] as String[], binding)
	}
	
	void loadFixtures(String[] fixtureNames) {
		loadFixtures(fixtureNames, [:])
	}
	
	void loadFixtures(Map binding, String[] fixtureNames) {
		loadFixtures(fixtureNames, binding)
	}
	
	void loadFixtures(String[] fixtureNames, Map binding) {
		binding = binding ?: [:]
		fixtureNames.each { fixtureName ->
			def fixture = new File(fixturesDir, "${fixtureName}.ldif")
			if (fixture.exists()) {
				def fixtureReader = new FileReader(fixture)
				def engine = new SimpleTemplateEngine()
				def ldif = engine.createTemplate(fixtureReader).make(binding).toString()
				println ldif
				loadLdif(ldif)
			} else {
				throw new IllegalArgumentException("Cannot load fixture '${fixtureName} as it does not exist")
			}
		}
	}
	
	void loadLdif(ldif) {
		consumeLdifReader(new LdifReader(new StringReader(ldif)))
	}
	
	def exists(dn) {
		directoryService.adminSession.exists(new LdapDN(dn as String))
	}
	
	Map get(String dn) {
		try {
			def entry = directoryService.adminSession.lookup(new LdapDN(dn))
			def entryMap = [:]
			entry.attributeTypes.each { at ->
				def attribute = entry.get(at)
				if (at.singleValue) {
					entryMap[attribute.id] = (attribute.isHR()) ? attribute.string : attribute.bytes
				} else {
					def values = []
					attribute.all.each {
						values << it.get()
					}
					entryMap[attribute.id] = values
				}
			}
			entryMap
		} catch (LdapNameNotFoundException e) {
			null
		}
	}
	
	private startDirectoryService() {
		
		directoryService = new DefaultDirectoryService()
		directoryService.changeLog.enabled = true
		def workingDir = new File(baseWorkingDir, beanName)
		if (workingDir.exists()) workingDir.deleteDir()
		directoryService.workingDirectory = workingDir
		
		def partition = addPartition(baseDn.rdn.normValue, base)
		addIndex(partition, *indexed)
		
		directoryService.startup()
		createBase()
	}
	
	private startLdapService() {
		ldapService = new LdapService()
		ldapService.socketAcceptor = new SocketAcceptor(null)
		ldapService.directoryService = directoryService
		ldapService.ipPort = port
		
		ldapService.start()
	}
	
	private stopDirectoryService() {
		directoryService.shutdown()
	}
	
	private stopLdapService() {
		ldapService.stop()
	}
	
	private createBase() {
		def entry = directoryService.newEntry(baseDn)
		entry.add("objectClass", "top", "domain", "extensibleObject")
		entry.add(baseDn.rdn.normType, baseDn.rdn.normValue)
		directoryService.adminSession.add(entry)
	}
	
	private addPartition(partitionId, partitionDn) {
		def partition = new JdbmPartition()
		partition.id = partitionId
		partition.suffix = partitionDn
		directoryService.addPartition(partition)

		partition
	}

	private addIndex(partition, String[] attrs) {
		partition.indexedAttributes = attrs.collect { new JdbmIndex(it) } as Set
	}
	
	private loadSchema() {
		if (schemaFile.exists()) {
			loadFromLdifFile(schemaFile)
		}
	}
	
	private loadData() {
		if (dataDir.exists()) {
			log.debug("Loading server data")
			def filter = [accept: { File dir, String name -> name.endsWith(".ldif") }] as FilenameFilter
			dataDir.listFiles(filter).sort().each {
				loadFromLdifFile(it)
			}
		}
	}
	
	private consumeLdifReader(ldifReader) {
		while (ldifReader.hasNext()) {
			def entry = ldifReader.next()
			def ldif = LdifUtils.convertToLdif(entry, Integer.MAX_VALUE)
			directoryService.adminSession.add(directoryService.newEntry(ldif, entry.dn.toString()))
		}
	}
	
	private loadFromLdifFile(file) {
		consumeLdifReader(new LdifReader(file))
	} 
	
}