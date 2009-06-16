eventCreateWarStart = { warName, stagingDir ->
	
	if (config.ldapServers) {
		new File(stagingDir, "WEB-INF/grails-app/ldap-servers").mkdirs()
		config.ldapServers.each { ldapServer ->
			ant.copy(todir:"$stagingDir/WEB-INF/grails-app/ldap-servers", overwrite:true) {
				fileset(dir:"${basedir}/grails-app/ldap-servers", includes:"${ldapServer.key}/**")
			}
		}
	} else {
		def libs = new File(ldapServerPluginDir, "lib")
		libs.eachFile { file ->
			ant.delete(file: "$stagingDir/WEB-INF/lib/${file.name}")
		}
	}
	
}

