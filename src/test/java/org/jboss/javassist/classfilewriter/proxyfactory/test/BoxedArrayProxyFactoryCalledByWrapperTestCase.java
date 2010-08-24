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
import static junit.framework.Assert.assertSame;

import org.jboss.javassist.classfilewriter.proxyfactory.ProxyFactory;
import org.junit.Test;

/**
 * 
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class BoxedArrayProxyFactoryCalledByWrapperTestCase {
    
    @Test
    public void testBooleanMethod() throws Exception {
        BoxedArrayClass target = new BoxedArrayClass();
        HandlerNotCallingTarget<BoxedArrayClass> handler = new HandlerNotCallingTarget<BoxedArrayClass>(target);
        BoxedArrayClass proxy = ProxyFactory.createProxy(BoxedArrayClass.class, handler);
        
        Boolean[] b = new Boolean[] {true, false, true};
        assertEquals(b, proxy.testBooleanArray(b));
        assertEquals("testBooleanArray", handler.m.getName());
        assertEquals(1, handler.args.length);
        assertEquals(b, handler.args[0]);
        assertSame(target, handler.instance);
    }
    
    @Test
    public void testByteMethod() {
        BoxedArrayClass target = new BoxedArrayClass();
        HandlerNotCallingTarget<BoxedArrayClass> handler = new HandlerNotCallingTarget<BoxedArrayClass>(target);
        BoxedArrayClass proxy = ProxyFactory.createProxy(BoxedArrayClass.class, handler);
    
        Byte[] b = new Byte[] {1, 2, 3};
        assertEquals(b, proxy.testByteArray(b));
        assertEquals("testByteArray", handler.m.getName());
        assertEquals(1, handler.args.length);
        assertEquals(b, handler.args[0]);
        assertSame(target, handler.instance);
    }
    
    @Test
    public void testCharMethod() {
        BoxedArrayClass target = new BoxedArrayClass();
        HandlerNotCallingTarget<BoxedArrayClass> handler = new HandlerNotCallingTarget<BoxedArrayClass>(target);
        BoxedArrayClass proxy = ProxyFactory.createProxy(BoxedArrayClass.class, handler);
        
        Character[] c = new Character[] {'a', 'b'};
        assertEquals(c, proxy.testCharArray(c));
        assertEquals("testCharArray", handler.m.getName());
        assertEquals(1, handler.args.length);
        assertEquals(c, handler.args[0]);
        assertSame(target, handler.instance);
    }
    
    @Test
    public void testDoubleMethod() {
        BoxedArrayClass target = new BoxedArrayClass();
        HandlerNotCallingTarget<BoxedArrayClass> handler = new HandlerNotCallingTarget<BoxedArrayClass>(target);
        BoxedArrayClass proxy = ProxyFactory.createProxy(BoxedArrayClass.class, handler);
    
        Double[] d = new Double[] {1d, 2d};
        assertEquals(d, proxy.testDoubleArray(d));
        assertEquals("testDoubleArray", handler.m.getName());
        assertEquals(1, handler.args.length);
        assertEquals(d, handler.args[0]);
        assertSame(target, handler.instance);
    }
    
    @Test
    public void testFloatMethod() {
        BoxedArrayClass target = new BoxedArrayClass();
        HandlerNotCallingTarget<BoxedArrayClass> handler = new HandlerNotCallingTarget<BoxedArrayClass>(target);
        BoxedArrayClass proxy = ProxyFactory.createProxy(BoxedArrayClass.class, handler);
        
        Float[] f = new Float[] {1.0f, 2.0f};
        assertEquals(f, proxy.testFloatArray(f));
        assertEquals("testFloatArray", handler.m.getName());
        assertEquals(1, handler.args.length);
        assertEquals(f, handler.args[0]);
        assertSame(target, handler.instance);
    }
    
    @Test
    public void testIntMethod() {
        BoxedArrayClass target = new BoxedArrayClass();
        HandlerNotCallingTarget<BoxedArrayClass> handler = new HandlerNotCallingTarget<BoxedArrayClass>(target);
        BoxedArrayClass proxy = ProxyFactory.createProxy(BoxedArrayClass.class, handler);
    
        Integer[] i = new Integer[] {1, 2};
        assertEquals(i, proxy.testIntArray(i));
        assertEquals("testIntArray", handler.m.getName());
        assertEquals(1, handler.args.length);
        assertEquals(i, handler.args[0]);
        assertSame(target, handler.instance);
    }
    
    @Test
    public void testLongMethod() {
        BoxedArrayClass target = new BoxedArrayClass();
        HandlerNotCallingTarget<BoxedArrayClass> handler = new HandlerNotCallingTarget<BoxedArrayClass>(target);
        BoxedArrayClass proxy = ProxyFactory.createProxy(BoxedArrayClass.class, handler);
     
        Long[] l = new Long[] {123L, 234L};
        assertEquals(l, proxy.testLongArray(l));
        assertEquals("testLongArray", handler.m.getName());
        assertEquals(1, handler.args.length);
        assertEquals(l, handler.args[0]);
        assertSame(target, handler.instance);
    }
    
    @Test
    public void testShortMethod() {
        BoxedArrayClass target = new BoxedArrayClass();
        HandlerNotCallingTarget<BoxedArrayClass> handler = new HandlerNotCallingTarget<BoxedArrayClass>(target);
        BoxedArrayClass proxy = ProxyFactory.createProxy(BoxedArrayClass.class, handler);
     
        Short[] s = new Short[] {3, 4};
        assertEquals(s, proxy.testShortArray(s));
        assertEquals("testShortArray", handler.m.getName());
        assertEquals(1, handler.args.length);
        assertEquals(s, handler.args[0]);
        assertSame(target, handler.instance);
    }
}
