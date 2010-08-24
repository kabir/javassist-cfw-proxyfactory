/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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
 */
package org.jboss.javassist.classfilewriter.proxyfactory.test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNotSame;
import static junit.framework.Assert.assertSame;
import static junit.framework.Assert.assertTrue;

import java.lang.reflect.Field;

import org.jboss.javassist.classfilewriter.proxyfactory.ProxyFactory;
import org.junit.Test;

/**
 * 
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class BasicProxyFactoryTestCase {
    @Test
    @SuppressWarnings("unchecked")
    public void testProxyFactoryHandlerSetter() throws Exception {
        PrimitiveClass target = new PrimitiveClass();
        HandlerNotCallingTarget<PrimitiveClass> handler = new HandlerNotCallingTarget<PrimitiveClass>(target);
        PrimitiveClass proxy = ProxyFactory.createProxy(PrimitiveClass.class, handler);
        assertNotNull(proxy);
        assertNotSame(PrimitiveClass.class, proxy.getClass());
        
        Field field = proxy.getClass().getDeclaredField("_proxy$Handler");
        field.setAccessible(true);
        Object o = field.get(proxy);
        assertNotNull(o);
        assertTrue(o instanceof HandlerNotCallingTarget);
        assertSame(handler, o);
    }
    
    @Test
    public void testNoArgsVoidMethod() throws Exception {
        PrimitiveClass target = new PrimitiveClass();
        HandlerNotCallingTarget<PrimitiveClass> handler = new HandlerNotCallingTarget<PrimitiveClass>(target);
        PrimitiveClass proxy = ProxyFactory.createProxy(PrimitiveClass.class, handler);
        
        proxy.test();
        assertEquals("test", handler.m.getName());
        assertEquals(0, handler.args.length);
        assertSame(target, handler.instance);
    }
    
    @Test
    public void testOverriddenMethod() throws Exception {
        ChildClass target = new ChildClass();
        HandlerNotCallingTarget<ChildClass> handler = new HandlerNotCallingTarget<ChildClass>(target);
        ChildClass proxy = ProxyFactory.createProxy(ChildClass.class, handler);
        
        Object ret = proxy.overridden(12, 10);
        assertEquals("overridden", handler.m.getName());
        assertEquals(2, handler.args.length);
        assertSame(target, handler.instance);

        assertTrue(ret instanceof String);
        assertEquals("12", ret);
    }
}
