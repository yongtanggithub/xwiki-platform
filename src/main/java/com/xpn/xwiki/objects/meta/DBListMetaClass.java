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
 * @author ludovic
 */

package com.xpn.xwiki.objects.meta;

import com.xpn.xwiki.objects.BaseCollection;
import com.xpn.xwiki.objects.classes.DBListClass;
import com.xpn.xwiki.objects.classes.TextAreaClass;
import com.xpn.xwiki.XWikiContext;

public class DBListMetaClass extends ListMetaClass {

    public DBListMetaClass() {
        super();
        setPrettyName("Database List Class");
        setName(DBListClass.class.getName());

        TextAreaClass sql_class = new TextAreaClass(this);
        sql_class.setName("sql");
        sql_class.setPrettyName("Hibernate Query");
        sql_class.setSize(80);
        sql_class.setRows(5);
        safeput("sql", sql_class);
    }

    public BaseCollection newObject(XWikiContext context) {
        return new DBListClass();
    }
}

