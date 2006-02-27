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
 * @author vmassol
 * @author sdumitriu
 */


package com.xpn.xwiki.test;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.IntegerProperty;
import com.xpn.xwiki.objects.ListProperty;
import com.xpn.xwiki.objects.StringProperty;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.NumberClass;



public class ObjectTest extends TestCase {

    public XWikiContext context = new XWikiContext();

    public void testEqualsNumberProperty() {
         // Test both cloning and equals
         IntegerProperty prop = Utils.prepareIntegerProperty();
         IntegerProperty prop2 = (IntegerProperty) prop.clone();

         // test cloning and equals at the same time
         assertTrue("Cloning did not created equals integer property", prop.equals(prop2));

         // Test name (this is common to all elements)
         prop2.setName("toto");
         assertTrue("Equals did not detect different name", !prop.equals(prop2));
         prop2 = (IntegerProperty) prop.clone();
         prop2.setName(null);
         assertTrue("Equals did not detect different name with null", !prop.equals(prop2));
         prop.setName(null);
         assertTrue("Equals did not detect same null name", prop.equals(prop2));

         // Test pretty name (this is common to all elements)
         prop2 = (IntegerProperty) prop.clone();
         prop2.setPrettyName("toto");
         assertTrue("Equals did not detect different pretty name", !prop.equals(prop2));
         prop2 = (IntegerProperty) prop.clone();
         prop2.setPrettyName(null);
         assertTrue("Equals did not detect different pretty name with null", !prop.equals(prop2));
         prop.setPrettyName(null);
         assertTrue("Equals did not detect same null pretty name", prop.equals(prop2));

         // Test value (this is integer specific)
         prop2 = (IntegerProperty) prop.clone();
         prop2.setValue(new Integer(1000));
         assertTrue("Equals did not detect different value", !prop.equals(prop2));
         prop2 = (IntegerProperty) prop.clone();
         prop2.setValue(null);
         assertTrue("Equals did not detect different value with null", !prop.equals(prop2));
         prop.setValue(null);
         assertTrue("Equals did not detect same null value", prop.equals(prop2));
     }

     public void testEqualsStringProperty() {
         // Test both cloning and equals
         IntegerProperty iprop = new IntegerProperty();
         StringProperty sprop = new StringProperty();
         assertNotSame("IntegerProperty cannot be equals to StringProperty", iprop, sprop);

         StringProperty prop = Utils.prepareStringProperty();
         StringProperty prop2 = (StringProperty) prop.clone();

         // test cloning and equals at the same time
         assertTrue("Cloning did not created equals string property", prop.equals(prop2));

         // Test value (this is string specific)
         prop2 = (StringProperty) prop.clone();
         prop2.setValue("blabla");
         assertTrue("Equals did not detect different value", !prop.equals(prop2));
         prop2 = (StringProperty) prop.clone();
         prop2.setValue(null);
         assertTrue("Equals did not detect different value with null", !prop.equals(prop2));
         prop.setValue(null);
         assertTrue("Equals did not detect same null value", prop.equals(prop2));
     }

    public void testEqualsListProperty() {
          // Test both cloning and equals
          ListProperty iprop = new ListProperty();
          StringProperty sprop = new StringProperty();
          assertNotSame("ListProperty cannot be equals to StringProperty", iprop, sprop);

          ListProperty prop = Utils.prepareStringListProperty();
          ListProperty prop2 = (ListProperty) prop.clone();

          // test cloning and equals at the same time
          assertEquals("Cloning did not created equals list property", prop, prop2);

          // Test value (this is string specific)
          prop2 = (ListProperty) prop.clone();
          List list = new ArrayList();
          list.add("1");
          list.add("2");
          prop2.setValue(list);
          assertNotSame("Equals did not detect different value", prop, prop2);
          prop2 = (ListProperty) prop.clone();
          prop2.setValue(null);
          assertNotSame("Equals did not detect different value with null", prop, prop2);
          prop.setValue(null);
          assertEquals("Equals did not detect same null value", prop, prop2);
      }

    public void testEqualsObject() throws XWikiException {
        XWikiDocument doc = new XWikiDocument();
        Utils.prepareObject(doc);
        BaseClass bclass = doc.getxWikiClass();
        BaseObject object = doc.getObject(bclass.getName(), 0);
        testEqualsObject(object, bclass);
    }

    public void testEqualsAdvancedObject() throws XWikiException {
        XWikiDocument doc = new XWikiDocument();
        Utils.prepareAdvancedObject(doc);
        BaseClass bclass = doc.getxWikiClass();
        BaseObject object = doc.getObject(bclass.getName(), 0);
        testEqualsObject(object, bclass);

        BaseObject object2 = (BaseObject)object.clone();
        ((StringProperty)object.safeget("category")).setValue(null);
        assertNotSame("Equals did not detect different List field value", object, object2);
    }

     public void testEqualsObject(BaseObject object, BaseClass bclass) throws XWikiException {
         BaseObject object2 = (BaseObject)object.clone();

         // test cloning and equals at the same time
         assertEquals("Cloning did not created equals objects", object, object2);

         object2.setName("titi");
         assertNotSame("Equals did not detect different object name", object, object2);

         object2 = (BaseObject)object.clone();
         ((BaseProperty)object.safeget("age")).setName("titi");
         assertNotSame("Equals did not detect changed field property", object, object2);

         object2 = (BaseObject)object.clone();
         ((BaseProperty)object.safeget("age")).setValue(new Integer(45));
         assertNotSame("Equals did not detect changed field property", object, object2);

         object2 = (BaseObject)object.clone();
         object.removeField("age");
         assertNotSame("Equals did not detect missing age field", object, object2);

         object2 = (BaseObject)object.clone();
         bclass.safeget("age").setName("titi");
         assertNotSame("Equals did not detect different class property name", object, object2);

         object2 = (BaseObject)object.clone();
         ((NumberClass)bclass.safeget("age")).setSize(1);
         assertNotSame("Equals did not detect different class property size", object, object2);

         object2 = (BaseObject)object.clone();
         bclass.removeField("age");
         assertNotSame("Equals did not detect different null class", object, object2);

         object2 = (BaseObject)object.clone();
         object.setClassName(null);
         assertNotSame("Equals did not detect different null class", object, object2);
     }

     public void testMergeObject() throws XWikiException {
         XWikiDocument doc = new XWikiDocument();
         Utils.prepareObject(doc);
         BaseObject object = doc.getObject(doc.getxWikiClass().getName(), 0);
         BaseObject object2 = (BaseObject)object.clone();

         object2.merge(object);
         // test merging and equals at the same time
         assertEquals("Merging did not created equals objects", object, object2);

         object2 = (BaseObject)object.clone();
         object2.removeField("age");
         object2.merge(object);
         assertEquals("Merging did not created equals objects", object, object2);

         object2 = (BaseObject)object.clone();
         ((BaseProperty)object2.safeget("age")).setName("titi");
         object2.merge(object);
         assertNotSame("Merging overrided age property", object, object2);

     }

    public void testSetObject() throws XWikiException {
        XWikiDocument doc = new XWikiDocument("Test", "WebHome");
        BaseObject bobj = new BaseObject();
        bobj.setName("Test.WebHome");
        doc.setObject("Test.WebHome", 0, bobj);
        assertEquals("Object size should be 1", 1, doc.getObjects("Test.WebHome").size());
        BaseObject bobj2 = doc.getObject("Test.WebHome", 0);
        assertEquals("Object name should be Test.WebHome", "Test.WebHome", bobj2.getName());

        BaseObject bobj3 = new BaseObject();
        bobj3.setName("Test.Toto");
        doc.setObject("Test.WebHome", 0, bobj3);
        assertEquals("Object size should be 1", 1, doc.getObjects("Test.WebHome").size());
        BaseObject bobj3b = doc.getObject("Test.WebHome", 0);
        assertEquals("Object name should be Test.Toto", "Test.Toto", bobj3b.getName());

        BaseObject bobj4 = new BaseObject();
        bobj4.setName("Test.WebHome");
        doc.setObject("Test.WebHome", 2, bobj4);
        assertEquals("Object size should be 3", 3, doc.getObjects("Test.WebHome").size());
        assertNull("Object 1 should be null", doc.getObject("Test.WebHome",1));
        BaseObject obj5 = doc.getObject("Test.WebHome", 2);
        assertEquals("Object name should be Test.WebHome", "Test.WebHome", obj5.getName());
    }

}
