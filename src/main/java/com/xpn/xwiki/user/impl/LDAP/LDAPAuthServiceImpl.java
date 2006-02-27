/*
 * Copyright 2006, XpertNet SARL, and individual contributors as indicated
 * by the contributors.txt.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 *
 * @author akartmann
 */

package com.xpn.xwiki.user.impl.LDAP;

import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.text.MessageFormat;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.securityfilter.realm.SimplePrincipal;

import com.novell.ldap.LDAPAttribute;
import com.novell.ldap.LDAPAttributeSet;
import com.novell.ldap.LDAPConnection;
import com.novell.ldap.LDAPEntry;
import com.novell.ldap.LDAPException;
import com.novell.ldap.LDAPSearchResults;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.user.impl.xwiki.XWikiAuthServiceImpl;


public class LDAPAuthServiceImpl extends XWikiAuthServiceImpl {
    private static final Log log = LogFactory.getLog(LDAPAuthServiceImpl.class);

    public Principal authenticate(String username, String password, XWikiContext context) throws XWikiException {
        Principal principal = null;

        if ((username==null)||(username.trim().equals("")))
            return null;

        if ((password==null)||(password.trim().equals("")))
            return null;

        String superadmin = "superadmin";
        if (username.equals(superadmin)) {
            String superadminpassword = context.getWiki().Param("xwiki.superadminpassword");
            if ((superadminpassword!=null)&&(superadminpassword.equals(password))) {
                principal = new SimplePrincipal("XWiki.superadmin");
                return principal;
            } else {
                return null;
            }
        }

        // If we have the context then we are using direct mode
        // then we should specify the database
        // This is needed for virtual mode to work
        if (context!=null) {
            String susername = username;
            int i = username.indexOf(".");
            if (i!=-1)
                susername = username.substring(i+1);

           String DN = getLDAP_DN(susername, context);

           if (DN != null && DN.length()!=0)
           {
               if (checkDNPassword(DN, susername, password, context))
               {
                   principal = GetUserPrincipal(susername, context);
               }
           }
           else
            {
               HashMap attributes = new HashMap();
               if (checkUserPassword(susername, password, attributes, context))
               {
                   principal = GetUserPrincipal(susername, context);
                   if (principal == null && attributes.size() > 0)
                   {
                       CreateUserFromLDAP(susername, attributes, context);
                       principal = GetUserPrincipal(susername, context);
                   }
               }
            }
        }
        return principal;
    }

    private void CreateUserFromLDAP(String susername, HashMap attributes, XWikiContext context) throws XWikiException {
        String ldapFieldMapping = getParam("ldap_fields_mapping",context);
        if (ldapFieldMapping != null && ldapFieldMapping.length() > 0)
        {
            String[] fields = ldapFieldMapping.split(",");
            BaseClass bclass = context.getWiki().getUserClass(context);
            BaseObject bobj = new BaseObject();
            bobj.setClassName(bclass.getName());
            String name = null;
            String fullwikiname = null;
            for(int i = 0; i < fields.length; i++ )
            {
                String[] field = fields[i].split("=");
                if (2 == field.length)
                {
                   String fieldName = field[0];
                   if (attributes.containsKey(field[1]))
                   {
                       String fieldValue;
                       fieldValue = (String)attributes.get(field[1]);
                       if (fieldName.equals("name"))
                       {
                           name = fieldValue;
                           fullwikiname = "XWiki." + name;
                           bobj.setName(fullwikiname);
                       }
                       else
                       {
                           bobj.setStringValue(fieldName, fieldValue);
                       }
                   }
                }
            }

            if (name != null && name.length() > 0)
            {
                XWikiDocument doc = context.getWiki().getDocument(fullwikiname, context);
                doc.setParent("");
                doc.addObject(bclass.getName(), bobj);
                doc.setContent("#includeForm(\"XWiki.XWikiUserTemplate\")");

                context.getWiki().ProtectUserPage(context, fullwikiname, "edit", doc);

                context.getWiki().saveDocument(doc, null, context);

                context.getWiki().SetUserDefaultGroup(context, fullwikiname);
            }
        }
    }

    protected Principal GetUserPrincipal(String susername, XWikiContext context) {
        Principal principal = null;

        // First we check in the local database
        try {
            String user = findUser(susername, context);
            if (user!=null) {
                principal = new SimplePrincipal(user);
            }
        } catch (Exception e) {}

        if (context.isVirtual()) {
            if (principal==null) {
                // Then we check in the main database
                String db = context.getDatabase();
                try {
                    context.setDatabase(context.getWiki().getDatabase());
                    try {
                        String user = findUser(susername, context);
                        if (user!=null)
                            principal = new SimplePrincipal(context.getDatabase() + ":" + user);
                    } catch (Exception e) {}
                } finally {
                    context.setDatabase(db);
                }
            }
        }
        return principal;
    }

    public String getLDAP_DN(String susername, XWikiContext context)
    {
        String DN=null;
        if (context!=null) {
            // First we check in the local database
            try {
                String user = findUser(susername, context);
                if (user!=null && user.length()!=0) {
                    DN = readLDAP_DN(user, context);
                }
            } catch (Exception e) {}

            if (context.isVirtual()) {
                if (DN==null || DN.length()==0) {
                    // Then we check in the main database
                    String db = context.getDatabase();
                    try {
                        context.setDatabase(context.getWiki().getDatabase());
                        try {
                            String user = findUser(susername, context);
                            if (user!=null && user.length()!=0)
                                DN = readLDAP_DN(user, context);
                        } catch (Exception e) {}
                    } finally {
                        context.setDatabase(db);
                    }
                }
            }
        }
        return DN;
    }

    private String readLDAP_DN(String username, XWikiContext context) {
        String DN = null;
        try {
            XWikiDocument doc = context.getWiki().getDocument(username, context);
            // We only allow empty password from users having a XWikiUsers object.
            if (doc.getObject("XWiki.XWikiUsers")!=null) {
              DN = doc.getStringValue("XWiki.XWikiUsers", "ldap_dn");
            }

        } catch (Throwable e) {}
        return DN;
    }

    protected boolean checkUserPassword(String username, String password, HashMap attributes, XWikiContext context) throws XWikiException {
        LDAPConnection lc = new LDAPConnection();
        boolean result = false;
        boolean notinLDAP = false;
        String foundDN = null;

        try {
            if (log.isDebugEnabled())
                 log.debug("LDAP Password check for user " + username);

            int ldapPort = getLDAPPort(context);
            int ldapVersion = LDAPConnection.LDAP_V3;
            String ldapHost = getParam("ldap_server", context);
            String bindDNFormat = getParam("ldap_bind_DN",context);
            String bindPasswordFormat = getParam("ldap_bind_pass",context);

            int checkLevel = GetCheckLevel(context);

            Object[] arguments = {
                username,
                password
             };
            String bindDN = MessageFormat.format(bindDNFormat, arguments);
            String bindPassword =  MessageFormat.format(bindPasswordFormat, arguments);

            String baseDN = getParam("ldap_base_DN",context);


            lc.connect( ldapHost, ldapPort );

            if (log.isDebugEnabled())
                 log.debug("LDAP Connect successfull to host " + ldapHost + " and port " + ldapPort );

            // authenticate to the server
            result = Bind(bindDN, bindPassword, lc, ldapVersion);

            if (log.isDebugEnabled())
                 log.debug("LDAP Bind returned");

            if (result && checkLevel > 0)
            {
                if (log.isDebugEnabled())
                     log.debug("LDAP searching user");

                LDAPSearchResults searchResults =
                    lc.search(  baseDN,
                                LDAPConnection.SCOPE_SUB ,
                                "("+ getParam("ldap_UID_attr",context) +
                                   "=" + username + ")",
                                null,          // return all attributes
                                false);        // return attrs and values

                if (searchResults.hasMore())
                {
                    if (log.isDebugEnabled())
                         log.debug("LDAP searching found user");

                    LDAPEntry nextEntry = searchResults.next();
                    foundDN = nextEntry.getDN();

                    if (log.isDebugEnabled())
                         log.debug("LDAP searching found DN: " + foundDN);

                    if (checkLevel > 1)
                    {
                        if (log.isDebugEnabled())
                             log.debug("LDAP comparing password");

                        LDAPAttribute attr = new LDAPAttribute(
                                                        "userPassword", password );
                        result = lc.compare( foundDN, attr );
                    }
                    if (result)
                    {
                        if (log.isDebugEnabled())
                             log.debug("LDAP adding user attributes");

                        LDAPAttributeSet attributeSet = nextEntry.getAttributeSet();
                        Iterator allAttributes = attributeSet.iterator();

                        while(allAttributes.hasNext()) {
                            LDAPAttribute attribute =
                                        (LDAPAttribute)allAttributes.next();
                            String attributeName = attribute.getName();

                            Enumeration allValues = attribute.getStringValues();

                            if( allValues != null) {
                                while(allValues.hasMoreElements()) {
                                    if (log.isDebugEnabled())
                                         log.debug("LDAP adding user attribute " + attributeName);

                                    String Value = (String) allValues.nextElement();
                                    attributes.put(attributeName, Value);
                                }
                            }
                        }
                        attributes.put("dn", foundDN);
                    }
                }
                else {
                    if (log.isDebugEnabled())
                       log.debug("LDAP search user failed");
                    notinLDAP = true;
                }

                if (log.isInfoEnabled()) {
                    if (result)
                     log.info("LDAP Password check for user " + username + " successfull");
                    else
                     log.info("LDAP Password check for user " + username + " failed");
                }
            }
        }
        catch( LDAPException e ) {
            if (log.isInfoEnabled())
                log.info("LDAP Password check for user " + username + " failed with exception " + e.getMessage());

            if ( e.getResultCode() == LDAPException.NO_SUCH_OBJECT ) {
                notinLDAP = true;
            } else if ( e.getResultCode() ==
                                        LDAPException.NO_SUCH_ATTRIBUTE ) {
                notinLDAP = true;
            }
        }
        catch (Throwable e) {
            notinLDAP = true;
            if (log.isErrorEnabled())
                 log.error("LDAP Password check for user " + username + " failed with exception " + e.getMessage());
        }
        finally
        {
            if (log.isDebugEnabled())
                 log.debug("LDAP check in finally block");

            try {
                lc.disconnect();
            } catch (LDAPException e) {
                e.printStackTrace();
            }
        }

        if (notinLDAP)
        {
            if (log.isDebugEnabled())
                 log.debug("LDAP Password check reverting to XWiki");

            // Use XWiki password if user not in LDAP
            result = checkPassword(username, password, context);
            foundDN = null;
        }

        return result;
    }

    private String getParam(String name, XWikiContext context) {
        String param = "";
        try {
         param = context.getWiki().getXWikiPreference(name,context);
        } catch (Exception e) {}
        if (param == null || "".equals(param))
        {
            try{
             param = context.getWiki().Param("xwiki.authentication." + StringUtils.replace(name, "ldap_","ldap."));
            } catch (Exception e) {}
        }
        if (param == null)
            param = "";
        return param;
    }

    protected int GetCheckLevel(XWikiContext context)
    {
        String checkLevel = getParam("ldap_check_level",  context);
        int val = 2;
        if ("1".equals(checkLevel))
            val = 1;
        else if ("0".equals(checkLevel))
            val = 0;
        return val;
    }

    private int getLDAPPort(XWikiContext context) {
        try {
         return context.getWiki().getXWikiPreferenceAsInt("ldap_port", context);
        } catch (Exception e) {
         return (int)context.getWiki().ParamAsLong("xwiki.authentication.ldap.port", LDAPConnection.DEFAULT_PORT);
        }
    }

    protected boolean checkDNPassword(String DN, String username, String password, XWikiContext context) throws XWikiException {
        LDAPConnection lc = new LDAPConnection();
        boolean result = false;
        boolean notinLDAP = false;
        try {

            int ldapPort = getLDAPPort(context);
            int ldapVersion = LDAPConnection.LDAP_V3;
            String ldapHost = getParam("ldap_server", context);
            String bindDN = getParam("ldap_bind_DN",context);
            String bindPassword = getParam("ldap_bind_pass",context);
            String baseDN = getParam("ldap_base_DN",context);

            lc.connect( ldapHost, ldapPort );

            // authenticate to the server
            result = Bind(DN, password, lc, ldapVersion);

            if (log.isDebugEnabled()) {
                if (result)
                 log.debug("(debug) Password check for user " + DN + " successfull");
                else
                 log.debug("(debug) Password check for user " + DN + " failed");
            }
        }
        catch( LDAPException e ) {
            if ( e.getResultCode() == LDAPException.NO_SUCH_OBJECT ) {
                notinLDAP = true;
            } else if ( e.getResultCode() ==
                                        LDAPException.NO_SUCH_ATTRIBUTE ) {
                notinLDAP = true;
            }
        }
        catch (Throwable e) {
            e.printStackTrace();
        }
        finally
        {
            try {
                lc.disconnect();
            } catch (LDAPException e) {
                e.printStackTrace();
            }
        }
        if (notinLDAP)
        {
            // Use XWiki password if user not in LDAP
            result = checkPassword(username, password, context);
        }
        return result;
    }


    private boolean Bind(String bindDN, String bindPassword, LDAPConnection lc, int ldapVersion) throws UnsupportedEncodingException {
        boolean bound = false;
        if (log.isDebugEnabled())
             log.debug("LDAP Bind starting");            

        if (bindDN != null && bindDN.length() > 0 && bindPassword != null)
        {
            try
            {
                lc.bind( ldapVersion, bindDN, bindPassword.getBytes("UTF8") );
                bound = true;

                if (log.isDebugEnabled())
                     log.debug("LDAP Bind successfull");
            }
            catch(LDAPException e) {
                if (log.isErrorEnabled())
                     log.error("LDAP Bind failed with Exception " + e.getMessage());
            };
        } else {
            if (log.isDebugEnabled())
                 log.debug("LDAP Bind does not have binding info");
        }
        return bound;
    }
}
