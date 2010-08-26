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
import static junit.framework.Assert.assertFalse;

import org.jboss.javassist.classfilewriter.proxyfactory.ProxyFactory;
import org.jboss.javassist.classfilewriter.proxyfactory.support.BoxedClass;
import org.jboss.javassist.classfilewriter.proxyfactory.support.HandlerCallingTarget;
import org.junit.Test;

/**
 * 
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class BoxedProxyFactoryCalledByHandlerTestCase {
    
    @Test
    public void testBooleanMethod() throws Exception {
        BoxedClass target = new BoxedClass();
        HandlerCallingTarget<BoxedClass> handler = new HandlerCallingTarget<BoxedClass>(target, new Object[] {false});
        BoxedClass proxy = ProxyFactory.createProxy(BoxedClass.class, handler);
        
        assertFalse(proxy.testBoolean(true));
        assertEquals("testBoolean", handler.m.getName());
    }
    
    @Test
    public void testByteMethod() {
        BoxedClass target = new BoxedClass();
        HandlerCallingTarget<BoxedClass> handler = new HandlerCallingTarget<BoxedClass>(target, new Object[] {(byte)6});
        BoxedClass proxy = ProxyFactory.createProxy(BoxedClass.class, handler);
        
        assertEquals((byte)6, proxy.testByte((byte)2).byteValue());
        assertEquals("testByte", handler.m.getName());
    }
    
    @Test
    public void testCharMethod() {
        BoxedClass target = new BoxedClass();
        HandlerCallingTarget<BoxedClass> handler = new HandlerCallingTarget<BoxedClass>(target, new Object[] {'d'});
        BoxedClass proxy = ProxyFactory.createProxy(BoxedClass.class, handler);
        
        assertEquals('d', proxy.testChar('a').charValue());
        assertEquals("testChar", handler.m.getName());
    }
    
    @Test
    public void testDoubleMethod() {
        BoxedClass target = new BoxedClass();
        HandlerCallingTarget<BoxedClass> handler = new HandlerCallingTarget<BoxedClass>(target, new Object[] {765d});
        BoxedClass proxy = ProxyFactory.createProxy(BoxedClass.class, handler);
        
        assertEquals(765d, proxy.testDouble(457.0d).doubleValue());
        assertEquals("testDouble", handler.m.getName());
    }
    
    @Test
    public void testFloatMethod() {
        BoxedClass target = new BoxedClass();
        HandlerCallingTarget<BoxedClass> handler = new HandlerCallingTarget<BoxedClass>(target, new Object[] {8f});
        BoxedClass proxy = ProxyFactory.createProxy(BoxedClass.class, handler);
        
        assertEquals(8f, proxy.testFloat(6.0f).floatValue());
        assertEquals("testFloat", handler.m.getName());
    }
    
    @Test
    public void testIntMethod() {
        BoxedClass target = new BoxedClass();
        HandlerCallingTarget<BoxedClass> handler = new HandlerCallingTarget<BoxedClass>(target, new Object[] {321});
        BoxedClass proxy = ProxyFactory.createProxy(BoxedClass.class, handler);
        
        assertEquals(321, proxy.testInt(123).intValue());
        assertEquals("testInt", handler.m.getName());
    }
    
    @Test
    public void testLongMethod() {
        BoxedClass target = new BoxedClass();
        HandlerCallingTarget<BoxedClass> handler = new HandlerCallingTarget<BoxedClass>(target, new Object[] {99999L});
        BoxedClass proxy = ProxyFactory.createProxy(BoxedClass.class, handler);
        
        assertEquals(99999L, proxy.testLong(12399L).longValue());
        assertEquals("testLong", handler.m.getName());
    }
    
    @Test
    public void testShortMethod() {
        BoxedClass target = new BoxedClass();
        HandlerCallingTarget<BoxedClass> handler = new HandlerCallingTarget<BoxedClass>(target, new Object[] {(short)98});
        BoxedClass proxy = ProxyFactory.createProxy(BoxedClass.class, handler);
        
        assertEquals(98, proxy.testShort((short)78).shortValue());
        assertEquals("testShort", handler.m.getName());
    }
}
