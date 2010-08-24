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
package org.jboss.javassist.classfilewriter.proxyfactory;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.ProtectionDomain;

/**
 * 
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
class SecurityActions {
    static ClassLoader getContextClassLoader() {
        if (System.getSecurityManager() == null)
            return Thread.currentThread().getContextClassLoader();
        else
            return AccessController.doPrivileged(GetContextClassLoaderAction.INSTANCE);
    }

    static class GetContextClassLoaderAction implements PrivilegedAction<ClassLoader> {
        final static GetContextClassLoaderAction INSTANCE = new GetContextClassLoaderAction();

        public ClassLoader run() {
            return Thread.currentThread().getContextClassLoader();
        }

    }

    static ClassLoader getClassLoader(final Class<?> clazz) {
        if (System.getSecurityManager() == null)
            return clazz.getClassLoader();
        else {
            return AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
                @Override
                public ClassLoader run() {
                    return clazz.getClassLoader();
                }
            });
        }
    }

    static ClassLoader getSystemClassLoader() {
        if (System.getSecurityManager() == null)
            return ClassLoader.getSystemClassLoader();
        else {
            return AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
                @Override
                public ClassLoader run() {
                    return ClassLoader.getSystemClassLoader();
                }
            });
        }
    }
    
    static ProtectionDomain getProtectionDomain(final Class<?> clazz) {
        if (System.getSecurityManager() == null)
            return clazz.getProtectionDomain();
        else {
            return AccessController.doPrivileged(new PrivilegedAction<ProtectionDomain>() {
                @Override
                public ProtectionDomain run() {
                    return clazz.getProtectionDomain();
                }
            });
        }
    }

    static Constructor<?>[] getDeclaredConstructors(final Class<?> clazz) {
        if (System.getSecurityManager() == null)
            return clazz.getDeclaredConstructors();
        else {
            return AccessController.doPrivileged(new PrivilegedAction<Constructor<?>[]>() {
                public Constructor<?>[] run() {
                    return clazz.getDeclaredConstructors();
                }
            });
        }
    }

    static Method[] getDeclaredMethods(final Class<?> clazz) {
        if (System.getSecurityManager() == null)
            return clazz.getDeclaredMethods();
        else {
            return AccessController.doPrivileged(new PrivilegedAction<Method[]>() {
                public Method[] run() {
                    return clazz.getDeclaredMethods();
                }
            });
        }
    }

    static void setAccessible(final AccessibleObject member) {
        if (System.getSecurityManager() == null)
            member.setAccessible(true);
        else
            AccessController.doPrivileged(new PrivilegedAction<Object>() {
                public Object run() {
                    member.setAccessible(true);
                    return null;
                }
            });
    }

    static Object invoke(final Method method, final Object target, final Object... args) throws IllegalAccessException,
            InvocationTargetException {
        if (System.getSecurityManager() == null)
            return method.invoke(target, args);

        try {
            return AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
                public Object run() throws Exception {
                    return method.invoke(target, args);
                }
            });
        } catch (PrivilegedActionException e) {
            if (e.getCause() instanceof RuntimeException)
                throw (RuntimeException) e.getCause();
            if (e.getCause() instanceof IllegalAccessException)
                throw (IllegalAccessException) e.getCause();
            if (e.getCause() instanceof InvocationTargetException)
                throw (InvocationTargetException) e.getCause();

            throw new RuntimeException(e.getCause());
        }
    }

    static Field getDeclaredField(final Class<?> clazz, final String name) throws NoSuchFieldException {
        if (System.getSecurityManager() == null)
            return clazz.getDeclaredField(name);
        else {
            try {
                return AccessController.doPrivileged(new PrivilegedExceptionAction<Field>() {
                    public Field run() throws Exception {
                        return clazz.getDeclaredField(name);
                    }
                });
            } catch (PrivilegedActionException e) {
                Throwable cause = e.getCause();
                if (cause instanceof NoSuchFieldException)
                    throw (NoSuchFieldException) cause;
                if (cause instanceof RuntimeException)
                    throw (RuntimeException) cause;
                throw new RuntimeException(cause);
            }
        }
    }

    static Method getDeclaredMethod(final Class<?> clazz, final String name, final Class<?>... parameters)
            throws NoSuchMethodException {
        if (System.getSecurityManager() == null)
            return clazz.getDeclaredMethod(name, parameters);
        else {
            try {
                return AccessController.doPrivileged(new PrivilegedExceptionAction<Method>() {
                    public Method run() throws Exception {
                        return clazz.getDeclaredMethod(name, parameters);
                    }
                });
            } catch (PrivilegedActionException e) {
                Throwable cause = e.getCause();
                if (cause instanceof NoSuchFieldException)
                    throw (NoSuchMethodException) cause;
                if (cause instanceof RuntimeException)
                    throw (RuntimeException) cause;
                throw new RuntimeException(cause);
            }
        }
    }

    static Constructor<?> getDeclaredConstructor(final Class<?> clazz, final Class<?>... parameters)
            throws NoSuchMethodException {
        if (System.getSecurityManager() == null)
            return clazz.getDeclaredConstructor(parameters);
        else {
            try {
                return AccessController.doPrivileged(new PrivilegedExceptionAction<Constructor<?>>() {
                    public Constructor<?> run() throws Exception {
                        return clazz.getDeclaredConstructor(parameters);
                    }
                });
            } catch (PrivilegedActionException e) {
                Throwable cause = e.getCause();
                if (cause instanceof NoSuchFieldException)
                    throw (NoSuchMethodException) cause;
                if (cause instanceof RuntimeException)
                    throw (RuntimeException) cause;
                throw new RuntimeException(cause);
            }
        }
    }

    static Class<?> classForName(final String name) throws ClassNotFoundException {
        if (System.getSecurityManager() == null)
            return Class.forName(name);
        else {
            try {
                return AccessController.doPrivileged(new PrivilegedExceptionAction<Class<?>>() {
                    public Class<?> run() throws Exception {
                        return Class.forName(name);
                    }
                });
            } catch (PrivilegedActionException e) {
                if (e.getException() instanceof ClassNotFoundException)
                    throw (ClassNotFoundException) e.getException();
                throw new RuntimeException(e.getException());
            }
        }
    }

}