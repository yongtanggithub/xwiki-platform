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
package com.xpn.xwiki.store;

import org.hibernate.jdbc.BatchingBatcherFactory;
import org.hibernate.jdbc.Batcher;
import org.hibernate.jdbc.JDBCContext;
import org.hibernate.jdbc.BatchingBatcher;

/**
 * Created by IntelliJ IDEA.
 * User: ludovic
 * Date: 25 sept. 2005
 * Time: 12:02:10
 * To change this template use File | Settings | File Templates.
 */
public class XWikiBatcherFactory extends BatchingBatcherFactory {
    public Batcher createBatcher(JDBCContext jdbcContext) {
        return new XWikiBatcher( jdbcContext );
    }
}
