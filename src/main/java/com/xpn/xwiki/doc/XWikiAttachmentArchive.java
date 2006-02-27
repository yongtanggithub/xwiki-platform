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
 * @author sdumitriu
 */

package com.xpn.xwiki.doc;

import java.io.ByteArrayInputStream;
import java.util.Date;

import org.apache.commons.jrcs.rcs.Archive;
import org.apache.commons.jrcs.rcs.Lines;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;

public class XWikiAttachmentArchive {

    private XWikiAttachment attachment;

    public long getId() {
        return attachment.getId();
    }

    public void setId(long id) {
    }

    public Object clone() {
        XWikiAttachmentArchive attachmentarchive = null;
        try {
            attachmentarchive = (XWikiAttachmentArchive) getClass().newInstance();
        } catch (Exception e) {
            // This should not happen
        }

        attachmentarchive.setAttachment(getAttachment());
        attachmentarchive.setRCSArchive(getRCSArchive());
        return attachmentarchive;
    }

    // Document Archive
    private Archive archive;

    public Archive getRCSArchive() {
        return archive;
    }

    public void setRCSArchive(Archive archive) {
        this.archive = archive;
    }

    public byte[] getArchive() throws XWikiException {
     return getArchive(null);      
    }

    public byte[] getArchive(XWikiContext context) throws XWikiException {
        if (archive==null)
            updateArchive(attachment.getContent(context));
        if (archive==null)
            return new byte[0];
        else {
            return archive.toByteArray();
        }
    }

    public void setArchive(byte[] data) throws XWikiException {
        if (data==null) {
            archive = null;

        } else {
            try {
                ByteArrayInputStream is = new ByteArrayInputStream(data);
                archive = new Archive(getAttachment().getFilename(), is);
            }
            catch (Exception e) {
                Object[] args = { getAttachment().getFilename() };
                throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_ATTACHMENT_ARCHIVEFORMAT,
                        "Exception while manipulating the archive for file {0}", e, args);
            }
        }
    }

    public void updateArchive(byte[] data) throws XWikiException {
        try {
            String sdata = data.toString();
            Lines lines = new Lines(sdata);

            if (archive!=null) {
                archive.addRevision(lines.toArray(),"");
                attachment.incrementVersion();
                attachment.setDate(new Date());
            }
            else
                archive = new Archive(lines.toArray(),getAttachment().getFilename(),getAttachment().getVersion());
        }
        catch (Exception e) {
            Object[] args = { getAttachment().getFilename() };
            throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_ATTACHMENT_ARCHIVEFORMAT,
                    "Exception while manipulating the archive for file {0}", e, args);
        }
    }

    public XWikiAttachment getAttachment() {
        return attachment;
    }

    public void setAttachment(XWikiAttachment attachment) {
        this.attachment = attachment;
    }
}
