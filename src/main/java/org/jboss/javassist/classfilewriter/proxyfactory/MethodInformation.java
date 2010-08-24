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

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class to get hold of the information from a method in jvm format
 * 
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
final class MethodInformation {
    private static final String[] NO_EXCEPTIONS = new String[0];
    
    static final Map<Class<?>, Character> PRIMITIVE_DESCRIPTORS;
    static {
        Map<Class<?>, Character> map = new HashMap<Class<?>, Character>();
        map.put(Boolean.TYPE, 'Z');
        map.put(Byte.TYPE, 'B');
        map.put(Character.TYPE, 'C');
        map.put(Double.TYPE, 'D');
        map.put(Float.TYPE, 'F');
        map.put(Integer.TYPE, 'I');
        map.put(Long.TYPE, 'J');
        map.put(Short.TYPE, 'S');
        map.put(Void.TYPE, 'V');
        
        PRIMITIVE_DESCRIPTORS = Collections.unmodifiableMap(map);
    }

    private final Method method;
    private final int modifiers;
    private final String name;
    private final String returnType;
    private final String params;
    private volatile String fullSignature;
    private volatile String nameAndFillSignature;
    private volatile String[] exceptions;
    private final int hashCode;
    
    
    MethodInformation(Method method) {
        this.method = method;
        modifiers = method.getModifiers();
        name = method.getName();
        
        StringBuilder sb = new StringBuilder();
        getDescriptor(sb, method.getReturnType());
        returnType = sb.toString();
        
        sb = new StringBuilder();
        for (Class<?> param : method.getParameterTypes()) {
            getDescriptor(sb, param);
        }
        params = sb.toString();
        
        int hash = 17;
        hash = 31 * hash + name.hashCode();
        hash = 31 * hash + name.hashCode();
        hashCode = hash;
    }
    
    private void getDescriptor(StringBuilder sb, Class<?> clazz) {
        if (clazz.isArray()) {
            sb.append('[');
            getDescriptor(sb, clazz.getComponentType());
            return;
        }
        if (clazz.isPrimitive()) 
            sb.append(PRIMITIVE_DESCRIPTORS.get(clazz));
        else {
            sb.append('L');
            sb.append(clazz.getName().replace('.', '/'));
            sb.append(';');
        }
    }

    Method getMethod() {
        return method;
    }
    
    String getParams() {
        return params;
    }

    int getModifiers() {
        return modifiers;
    }

    String getName() {
        return name;
    }

    String getReturnType() {
        return returnType;
    }
    
    String getFullSignature() {
        if (fullSignature == null) {
            String sig = "(" + params + ")" + returnType;
            fullSignature = sig;
        }
            
        return fullSignature;
    }
    
    String getNameAndFullSignature() {
        if (nameAndFillSignature == null) {
            String sig = name + getFullSignature();
            nameAndFillSignature = sig;
        }
        return nameAndFillSignature;
    }
    
    String[] getExceptions() {
        Class<?>[] exceptionTypes = method.getExceptionTypes();
        if (exceptionTypes.length == 0)
            return NO_EXCEPTIONS;
        if (exceptions == null) {
            String[] exceptions = new String[exceptionTypes.length];
            for (int i = 0 ; i < exceptionTypes.length ; i++) {
                exceptions[i] = exceptionTypes[i].getName().replace('.', '/');
            }
            this.exceptions = exceptions;
        }
        return exceptions;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj.getClass() != this.getClass())
            return false;
        MethodInformation other = (MethodInformation)obj;
        if (!other.getName().equals(name))
            return false;
        if (!other.getParams().equals(params))
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        return hashCode; 
    }
}