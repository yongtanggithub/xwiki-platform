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
 * @author erwan
 * @author sdumitriu
 */

package com.xpn.xwiki.objects.classes;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Arrays;

import org.apache.commons.lang.StringUtils;
import org.apache.ecs.xhtml.input;
import org.apache.ecs.xhtml.option;
import org.apache.ecs.xhtml.select;
import org.dom4j.Element;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.objects.BaseCollection;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.DBStringListProperty;
import com.xpn.xwiki.objects.ListProperty;
import com.xpn.xwiki.objects.StringListProperty;
import com.xpn.xwiki.objects.StringProperty;
import com.xpn.xwiki.objects.meta.PropertyMetaClass;

public abstract class ListClass extends PropertyClass {

    public ListClass(String name, String prettyname, PropertyMetaClass wclass) {
        super(name, prettyname, wclass);
        setRelationalStorage(false);
        setDisplayType("select");
        setMultiSelect(false);
        setSize(1);
    }


    public ListClass(PropertyMetaClass wclass) {
        this("list", "List", wclass);
    }

    public ListClass() {
        this(null);
    }

    public String getDisplayType() {
        return getStringValue("displayType");
    }

    public void setDisplayType(String type) {
        setStringValue("displayType", type);
    }

    public int getSize() {
        return getIntValue("size");
    }

    public void setSize(int size) {
        setIntValue("size", size);
    }

    public boolean isMultiSelect() {
        return (getIntValue("multiSelect")==1);
    }

    public void setMultiSelect(boolean multiSelect) {
        setIntValue("multiSelect", multiSelect ? 1 : 0);
    }

    public boolean isRelationalStorage() {
        return (getIntValue("relationalStorage")==1);
    }

    public void setRelationalStorage(boolean storage) {
        setIntValue("relationalStorage", storage ? 1 : 0);
    }

    public static List getListFromString(String value) {
        List list = new ArrayList();
        if (value==null)
            return list;

        String val = StringUtils.replace(value, "\\|", "%PIPE%");
        String[] result = StringUtils.split(value,"|");
        for (int i=0;i<result.length;i++)
            list.add(StringUtils.replace(result[i],"%PIPE%", "|"));
        return list;
    }

    public BaseProperty newProperty() {
        BaseProperty lprop;

        if (isRelationalStorage()&&isMultiSelect())
            lprop = new DBStringListProperty();
        else if (isMultiSelect())
            lprop = new StringListProperty();
        else
            lprop = new StringProperty();

        if (isMultiSelect() && getDisplayType().equals("input")) {
            ((ListProperty)lprop).setFormStringSeparator(" ");
        }


        return lprop;
    }

    public BaseProperty fromString(String value) {
        BaseProperty prop = newProperty();
        if (isMultiSelect()) {
          if (!getDisplayType().equals("input")) {
            ((ListProperty)prop).setList(getListFromString(value));
          } else {
            ((ListProperty)prop).setList(Arrays.asList(StringUtils.split(value," ,|")));
          }
        } else
            prop.setValue(value);
        return prop;
    }

    public BaseProperty fromStringArray(String[] strings) {
        if ((!isMultiSelect())||(strings.length==1))
            return fromString(strings[0]);
        else {
            List list = new ArrayList();
            for (int i=0;i<strings.length;i++)
                list.add(strings[i]);
            BaseProperty prop = newProperty();
            ((ListProperty)prop).setList(list);
            return prop;
        }
    }


    public BaseProperty newPropertyfromXML(Element ppcel) {
        if ((!isRelationalStorage())&&(!isMultiSelect()))
            return super.newPropertyfromXML(ppcel);

        List elist = ppcel.elements("value");
        BaseProperty lprop = (BaseProperty)newProperty();


        if (isMultiSelect()) {
            List llist = ((ListProperty)lprop).getList();
            for (int i=0;i<elist.size();i++) {
                Element el = (Element) elist.get(i);
                llist.add(el.getText());
            }
        }
        else {
            for (int i=0;i<elist.size();i++) {
                Element el = (Element) elist.get(i);
                ((StringProperty)lprop).setValue(el.getText());
            }
        }
        return lprop;
    }


    public void displayHidden(StringBuffer buffer, String name, String prefix, BaseCollection object, XWikiContext context) {
        input input = new input();
        BaseProperty prop = (BaseProperty) object.safeget(name);
        if (prop!=null) input.setValue(prop.toFormString());

        input.setType("hidden");
        input.setName(prefix + name);
        buffer.append(input.toString());
    }

    public void displayView(StringBuffer buffer, String name, String prefix, BaseCollection object, XWikiContext context) {
        List selectlist;
        BaseProperty prop =  (BaseProperty)object.safeget(name);
        if ((prop instanceof ListProperty)||(prop instanceof DBStringListProperty)) {
            selectlist = (List) prop.getValue();
            buffer.append(StringUtils.join(selectlist.toArray(), " "));
        } else {
            buffer.append(prop.getValue().toString());
        }
    }

    public void displayEdit(StringBuffer buffer, String name, String prefix, BaseCollection object, XWikiContext context) {
        if (getDisplayType().equals("input")) {
            input input = new input();
            BaseProperty prop = (BaseProperty) object.safeget(name);
            if (prop!=null) input.setValue(prop.toFormString());
            input.setType("text");
            input.setSize(60);
            input.setName(prefix + name);
            buffer.append(input.toString());
        } else {
            select select = new select(prefix + name, 1);
            select.setMultiple(isMultiSelect());
            select.setSize(getSize());

            List list = getList(context);
            List selectlist;

            BaseProperty prop =  (BaseProperty)object.safeget(name);
            if (prop==null) {
                selectlist = new ArrayList();
            } else if ((prop instanceof ListProperty)||(prop instanceof DBStringListProperty)) {
                selectlist = (List) prop.getValue();
            } else {
                selectlist = new ArrayList();
                selectlist.add(prop.getValue());
            }

            // Add options from Set
            for (Iterator it=list.iterator();it.hasNext();) {
                String value = it.next().toString();
                option option = new option(value, value);
                option.addElement(value);
                if (selectlist.contains(value))
                    option.setSelected(true);
                select.addElement(option);
            }

            buffer.append(select.toString());
        }
    }

    public abstract List getList(XWikiContext context);


}
