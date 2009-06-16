eventCreateWarStart = { warName, stagingDir ->
	
	if (config.ldapServers) {
		new File(stagingDir, "WEB-INF/grails-app/ldap-servers").mkdirs()
		config.ldapServers.each { ldapServer ->
			ant.copy(todir:"$stagingDir/WEB-INF/grails-app/ldap-servers", overwrite:true) {
				fileset(dir:"${basedir}/grails-app/ldap-servers", includes:"${ldapServer.key}/**")
			}
		}
	}
	
}

