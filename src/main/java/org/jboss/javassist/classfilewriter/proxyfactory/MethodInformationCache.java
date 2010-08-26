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

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.WeakHashMap;

/**
 * 
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class MethodInformationCache {
    private static final String[] NO_EXCEPTIONS = new String[0];
    
    private static final Map<Class<?>, List<WeakMethodInformation>> CACHE = Collections.synchronizedMap(new WeakHashMap<Class<?>, List<WeakMethodInformation>>());
    
    static List<MethodInformation> getSortedProxyableMethods(Class<?> clazz) {
        
        List<WeakMethodInformation> methods = CACHE.get(clazz); 
        if (methods == null) {
	        SortedSet<WeakMethodInformation> methodSet = new TreeSet<WeakMethodInformation>(new Comparator<WeakMethodInformation>() {
				@Override
	            public int compare(WeakMethodInformation m1, WeakMethodInformation m2) {
					int nameCompare = m1.getName().compareTo(m2.getName());
					if (nameCompare != 0)
						return nameCompare;
		            return m1.getParams().compareTo(m2.getParams());
	            }
			});
	        getProxyableMethods(methodSet, clazz);
	        
	        methods = Collections.unmodifiableList(new ArrayList<WeakMethodInformation>(methodSet)); 
	        CACHE.put(clazz, new ArrayList<WeakMethodInformation>(methods));
        }        
        
        List<MethodInformation> result = new ArrayList<MethodInformation>(methods.size());
        for (WeakMethodInformation m : methods)
        	result.add(new MethodInformation(m));
        return result;
    }

    private static void getProxyableMethods(Set<WeakMethodInformation> methodSet, Class<?> clazz) {
        if (clazz == Object.class)
            return;
        Method[] methods = SecurityActions.getDeclaredMethods(clazz);
        for (Method m : methods) {
            int modifiers = m.getModifiers();
            if (Modifier.isFinal(modifiers))
                continue;
            if (Modifier.isPrivate(modifiers))
                continue;
            if (Modifier.isStatic(modifiers))
                continue;
            if (Modifier.isVolatile(modifiers)) // Bridge method
                continue;
                
            WeakMethodInformation info = new WeakMethodInformation(m);
            if (methodSet.contains(info))
                continue;
            methodSet.add(info);
        }
        getProxyableMethods(methodSet, clazz.getSuperclass());
    }
    
    
    /**
     * MethodInformation implementation for caching with a weak reference to the Method object it represents 
     * to avoid classloader leaks.
     */
    private static class WeakMethodInformation{
        private final MethodPersistentReference method;
        private final String returnType;
        private final String params;
        private final int hashCode;
        
        private WeakMethodInformation(Method method) {
            this.method = new MethodPersistentReference(method);
            
            StringBuilder sb = new StringBuilder();
            getDescriptor(sb, method.getReturnType());
            returnType = sb.toString();
            
            sb = new StringBuilder();
            for (Class<?> param : method.getParameterTypes()) {
                getDescriptor(sb, param);
            }
            params = sb.toString();
            
            int hash = 17;
            hash = 31 * hash + getName().hashCode();
            hash = 31 * hash + params.hashCode();
            hashCode = hash;
        }
        
        private void getDescriptor(StringBuilder sb, Class<?> clazz) {
            if (clazz.isArray()) {
                sb.append('[');
                getDescriptor(sb, clazz.getComponentType());
                return;
            }
            if (clazz.isPrimitive()) 
                sb.append(BytecodePrimitive.valueOf(clazz.getName()).getArrayComponentName());
            else {
                sb.append('L');
                sb.append(clazz.getName().replace('.', '/'));
                sb.append(';');
            }
        }

        String getParams() {
        	return params;
        }
        
        Method getMethod() {
            return method.get();
        }
        
        String getName() {
            return method.getName();
        }

        String getFullSignature() {
        	StringBuilder sb = new StringBuilder();
            getFullSignature(sb);   
            return sb.toString();
        }
        
        private void getFullSignature(StringBuilder sb) {
        	sb.append("(");
        	sb.append(params);
        	sb.append(")");
        	sb.append(returnType);
        }
        
        String getNameAndFullSignature() {
        	StringBuilder sb = new StringBuilder(getName());
        	getFullSignature(sb);
        	return sb.toString();
        }
        
        @Override
        public boolean equals(Object obj) {
            if (obj.getClass() != this.getClass())
                return false;
            WeakMethodInformation other = (WeakMethodInformation)obj;
            if (!other.getName().equals(getName()))
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
    
    /**
     * The MethodInformation passed to the client.
     * It has a hard reference to the Method to avoid reloading it
     * in case GC happens during its use.
     * 
     * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
     * @version $Revision: 1.1 $
     */
    static class MethodInformation{
    	private final WeakMethodInformation delegate;
    	
    	private volatile Method method;
    	
    	private volatile String[] exceptions;
    	
        public MethodInformation(WeakMethodInformation delegate) {
	        this.delegate = delegate;
        }

		public String[] getExceptions() {
            Class<?>[] exceptionTypes = getMethod().getExceptionTypes();
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

        Method getMethod() {
        	if (method == null)
        		method = delegate.getMethod();
            return method;
        }
        
        String getName() {
        	return delegate.getName();
        }
        
        int getModifiers() {
            return getMethod().getModifiers();
        }

        String getFullSignature() {
        	return delegate.getFullSignature();
        }
        
        String getNameAndFullSignature() {
        	return delegate.getNameAndFullSignature();
        }
    }

    /**
     * Avoid keeping a hard reference to the method in CACHE since that
     * causes classloader leaks
     */
    private static class MethodPersistentReference {
    	private final WeakReference<Class<?>> classReference;
    	private final WeakReference<Class<?>>[] arguments;
    	private final String name;
    	
      	private volatile WeakReference<Method> referencedMethod;

       	public MethodPersistentReference(Method m) {
       		classReference = new WeakReference<Class<?>>(m.getDeclaringClass());
       		arguments = new WeakReference[m.getParameterTypes().length];
       		name = m.getName();
       		referencedMethod = new WeakReference<Method>(m);
    	}
       	
       	public String getName() {
       		return name;
       	}
    	
    	public Method get() {
    		Method m = referencedMethod.get();
    		if (m == null)
    		{
    			Class<?> declClass = getDeclaringClass();
    			Class<?>[] args = getArguments();
    			synchronized (this) {
    				m = referencedMethod.get();
	                if (m != null)
	                	return m;
	                try {
	                    m = SecurityActions.getDeclaredMethod(declClass, name, args);
                    } catch (Exception e) {
	                    throw new RuntimeException(e);
                    }
	                referencedMethod = new WeakReference<Method>(m);
    			}
    		}
    		
    		return m;
    	}
    	
    	private Class<?> getDeclaringClass(){
    		return getClassFromWeakReference(classReference);
    	}
    	
    	private Class<?>[] getArguments(){
    		Class<?>[] args = new Class[arguments.length];
    		for (int i = 0 ; i < args.length ; i++) {
    			args[i] = arguments[i].get();
    		}
    		return args;
    	}
    	
    	private Class<?> getClassFromWeakReference(WeakReference<Class<?>> ref){
    		Class<?> clazz = ref.get(); 
    		if (clazz == null)
    			throw new RuntimeException("Class was already unloaded for method '" + name + "'");
    		return clazz;
    	}
    }
}
