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
import static junit.framework.Assert.assertNotSame;
import static junit.framework.Assert.assertSame;
import static junit.framework.Assert.assertTrue;

import java.lang.reflect.Method;

import org.jboss.javassist.classfilewriter.proxyfactory.ProxyFactory;
import org.jboss.javassist.classfilewriter.proxyfactory.ProxyHandler;
import org.jboss.javassist.classfilewriter.proxyfactory.support.SomeClass;
import org.junit.Test;

/**
 * 
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class ProxyAndFilterTestCase {
	
	@Test
	public void testDefaultCachedProxy() throws Exception {
		SomeClass instanceA = new SomeClass();
		StandardSomeClassHandlerNotCallingTarget handler = new StandardSomeClassHandlerNotCallingTarget(instanceA);
		SomeClass proxy1 = ProxyFactory.createProxy(SomeClass.class, handler);
		
		SomeClass proxy2 = ProxyFactory.createProxy(SomeClass.class, handler);
		assertSame(proxy1.getClass(), proxy2.getClass());

		SomeClass instanceB = new SomeClass();
		StandardSomeClassHandlerNotCallingTarget handler2 = new StandardSomeClassHandlerNotCallingTarget(instanceB);
		SomeClass proxy3 = ProxyFactory.createProxy(SomeClass.class, handler2);
		assertSame(proxy2.getClass(), proxy3.getClass());
	}
	
	@Test
	public void testNoCallFilter() throws Exception {
		SomeClass instanceA = new SomeClass();
		SomeClass instanceB = new SomeClass();
		NoopSomeClassHandler handler1 = new NoopSomeClassHandler(instanceA);
		NoopSomeClassHandler handler2 = new NoopSomeClassHandler(instanceB);
		SomeClass proxy1 = ProxyFactory.createProxy(SomeClass.class, handler1);
		SomeClass proxy2 = ProxyFactory.createProxy(SomeClass.class, handler2);
		assertSame(proxy1.getClass(), proxy2.getClass());
		
		handler1.invoked = false;
		handler2.invoked = false;
		assertEquals(1, proxy1.method(1));
		assertEquals("A", proxy1.method("A"));
		assertEquals(2, proxy1.method(2));
		assertEquals("B", proxy1.method("B"));
		assertFalse(handler1.invoked);
		assertFalse(handler2.invoked);
	}
	
	@Test
	public void testIntMethodFilter() throws Exception {
		SomeClass instanceA = new SomeClass();
		SomeClass instanceB = new SomeClass();
		SomeClassIntMethodHandlerNotCallingTarget handler1A = new SomeClassIntMethodHandlerNotCallingTarget(instanceA);
		SomeClassIntMethodHandlerNotCallingTarget handler1B = new SomeClassIntMethodHandlerNotCallingTarget(instanceB);
		SomeClassIntMethodHandlerCallingTarget handler2 = new SomeClassIntMethodHandlerCallingTarget(instanceB);
		SomeClass proxy1A = ProxyFactory.createProxy(SomeClass.class, handler1A);
		SomeClass proxy1B = ProxyFactory.createProxy(SomeClass.class, handler1B);
		SomeClass proxy2 = ProxyFactory.createProxy(SomeClass.class, handler2);
		
		assertSame(proxy1A.getClass(), proxy1B.getClass());
		assertNotSame(proxy1A.getClass(), proxy2.getClass());
		
		assertEquals(1, proxy1A.method(1));
		assertTrue(handler1A.invoked);
		assertFalse(handler1B.invoked);
		assertFalse(handler2.invoked);
		handler1A.invoked = false;
		
		assertEquals(2, proxy1B.method(2));
		assertFalse(handler1A.invoked);
		assertTrue(handler1B.invoked);
		assertFalse(handler2.invoked);
		handler1B.invoked = false;

		assertEquals(999, proxy2.method(3));
		assertFalse(handler1A.invoked);
		assertFalse(handler1B.invoked);
		assertTrue(handler2.invoked);
		handler2.invoked = false;
		
		assertEquals("A", proxy1A.method("A"));
		assertEquals("B", proxy1B.method("B"));
		assertEquals("C", proxy2.method("C"));
		assertFalse(handler1A.invoked);
		assertFalse(handler1B.invoked);
		assertFalse(handler2.invoked);
	}

	@Test
	public void testStringMethodFilter() throws Exception {
		SomeClass instanceA = new SomeClass();
		SomeClass instanceB = new SomeClass();
		SomeClassStringMethodHandlerNotCallingTarget handler1A = new SomeClassStringMethodHandlerNotCallingTarget(instanceA);
		SomeClassStringMethodHandlerNotCallingTarget handler1B = new SomeClassStringMethodHandlerNotCallingTarget(instanceB);
		SomeClassStringMethodHandlerCallingTarget handler2 = new SomeClassStringMethodHandlerCallingTarget(instanceB);
		SomeClass proxy1A = ProxyFactory.createProxy(SomeClass.class, handler1A);
		SomeClass proxy1B = ProxyFactory.createProxy(SomeClass.class, handler1B);
		SomeClass proxy2 = ProxyFactory.createProxy(SomeClass.class, handler2);
		
		assertSame(proxy1A.getClass(), proxy1B.getClass());
		assertNotSame(proxy1A.getClass(), proxy2.getClass());
		
		assertEquals(1, proxy1A.method(1));
		assertEquals(2, proxy1B.method(2));
		assertEquals(3, proxy2.method(3));
		assertFalse(handler1A.invoked);
		assertFalse(handler1B.invoked);
		assertFalse(handler2.invoked);
		
		assertEquals("A", proxy1A.method("A"));
		assertTrue(handler1A.invoked);
		assertFalse(handler1B.invoked);
		assertFalse(handler2.invoked);
		handler1A.invoked = false;
		
		assertEquals("B", proxy1B.method("B"));
		assertFalse(handler1A.invoked);
		assertTrue(handler1B.invoked);
		assertFalse(handler2.invoked);
		handler1B.invoked = false;

		assertEquals("HANDLED", proxy2.method("C"));
		assertFalse(handler1A.invoked);
		assertFalse(handler1B.invoked);
		assertTrue(handler2.invoked);
		handler2.invoked = false;
	}
	
	@Test
	public void testHandleAllMethodsDifferentFinalCall() throws Exception{
		SomeClass instanceA = new SomeClass();
		SomeClass instanceB = new SomeClass();
		SomeClassHandleAllIntCallingTarget handler1A = new SomeClassHandleAllIntCallingTarget(instanceA);
		SomeClassHandleAllIntCallingTarget handler1B = new SomeClassHandleAllIntCallingTarget(instanceB);
		SomeClassHandleAllStringCallingTarget handler2 = new SomeClassHandleAllStringCallingTarget(instanceB);
		SomeClass proxy1A = ProxyFactory.createProxy(SomeClass.class, handler1A);
		SomeClass proxy1B = ProxyFactory.createProxy(SomeClass.class, handler1B);
		SomeClass proxy2 = ProxyFactory.createProxy(SomeClass.class, handler2);
		
		assertSame(proxy1A.getClass(), proxy1B.getClass());
		assertNotSame(proxy1A.getClass(), proxy2.getClass());

		assertEquals(999, proxy1A.method(1));
		assertTrue(handler1A.invoked);
		assertFalse(handler1B.invoked);
		assertFalse(handler2.invoked);
		handler1A.invoked = false;
		
		assertEquals(999, proxy1B.method(1));
		assertFalse(handler1A.invoked);
		assertTrue(handler1B.invoked);
		assertFalse(handler2.invoked);
		handler1B.invoked = false;
		
		assertEquals(1, proxy2.method(1));
		assertFalse(handler1A.invoked);
		assertFalse(handler1B.invoked);
		assertTrue(handler2.invoked);
		handler2.invoked = false;
		
		assertEquals("A", proxy1A.method("A"));
		assertTrue(handler1A.invoked);
		assertFalse(handler1B.invoked);
		assertFalse(handler2.invoked);
		handler1A.invoked = false;
		
		assertEquals("B", proxy1B.method("B"));
		assertFalse(handler1A.invoked);
		assertTrue(handler1B.invoked);
		assertFalse(handler2.invoked);
		handler1B.invoked = false;
		
		assertEquals("HANDLED", proxy2.method("C"));
		assertFalse(handler1A.invoked);
		assertFalse(handler1B.invoked);
		assertTrue(handler2.invoked);
		handler2.invoked = false;
		
	}

	
	private static class StandardSomeClassHandlerNotCallingTarget extends ProxyHandler<SomeClass>{
		boolean invoked;
		protected StandardSomeClassHandlerNotCallingTarget(SomeClass instance) {
	        super(instance);
        }

		@Override
        protected Object invokeMethod(SomeClass instance, Method m, Object[] args) {
			invoked = true;
	        return null;
        }
	}
	
	private static class NoopSomeClassHandler extends ProxyHandler<SomeClass>{
		boolean invoked;
		protected NoopSomeClassHandler(SomeClass instance) {
	        super(instance);
        }

		@Override
        protected Object invokeMethod(SomeClass instance, Method m, Object[] args) {
			invoked = true;
	        return null;
        }

		@Override
        public boolean isHandled(Method m) {
	        return false;
        }
	}
	
	private static class SomeClassIntMethodHandlerNotCallingTarget extends ProxyHandler<SomeClass>{
		boolean invoked;
		protected SomeClassIntMethodHandlerNotCallingTarget(SomeClass instance) {
	        super(instance);
        }

		@Override
        protected Object invokeMethod(SomeClass instance, Method m, Object[] args) {
			invoked = true;
	        return null;
        }

		@Override
        public boolean isHandled(Method m) {
			if (!m.getName().equals("method"))
				throw new IllegalArgumentException("Unexpexted method " + m);
			if (m.getParameterTypes().length != 1)
				throw new IllegalArgumentException("Unexpexted method " + m);
			return m.getParameterTypes()[0] == Integer.TYPE;
        }
	}
	
	private static class SomeClassIntMethodHandlerCallingTarget extends ProxyHandler<SomeClass>{
		boolean invoked;
		protected SomeClassIntMethodHandlerCallingTarget(SomeClass instance) {
	        super(instance);
        }

		@Override
        protected Object invokeMethod(SomeClass instance, Method m, Object[] args) throws Throwable{
			invoked = true;
	        return m.invoke(instance, new Object[] {999});
        }

		@Override
        public boolean isHandled(Method m) {
			if (!m.getName().equals("method"))
				throw new IllegalArgumentException("Unexpexted method " + m);
			if (m.getParameterTypes().length != 1)
				throw new IllegalArgumentException("Unexpexted method " + m);
			return m.getParameterTypes()[0] == Integer.TYPE;
        }

		@Override
        protected boolean finalCallInHandler(Method m) {
			if (!m.getName().equals("method"))
				throw new IllegalArgumentException("Unexpexted method " + m);
			if (m.getParameterTypes().length != 1)
				throw new IllegalArgumentException("Unexpexted method " + m);
			return m.getParameterTypes()[0] == Integer.TYPE;
        }
	}

	private static class SomeClassStringMethodHandlerNotCallingTarget extends ProxyHandler<SomeClass>{
		boolean invoked;
		protected SomeClassStringMethodHandlerNotCallingTarget(SomeClass instance) {
	        super(instance);
        }

		@Override
        protected Object invokeMethod(SomeClass instance, Method m, Object[] args) {
			invoked = true;
	        return null;
        }

		@Override
        public boolean isHandled(Method m) {
			if (!m.getName().equals("method"))
				throw new IllegalArgumentException("Unexpexted method " + m);
			if (m.getParameterTypes().length != 1)
				throw new IllegalArgumentException("Unexpexted method " + m);
			return m.getParameterTypes()[0] == String.class;
        }
	}
	
	private static class SomeClassStringMethodHandlerCallingTarget extends ProxyHandler<SomeClass>{
		boolean invoked;
		protected SomeClassStringMethodHandlerCallingTarget(SomeClass instance) {
	        super(instance);
        }

		@Override
        protected Object invokeMethod(SomeClass instance, Method m, Object[] args) throws Throwable {
			invoked = true;
			return m.invoke(instance, new Object[] {"HANDLED"});
        }

		@Override
        public boolean isHandled(Method m) {
			if (!m.getName().equals("method"))
				throw new IllegalArgumentException("Unexpexted method " + m);
			if (m.getParameterTypes().length != 1)
				throw new IllegalArgumentException("Unexpexted method " + m);
			return m.getParameterTypes()[0] == String.class;
        }

		@Override
        protected boolean finalCallInHandler(Method m) {
			if (!m.getName().equals("method"))
				throw new IllegalArgumentException("Unexpexted method " + m);
			if (m.getParameterTypes().length != 1)
				throw new IllegalArgumentException("Unexpexted method " + m);
			return m.getParameterTypes()[0] == String.class;
        }
	}

	private static abstract class SomeClassHandleAllDifferentFinalCall extends ProxyHandler<SomeClass> {
		boolean invoked;
		
		protected SomeClassHandleAllDifferentFinalCall(SomeClass instance) {
	        super(instance);
        }

		@Override
        protected Object invokeMethod(SomeClass instance, Method m, Object[] args) throws Throwable {
			invoked = true;
			if (finalCallInHandler(m))
				return m.invoke(instance, getOverriddenArg());
	        return null;
        }
		
		abstract Object getOverriddenArg();
		
	}
	
	private static class SomeClassHandleAllStringCallingTarget extends SomeClassHandleAllDifferentFinalCall {

		protected SomeClassHandleAllStringCallingTarget(SomeClass instance) {
	        super(instance);
        }

		@Override
        protected boolean finalCallInHandler(Method m) {
			if (!m.getName().equals("method"))
				throw new IllegalArgumentException("Unexpexted method " + m);
			if (m.getParameterTypes().length != 1)
				throw new IllegalArgumentException("Unexpexted method " + m);
			return m.getParameterTypes()[0] == String.class;
        }

		@Override
        Object getOverriddenArg() {
	        return "HANDLED";
        }
	}
	
	private static class SomeClassHandleAllIntCallingTarget extends SomeClassHandleAllDifferentFinalCall {

		protected SomeClassHandleAllIntCallingTarget(SomeClass instance) {
	        super(instance);
        }

		@Override
        protected boolean finalCallInHandler(Method m) {
			if (!m.getName().equals("method"))
				throw new IllegalArgumentException("Unexpexted method " + m);
			if (m.getParameterTypes().length != 1)
				throw new IllegalArgumentException("Unexpexted method " + m);
			return m.getParameterTypes()[0] == Integer.TYPE;
        }

		@Override
        Object getOverriddenArg() {
	        return 999;
        }
	}
	
}
