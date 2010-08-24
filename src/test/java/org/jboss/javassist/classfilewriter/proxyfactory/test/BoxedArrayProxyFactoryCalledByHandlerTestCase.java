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

import org.jboss.javassist.classfilewriter.proxyfactory.ProxyFactory;
import org.junit.Test;

/**
 * 
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class BoxedArrayProxyFactoryCalledByHandlerTestCase {
    
    @Test
    public void testBooleanMethod() throws Exception {
        BoxedArrayClass target = new BoxedArrayClass();
        Boolean[] b = new Boolean[] {true, false, true};
        HandlerCallingTarget<BoxedArrayClass> handler = new HandlerCallingTarget<BoxedArrayClass>(target, new Object[] {b});
        BoxedArrayClass proxy = ProxyFactory.createProxy(BoxedArrayClass.class, handler);
        
        assertEquals(b, proxy.testBooleanArray(new Boolean[] {true, true}));
        assertEquals("testBooleanArray", handler.m.getName());
    }
    
    @Test
    public void testByteMethod() {
        BoxedArrayClass target = new BoxedArrayClass();
        Byte[] b = new Byte[] {1, 2, 3};
        HandlerCallingTarget<BoxedArrayClass> handler = new HandlerCallingTarget<BoxedArrayClass>(target, new Object[] {b});
        BoxedArrayClass proxy = ProxyFactory.createProxy(BoxedArrayClass.class, handler);
    
        assertEquals(b, proxy.testByteArray(new Byte[] {3, 2, 1}));
        assertEquals("testByteArray", handler.m.getName());
    }
    
    @Test
    public void testCharMethod() {
        BoxedArrayClass target = new BoxedArrayClass();
        Character[] c = new Character[] {'a', 'b'};
        HandlerCallingTarget<BoxedArrayClass> handler = new HandlerCallingTarget<BoxedArrayClass>(target, new Object[] {c});
        BoxedArrayClass proxy = ProxyFactory.createProxy(BoxedArrayClass.class, handler);
        
        assertEquals(c, proxy.testCharArray(new Character[] {'f', 'g'}));
        assertEquals("testCharArray", handler.m.getName());
    }
    
    @Test
    public void testDoubleMethod() {
        BoxedArrayClass target = new BoxedArrayClass();
        Double[] d = new Double[] {1d, 2d};
        HandlerCallingTarget<BoxedArrayClass> handler = new HandlerCallingTarget<BoxedArrayClass>(target, new Object[] {d});
        BoxedArrayClass proxy = ProxyFactory.createProxy(BoxedArrayClass.class, handler);
    
        assertEquals(d, proxy.testDoubleArray(new Double[] {5d, 3d}));
        assertEquals("testDoubleArray", handler.m.getName());
    }
    
    @Test
    public void testFloatMethod() {
        BoxedArrayClass target = new BoxedArrayClass();
        Float[] f = new Float[] {1.0f, 2.0f};
        HandlerCallingTarget<BoxedArrayClass> handler = new HandlerCallingTarget<BoxedArrayClass>(target, new Object[] {f});
        BoxedArrayClass proxy = ProxyFactory.createProxy(BoxedArrayClass.class, handler);
        
        assertEquals(f, proxy.testFloatArray(new Float[] {4f, 5f}));
        assertEquals("testFloatArray", handler.m.getName());
    }
    
    @Test
    public void testIntMethod() {
        BoxedArrayClass target = new BoxedArrayClass();
        Integer[] i = new Integer[] {1, 2};
        HandlerCallingTarget<BoxedArrayClass> handler = new HandlerCallingTarget<BoxedArrayClass>(target, new Object[] {i});
        BoxedArrayClass proxy = ProxyFactory.createProxy(BoxedArrayClass.class, handler);
    
        assertEquals(i, proxy.testIntArray(new Integer[] {5, 6}));
        assertEquals("testIntArray", handler.m.getName());
    }
    
    @Test
    public void testLongMethod() {
        BoxedArrayClass target = new BoxedArrayClass();
        Long[] l = new Long[] {123L, 234L};
        HandlerCallingTarget<BoxedArrayClass> handler = new HandlerCallingTarget<BoxedArrayClass>(target, new Object[] {l});
        BoxedArrayClass proxy = ProxyFactory.createProxy(BoxedArrayClass.class, handler);
     
        assertEquals(l, proxy.testLongArray(new Long[] {555L, 444L}));
        assertEquals("testLongArray", handler.m.getName());
    }
    
    @Test
    public void testShortMethod() {
        BoxedArrayClass target = new BoxedArrayClass();
        Short[] s = new Short[] {3, 4};
        HandlerCallingTarget<BoxedArrayClass> handler = new HandlerCallingTarget<BoxedArrayClass>(target, new Object[] {s});
        BoxedArrayClass proxy = ProxyFactory.createProxy(BoxedArrayClass.class, handler);
     
        assertEquals(s, proxy.testShortArray(new Short[] {4, 5}));
        assertEquals("testShortArray", handler.m.getName());
    }
}
