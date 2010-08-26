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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.javassist.classfilewriter.proxyfactory.MethodInformationCache.MethodInformation;

/**
 * Provide a subclass of this to handle proxy calls.
 * 
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public abstract class ProxyHandler<T> {

    private final T instance;
    
    private volatile Map<String, Method> methods;
    
    /**
     * Constructor
     * 
     * @param instance the instance we want to proxy
     * @throws IllegalArgumentException if <code>instance</code> is null
     */
    protected ProxyHandler(T instance) {
        if (instance == null)
            throw new IllegalArgumentException("Null instance");
        this.instance = instance;
    }
    
    void setMethods(List<MethodInformation> methods) {
        Map<String, Method> map = new HashMap<String, Method>(methods.size());
        for (MethodInformation mi : methods)
            map.put(mi.getNameAndFullSignature(), mi.getMethod());
        this.methods = map;
    }
    
    /**
     * Gets a method from its name and signature
     * 
     * @param nameAndFullDescription the name and signature of the method called
     * @return the method
     * @throws IllegalArgumentException if the method was not found
     */
    protected final Method getMethod(String nameAndFullDescription) {
        Method method = methods.get(nameAndFullDescription);
        if (method == null)
            throw new IllegalArgumentException("No method in " + instance.getClass().getName() + " called " + nameAndFullDescription);
        return method;
    }
    
    /**
     * Get the instance handled
     * 
     * @return the instance
     */
    protected final T getInstance() {
        return instance;
    }
    
    /**
     * Override to indicate that the method is handled by this handler, so that invokeMethod gets called.
     * Default is that this handler will handle the method. This will only get called when generating the 
     * proxy, i.e. changing it at runtime has no effect.
     * 
     * @param m the method we are checking
     * @return true if we are handling this method
     */
    public boolean isHandled(Method m) {
        return true;
    }
    
    /**
     * Override to indicate whether the target method should be called by this handler.
     * Default is that this handler will not handle the method, instead it is called by
     * the proxy wrapper.  This will only get called when generating the 
     * proxy, i.e. changing it at runtime has no effect.
     * 
     * @param m the method we are checking
     * @return true if we will call the target method
     */
    protected boolean finalCallInHandler(Method m) {
        return false;
    }
    
    
    public final Object invokeMethod(String nameAndFullDescription, Object[] args) {
        Method m = getMethod(nameAndFullDescription);
        return invokeMethod(instance, m, args);
    }
    
    /**
     * Override to handle the method calls
     * 
     * @param nameAndFullDescription the name and signature of the method called
     * @return the value of calling the method
     */
    protected abstract Object invokeMethod(T instance, Method m, Object[] args);
    
}
