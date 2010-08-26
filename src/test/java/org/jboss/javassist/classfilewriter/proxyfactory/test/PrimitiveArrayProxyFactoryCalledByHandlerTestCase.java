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
import org.jboss.javassist.classfilewriter.proxyfactory.support.HandlerCallingTarget;
import org.jboss.javassist.classfilewriter.proxyfactory.support.PrimitiveArrayClass;
import org.junit.Test;

/**
 * 
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class PrimitiveArrayProxyFactoryCalledByHandlerTestCase {
    
    @Test
    public void testBooleanMethod() throws Exception {
        PrimitiveArrayClass target = new PrimitiveArrayClass();
        boolean[] b = new boolean[] {true, false, true};
        HandlerCallingTarget<PrimitiveArrayClass> handler = new HandlerCallingTarget<PrimitiveArrayClass>(target, new Object[] {b});
        PrimitiveArrayClass proxy = ProxyFactory.createProxy(PrimitiveArrayClass.class, handler);
        
        assertEquals(b, proxy.testBooleanArray(new boolean[] {true, true}));
        assertEquals("testBooleanArray", handler.m.getName());
    }
    
    @Test
    public void testByteMethod() {
        PrimitiveArrayClass target = new PrimitiveArrayClass();
        byte[] b = new byte[] {1, 2, 3};
        HandlerCallingTarget<PrimitiveArrayClass> handler = new HandlerCallingTarget<PrimitiveArrayClass>(target, new Object[] {b});
        PrimitiveArrayClass proxy = ProxyFactory.createProxy(PrimitiveArrayClass.class, handler);
    
        assertEquals(b, proxy.testByteArray(new byte[] {5,6,7}));
        assertEquals("testByteArray", handler.m.getName());
    }
    
    @Test
    public void testCharMethod() {
        PrimitiveArrayClass target = new PrimitiveArrayClass();
        char[] c = new char[] {'a', 'b'};
        HandlerCallingTarget<PrimitiveArrayClass> handler = new HandlerCallingTarget<PrimitiveArrayClass>(target, new Object[] {c});
        PrimitiveArrayClass proxy = ProxyFactory.createProxy(PrimitiveArrayClass.class, handler);
        
        assertEquals(c, proxy.testCharArray(new char[] {'d', 'e'}));
        assertEquals("testCharArray", handler.m.getName());
    }
    
    @Test
    public void testDoubleMethod() {
        PrimitiveArrayClass target = new PrimitiveArrayClass();
        double[] d = new double[] {1d, 2d};
        HandlerCallingTarget<PrimitiveArrayClass> handler = new HandlerCallingTarget<PrimitiveArrayClass>(target, new Object[] {d});
        PrimitiveArrayClass proxy = ProxyFactory.createProxy(PrimitiveArrayClass.class, handler);
    
        
        assertEquals(d, proxy.testDoubleArray(new double[] {5d, 6d}));
        assertEquals("testDoubleArray", handler.m.getName());
    }
    
    @Test
    public void testFloatMethod() {
        PrimitiveArrayClass target = new PrimitiveArrayClass();
        float[] f = new float[] {1.0f, 2.0f};
        HandlerCallingTarget<PrimitiveArrayClass> handler = new HandlerCallingTarget<PrimitiveArrayClass>(target, new Object[] {f});
        PrimitiveArrayClass proxy = ProxyFactory.createProxy(PrimitiveArrayClass.class, handler);
        
        assertEquals(f, proxy.testFloatArray(new float[] {6.f}));
        assertEquals("testFloatArray", handler.m.getName());
    }
    
    @Test
    public void testIntMethod() {
        PrimitiveArrayClass target = new PrimitiveArrayClass();
        int[] i = new int[] {1, 2};
        HandlerCallingTarget<PrimitiveArrayClass> handler = new HandlerCallingTarget<PrimitiveArrayClass>(target, new Object[] {i});
        PrimitiveArrayClass proxy = ProxyFactory.createProxy(PrimitiveArrayClass.class, handler);
    
        assertEquals(i, proxy.testIntArray(new int[] {6, 7, 8}));
        assertEquals("testIntArray", handler.m.getName());
    }
    
    @Test
    public void testLongMethod() {
        PrimitiveArrayClass target = new PrimitiveArrayClass();
        long[] l = new long[] {123L, 234L};
        HandlerCallingTarget<PrimitiveArrayClass> handler = new HandlerCallingTarget<PrimitiveArrayClass>(target, new Object[] {l});
        PrimitiveArrayClass proxy = ProxyFactory.createProxy(PrimitiveArrayClass.class, handler);
     
        assertEquals(l, proxy.testLongArray(new long[] {8777L}));
        assertEquals("testLongArray", handler.m.getName());
    }
    
    @Test
    public void testShortMethod() {
        PrimitiveArrayClass target = new PrimitiveArrayClass();
        short[] s = new short[] {3, 4};
        HandlerCallingTarget<PrimitiveArrayClass> handler = new HandlerCallingTarget<PrimitiveArrayClass>(target, new Object[] {s});
        PrimitiveArrayClass proxy = ProxyFactory.createProxy(PrimitiveArrayClass.class, handler);
     
        assertEquals(s, proxy.testShortArray(new short[] {6, 7}));
        assertEquals("testShortArray", handler.m.getName());
    }
}
