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
 * @author amelentev
 */

package com.xpn.xwiki.plugin.query;

import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.core.query.QueryParser;
import org.apache.jackrabbit.core.query.QueryRootNode;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.api.Api;
import com.xpn.xwiki.cache.api.XWikiCache;
import com.xpn.xwiki.plugin.XWikiDefaultPlugin;
import com.xpn.xwiki.plugin.XWikiPluginInterface;
import com.xpn.xwiki.store.XWikiStoreInterface;

/** Plugin for Query API */
public class QueryPlugin extends XWikiDefaultPlugin implements IQueryFactory {
	private static final Log log = LogFactory.getLog(QueryPlugin.class);
	XWikiCache		cache;
	XWikiContext	context;
	public QueryPlugin(String name, String className, XWikiContext context) {
		super(name, className, context);
		this.context = context;
		this.cache = this.context.getWiki().getCacheService().newCache();		
	}
	
	public String getName() { return "query"; }

	QueryPluginApi queryApi;
	public Api getPluginApi(XWikiPluginInterface plugin, XWikiContext context) {
		if (queryApi == null)
			queryApi = new QueryPluginApi(this);
		return queryApi;
	}
	
	public XWikiContext getContext() {		
		return context;
	}	
	public XWikiStoreInterface getStore() {
		return getContext().getWiki().getStore();
	}
	
	public XWikiCache getCache() {		
		return cache;
	}
	
	boolean isHibernate() {
		return getContext().getWiki().getHibernateStore() != null; 
	}	

	/** Translate query string to query tree */
	protected QueryRootNode parse(String query, String language) throws InvalidQueryException {
		if (query==null) return null;
		final QueryRootNode qn = QueryParser.parse(query, language, XWikiNamespaceResolver.getInstance());			
		return qn;		
	}
	/** create xpath query */
	public IQuery xpath(String q) throws InvalidQueryException {
		if (log.isDebugEnabled())
			log.debug("create xpath query: "+q);
		if (isHibernate())
			return new HibernateQuery( parse(q, Query.XPATH), this);
		return null;
	}
	/** create JCRSQL query 
	 * unsupported for now */
	public IQuery ql(String q) throws InvalidQueryException {
		if (log.isDebugEnabled())
			log.debug("create JCRSQL query: "+q);
		if (isHibernate())
			return new HibernateQuery( parse(q, Query.SQL), this);
		return null;
	}
	/** create query for docs 
	 * @param web, docname - document.web & .name. it may consist xpath []-selection. if any - *
	 * @param prop - return property, start with @, if null - return document
	 * @param order - properties for sort, separated by ','; order: ascending/descending after prop. name, or +/- before. if null - not sort 
	 * @throws InvalidQueryException 
	 * */
	public IQuery getDocs(String docname, String prop, String order) throws InvalidQueryException {
		return xpath("/"+getXPathName(docname) + getPropertyXPath(prop) + getOrderXPath(order));
	}
	/** create query for child documents
	 * @throws InvalidQueryException
	 * @param web,docname must be without templates & [] select    
	 * @see getDocs */
	public IQuery getChildDocs(String docname, String prop, String order) throws InvalidQueryException {
		return xpath("/*/*[@parent='"+getXWikiName(docname)+"']"+ getPropertyXPath(prop) + getOrderXPath(order));
	}
	/** create query for attachments
	 * @param attachname - name of attachment, may be *, *[] 
	 * @see getDocs
	 * @throws InvalidQueryException
	 */
	public IQuery getAttachment(String docname, String attachname, String order) throws InvalidQueryException {
		return xpath("/"+getXPathName(docname)+"/attach/" + attachname + getOrderXPath(order));
	}
	/** create query for objects
	 * @param oweb, oclass - object web & class. if any - *
	 * @param prop. for flex-attributes use f:flexname 
	 * @see getDocs
	 * @throws InvalidQueryException
	 */
	public IQuery getObjects(String docname, String oclass, String prop, String order) throws InvalidQueryException {
		return xpath("/"+getXPathName(docname)+"/obj/"+getXPathName(oclass) + getPropertyXPath(prop) + getOrderXPath(order));
	}
	
	protected String getXWikiName(String name) {
		int is = name.indexOf('['),
		ip = name.indexOf('/');
		if (is<0 || ip < is)
			return name.replace('/', '.');
		return name;
	}
	protected String getXPathName(String name) {
		int is = name.indexOf('['),
			ip = name.indexOf('.');
		if (is<0 || ip < is)
			return name.replace('.', '/');
		return name;
	}
	
	protected String getOrderXPath(String order) {
		if ("".equals(n2e(order))) return "";
		final String[] props = StringUtils.split(order,',');		
		StringBuffer res = new StringBuffer();
		res.append(" order by ");
		String comma = "";
		for (int i=0; i<props.length; i++) {
			res.append(comma);
			String prop = props[i].trim();
			char c = prop.charAt(0);
			boolean descending = (c == '-');			
			if (c=='-' || c=='+')
				prop = prop.substring(1);
			c = prop.charAt(0);
			if (c!='@')
				res.append("@");
			res.append(prop);
			if (descending)
				res.append(" descending");
			comma = ",";
		}
		return res.toString(); 
	}
	protected String getPropertyXPath(String prop) {
		if ("".equals(n2e(prop))) return "";
		prop = prop.trim();
		if (prop.charAt(0)=='(') return "/"+prop;
		
		final StringBuffer sb = new StringBuffer();
		final String[] props = StringUtils.split(prop, " ,");
		sb.append("/");
		if (props.length>1)
			sb.append("(");
		String comma = "";
		for (int i=0; i<props.length; i++) {
			final String p = props[i];
			sb.append(comma);
			if (p.charAt(0)!='@')
				sb.append("@");
			sb.append(p);
			comma = ",";
		}
		if (props.length>1)
			sb.append(")");
		
		return sb.toString();
	}
	private final String n2e(String s) {
		return s==null?"":s;
	}	
}
