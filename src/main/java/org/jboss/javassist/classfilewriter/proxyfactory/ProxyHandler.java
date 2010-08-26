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

import org.jboss.javassist.classfilewriter.proxyfactory.MethodInformationCache.MethodInformation;

/**
 * Provide a subclass of this to handle proxy calls.
 * 
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public abstract class ProxyHandler<T> {

    private final T instance;
    
    /** The sorted array of methods */
    private volatile Method[] methods;
    
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
    
    void setMethods(MethodInformation[] methods) {
    	this.methods = new Method[methods.length];
    	for (int i = 0 ; i < methods.length ; i++)
    		this.methods[i] = methods[i].getMethod();
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
    
    
    public final Object invokeMethod(int index, Object[] args) {
        Method method = methods[index];
        if (method == null)
            throw new IllegalArgumentException("No method in " + instance.getClass().getName() + " with index " + index);

        return invokeMethod(instance, method, args);
    }
    
    /**
     * Override to handle the method calls
     * 
     * @param instance the instance we are invoking on
     * @param m the method being called
     * @param args the arguments of the call
     * @return the value of calling the method
     */
    protected abstract Object invokeMethod(T instance, Method m, Object[] args);
    
}
