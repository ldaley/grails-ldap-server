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

class TransientGrailsLdapServer implements InitializingBean, BeanNameAware {
	
	static baseWorkingDir = new File(BuildSettingsHolder.settings?.projectWorkDir, "ldap-server")
	static baseConfigDir = new File("ldap-server")
	
	String beanName
	
	def port = 10389
	def base = "dc=grails,dc=org"
	def indexed = ["objectClass", "ou", "uid"]
	
	private directoryService
	private ldapService
	
	private baseDn
	
	private configDir
	
	void afterPropertiesSet()
	{
		configDir = new File(baseConfigDir, beanName)
		
		def workDir = new File(baseWorkingDir, beanName)
		if (workDir.exists()) workDir.deleteDir()
		
		baseDn = new LdapDN(base)
		
		directoryService = new DefaultDirectoryService()

		directoryService.changeLog.enabled = false
		directoryService.workingDirectory = workDir
		def partition = addPartition(baseDn.rdn.normValue, base)
		addIndex(partition, *indexed)
		directoryService.startup()

		try {
			directoryService.adminSession.lookup(partition.suffixDn)
		}
		catch (LdapNameNotFoundException lnnfe) {
			def dn = baseDn
			def entry = directoryService.newEntry(dn)
			entry.add("objectClass", "top", "domain", "extensibleObject")
			entry.add(baseDn.rdn.normType, baseDn.rdn.normValue)
			directoryService.adminSession.add(entry)
		}
		
		loadLdif()
		
		ldapService = new LdapService()
		ldapService.socketAcceptor = new SocketAcceptor(null)
		ldapService.directoryService = directoryService
		ldapService.ipPort = port
		ldapService.start()
	}
		
	def addPartition(partitionId, partitionDn) {
		def partition = new JdbmPartition()
		partition.id = partitionId
		partition.suffix = partitionDn
		directoryService.addPartition(partition)

		partition
	}

	void addIndex(partition, String[] attrs) {
		partition.indexedAttributes = attrs.collect { new JdbmIndex(it) } as Set
	}
	
	void loadLdif() {
		def ldifDir = new File(configDir, "ldif")
		if (ldifDir.exists()) {
			def filter = [accept: { File dir, String name -> name.endsWith(".ldif") }] as FilenameFilter
			ldifDir.listFiles(filter).sort().each {
				def ldifReader = new LdifReader(it)
				while (ldifReader.hasNext())
				{
					def entry = ldifReader.next()
					def ldif = LdifUtils.convertToLdif(entry)
					def dn = entry.get("dn").getString()
					directoryService.adminSession.add(directoryService.newEntry(ldif, dn))
				}
			}
		}
	}
}