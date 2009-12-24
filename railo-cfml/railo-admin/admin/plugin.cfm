
<cfparam name="application.plugin" default="#struct()#">
<cfparam name="application.pluginLanguage.de" default="#struct()#">
<cfparam name="application.pluginLanguage.en" default="#struct()#">
<cfparam name="url.pluginAction" default="overview">
<cfparam name="session.alwaysNew" default="false" type="boolean">
<cfif structKeyExists(url,'alwaysNew')>
	<cfset session.alwaysNew=url.alwaysNew EQ true>
</cfif>
<cfif not structKeyExists(url,"plugin")>
	<cflocation url="#request.self#" addtoken="no">
</cfif>

<!--- load language --->
<cfif true or not structKeyExists(application.pluginLanguage[session.railo_admin_lang],url.plugin)>
	<cfset fileLanguage="#pluginDir#/#url.plugin#/language.xml">
    <cfset language=struct(
			title:ucFirst(url.plugin),
			text:''
	)>
	<cfif fileExists(fileLanguage)>
		<cffile action="read" file="#fileLanguage#" variable="txtLanguage" charset="utf-8">
		<cfxml casesensitive="no" variable="xml">
		<cfoutput>#txtLanguage#</cfoutput>
		</cfxml>
        <cfset xml = XmlSearch(xml, "/languages/language[@key='#lCase(session.railo_admin_lang)#']")[1]>
		<cfset language.title=xml.title.XmlText>
		<cfset language.text=xml.description.XmlText>
		<cfif isDefined('xml.custom')>
			<cfset custom=xml.custom>
			<cfloop index="idx" from="1" to="#arraylen(custom)#">
				<cfset language[custom[idx].XmlAttributes.key]=custom[idx].XmlText>
			</cfloop>
		</cfif>
	</cfif>
	<cfset application.pluginLanguage[session.railo_admin_lang][url.plugin]=language>
</cfif>



<!--- load plugin --->
<cfif not structKeyExists(application.plugin,url.plugin)>
	<cfset application.plugin[url.plugin].application=struct()>
</cfif>
<cfif not structKeyExists(application.plugin[url.plugin],'component') or session.alwaysNew>
	<cfset application.plugin[url.plugin].component=createObject('component','railo_plugin_directory.'&url.plugin&'.Action')>
	<cfset application.plugin[url.plugin].component.init(
		application.pluginLanguage[session.railo_admin_lang][url.plugin],
		application.plugin[url.plugin].application)>
</cfif>
<cfset plugin=application.plugin[url.plugin]>

<cfset plugin.language=application.pluginLanguage[session.railo_admin_lang][url.plugin]>


<cfset request.subTitle=plugin.language.title>
<cfoutput><cfif structKeyExists(plugin.language,'text')>#plugin.language.text#<br /><br /></cfif></cfoutput>
	
<!--- create scopes --->
<cfset req=duplicate(url)>
<cfset _form=duplicate(form)>
<cfif structKeyExists(_form,'fieldnames')>
	<cfset structDelete(_form,'fieldnames')>
</cfif>
<cfloop collection="#_form#" item="key">
	<cfset req[key]=_form[key]>
</cfloop>
<cfset app=plugin.application>
<cfset lang=plugin.language>

<!---cfset plugin.component._action(plugin:plugin,lang:lang,app:app,req:req)--->

<!-- first call the action if exists -->
<cfset hasAction=structKeyExists(plugin.component,url.pluginAction)>

<cfif hasAction>
	<cfset rtnAction= plugin.component._action(url.pluginAction,lang,app,req)>
    
	<!--- cfset rtnAction= plugin.component[url.pluginAction](lang,app,req)--->
</cfif>
<cfif not isDefined('rtnAction')>
	<cfset rtnAction=url.pluginAction>
</cfif>

<!-- redirect -->
<cfif findNoCase('redirect:',rtnAction) EQ 1>
	<cflocation url="#plugin.component.action(mid(rtnAction,10,len(rtnAction)))#">
</cfif>

<!-- then call display -->
<cfset dspFile="/railo_plugin_directory/#url.plugin#/#rtnAction#.cfm">

<cfset hasDisplay=fileExists(expandPath(dspFile))>
<cfif rtnAction NEQ "_none" and hasDisplay>
	
	<cfset rtnAction= plugin.component._display(dspFile,lang,app,req)>
	<!--- cfinclude template="#dspFile#"--->
</cfif>

<cfif not hasAction and not hasDisplay>
<cfset printError(struct(message:"there is no action [#url.pluginAction#] or diplay handler [#expandPath(dspFile)#] defined for "&url.plugin,detail:''))>
</cfif>