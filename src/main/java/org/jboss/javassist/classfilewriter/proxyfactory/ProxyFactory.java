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
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import org.jboss.javassist.classfilewriter.proxyfactory.MethodInformationCache.MethodInformation;

/**
 * Factory to create proxies for a class. The proxies are currently
 * "dumb", i.e. they just override the selected methods with no 
 * annotations or Signature attributes.
 * 
 * 
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public final class ProxyFactory<T> {

    private static final String PROXY_HANDLER_FIELD_NAME = "_proxy$Handler";

    private static final String PROXY_HANDLER_FIELD_TYPE = ProxyHandler.class.getName().replace('.', '/');

    private static final String PROXY_HANDLER_SIGNATURE = "L" + PROXY_HANDLER_FIELD_TYPE + ";";

    private static final String SET_PROXY_HANDLER_SIGNATURE = "(L" + ProxyHandler.class.getName().replace('.', '/') + ";)V";

    private static final String[] INTERFACES = new String[] { ProxyHandlerSetter.class.getName().replace('.', '/') };
    
    private static final Map<Class<?>, Map<String, WeakReference<Class<?>>>> CACHE = new WeakHashMap<Class<?>, Map<String, WeakReference<Class<?>>>>();

    private final Class<T> clazz;
    private final ProxyHandler<T> handler;
    private final MethodInformation[] methods;
    private final ClassFileWriterContext<T> context;
    private final byte[] handledFilter;
    private final byte[] finalCallInHandlerFilter;
    

    private ProxyFactory(String proxyName, Class<T> clazz, ProxyHandler<T> handler, MethodInformation[] methods, byte[] handledFilter, byte[] finalCallInHandlerFilter) {
        this.clazz = clazz;
        this.handler = handler;
        this.methods = methods;
        this.handledFilter = handledFilter;
        this.finalCallInHandlerFilter = finalCallInHandlerFilter;

        // TODO might need an interface on the proxy to set the handler?
        context = new ClassFileWriterContext<T>(proxyName, clazz
                .getName(), INTERFACES);
    }

    /**
     * Create a proxy instance.
     * 
     * @param clazz the class we want to proxy
     * @param handler a proxy handler for the instance we want to proxy
     * @return the proxy
     * @throws IllegalArgumentException if the class is not proxyable
     * @throws RuntimeException if there was an error
     */
    public static <T> T createProxy(Class<T> clazz, ProxyHandler<T> handler) {
        if (clazz == null)
            throw new IllegalArgumentException("Null clazz");
        if (handler == null)
            throw new IllegalArgumentException("Null handler");

        checkClassModifiers(clazz);
        checkDefaultConstructor(clazz);
        MethodInformation[] methods = MethodInformationCache.getSortedProxyableMethods(clazz);
        byte[] handledFilter = filterHandledMethods(methods, handler);
        byte[] finalCallInHandlerFilter = filterFinalCallInHandlerMethods(methods, handler);
        String proxyName = getProxyClassName(clazz, handledFilter, finalCallInHandlerFilter);
        
        Class<? extends T> proxyClass = checkCache(clazz, proxyName);
        if (proxyClass == null) {
        	ProxyFactory<T> factory = new ProxyFactory<T>(proxyName, clazz, handler, methods, handledFilter, finalCallInHandlerFilter);
        	factory.createProxy();
        	proxyClass = defineClassAndPutInCache(factory, proxyName);
        }

        return instantiateProxy(proxyClass, methods, handler);
    }
    
    private static <T> Class<? extends T> checkCache(Class<T> clazz, String proxyName){
    	synchronized (CACHE) {
        	Map<String, WeakReference<Class<?>>> map = CACHE.get(clazz);
        	if (map == null)
        		return null;
        	WeakReference<Class<?>> proxyClassRef = map.get(proxyName);
        	if (proxyClassRef == null)
        		return null;
        	
        	Class<?> proxyClass = proxyClassRef.get();
//        	This can't happen, can it?!?
//        	if (proxyClass == null) {
//        		try {
//	                proxyClass = getClassLoader(clazz).loadClass(proxyName);
//                } catch (ClassNotFoundException e) {
//	                throw new RuntimeException("Could not load previously created proxy class " + proxyName, e);
//                }
//        	}
        	return proxyClass.asSubclass(clazz);
        }
    }
    
    private static <T> Class<? extends T> defineClassAndPutInCache(ProxyFactory<T> factory, String proxyName){
    	synchronized (CACHE) {
    		
    		Class<? extends T>  proxyClass = checkCache(factory.clazz, proxyName);
    		if (proxyClass != null)
    			return proxyClass;

            ClassLoader cl = getClassLoader(factory.clazz);
            if (cl == null)
                cl = SecurityActions.getSystemClassLoader();
            try {
                proxyClass = factory.context.toClass(cl, factory.clazz.getProtectionDomain());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            
        	Map<String, WeakReference<Class<?>>> map = CACHE.get(factory.clazz);
        	if (map == null) {
        		map = new HashMap<String, WeakReference<Class<?>>>();
        		CACHE.put(factory.clazz, map);
        	}
        	
        	map.put(proxyName, new WeakReference<Class<?>>(proxyClass));
        	return proxyClass.asSubclass(factory.clazz);
        }
    }
    
    private static ClassLoader getClassLoader(Class<?> clazz) {
        ClassLoader cl = SecurityActions.getClassLoader(clazz);
        if (cl == null)
            cl = SecurityActions.getSystemClassLoader();
        return cl;
    }
    
    private static <T> T instantiateProxy(Class<? extends T> proxyClass, MethodInformation[] methods, ProxyHandler<T> handler) {
        try {
            T proxy = proxyClass.newInstance();
            ((ProxyHandlerSetter) proxy).setProxyHandler(handler);
            handler.setMethods(methods);
            return proxy;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void createProxy() {
        createProxyHandlerFieldAndSetter();

        for (int i = 0 ; i < methods.length ; i++)
            createProxyMethod(i, methods[i]);
    }

    private void createProxyHandlerFieldAndSetter() {
        context.createField(Modifier.PRIVATE | Modifier.VOLATILE, PROXY_HANDLER_FIELD_NAME, PROXY_HANDLER_SIGNATURE);
        context.beginMethod(Modifier.PUBLIC, "setProxyHandler", SET_PROXY_HANDLER_SIGNATURE, null);
        context.addAload(0);
        context.addAload(1);
        context.addPutField(context.getName(), PROXY_HANDLER_FIELD_NAME, PROXY_HANDLER_SIGNATURE);
        context.addReturn();
        context.endMethod(2);
    }

    private void createProxyMethod(int methodIndex, MethodInformation methodInformation) {
    	if (handledFilter[methodIndex] == 0)
    		return;
    	
        final Method method = methodInformation.getMethod();
        context.beginMethod(methodInformation.getModifiers(), methodInformation.getName(), methodInformation.getFullSignature(), methodInformation.getExceptions());

        //Call the ProxyHandler.invokeMethod() with the parameters in an array
        Class<?>[] params = method.getParameterTypes();
        context.addAnewArray("java/lang/Object", params.length);
        int paramIndex = 0;
        for (int i = 0 ; i < params.length ; i++) {
            paramIndex++;
            context.addDup();
            context.addIconst(i);
            loadParameter(params[i], paramIndex);
            paramIndex = offsetParam(params[i], paramIndex);
            boxValue(params[i]);
            context.addAAStore();
        }
        final int argsArrayIndex = paramIndex + 1;
        context.addAstore(argsArrayIndex);
        context.addAload(0);
        context.addGetField(context.getName(), PROXY_HANDLER_FIELD_NAME, PROXY_HANDLER_SIGNATURE);
        context.addIconst(methodIndex);
        context.addAload(argsArrayIndex);
        context.addInvokeVirtual(PROXY_HANDLER_FIELD_TYPE, "invokeMethod", "(I[Ljava/lang/Object;)Ljava/lang/Object;");

        
        if (finalCallInHandlerFilter[methodIndex] == 0) {
            //Call the super implementation of the method
            context.addAload(0);
            paramIndex = 0;
            for (int i = 0 ; i < params.length ; i++) {
                paramIndex++;
                loadParameter(params[i], paramIndex);
                paramIndex = offsetParam(params[i], paramIndex);
            }
            context.addInvokeSpecial(ClassFileWriterContext.jvmClassName(clazz), methodInformation.getName(), methodInformation.getFullSignature());
        } else {
            //Unbox the return value from the handler if needed
            castAndUnboxValue(method.getReturnType());
        }
        
        addReturn(method);
        //Add an extra local variable each for 'this' and for the Object[] passed to PH.invokeMethod()
        context.endMethod(2 + paramIndex);
    }

    private void boxValue(Class<?> type) {
        Boxing boxing = Boxing.getUnboxer(type);
        if (boxing == null)
            return;
        context.addInvokeStatic(boxing.getClassName(), boxing.getBoxMethodName(), boxing.getBoxMethodDescriptor());
    }
    
    private void castAndUnboxValue(Class<?> type) {
        if (type.equals(Object.class) || type.equals(Void.TYPE))
            return;

         context.addCheckcast(ClassFileWriterContext.jvmClassName(getBoxedType(type)));
         Boxing unboxer = Boxing.getUnboxer(type);
         if (unboxer != null)
         {
            context.addInvokeVirtual(unboxer.getClassName(), unboxer.getUnboxMethodName(), unboxer.getUnboxMethodDescriptor());
         }
    }
    

    /**
     * Get the boxed type
     * 
     * @param type the type to box
     * @return the boxed type name
     */
    String getBoxedType(Class<?> type)
    {
       Boxing boxing = Boxing.getUnboxer(type);
       if (boxing != null)
       {
          return boxing.getClassName();
       }
       return type.getName();
    }

    
    private void addReturn(Method method) {
        Class<?> rtn = method.getReturnType();
        if (rtn.isPrimitive()) {
            if (rtn == Void.TYPE) {
                context.addReturn();
            } else if (rtn == Double.TYPE) {
                context.addDReturn();
            } else if (rtn == Float.TYPE) {
                context.addFReturn();
            } else if (rtn == Long.TYPE) {
                context.addLReturn();
            } else if (rtn == Boolean.TYPE || rtn == Byte.TYPE || rtn == Character.TYPE || rtn == Integer.TYPE || rtn == Short.TYPE) {
                context.addIReturn();
            } else {
                //Shouldn't happen
                throw new IllegalArgumentException("Unknown primitive " + rtn);
            }
        }
        else {
            context.addAReturn();
        }
        
    }
    
    /**
     * Long and double take an extra parameter slot
     */
    private int offsetParam(Class<?> type, int index) {
        if (type == Double.TYPE || type == Long.TYPE)
            index++;
        return index;
    }
    
    private void loadParameter(Class<?> type, int index) {
        //xLOAD uses 1 based indexing
        if (!type.isPrimitive()) {
            context.addAload(index);
        }else {
            if (type == Double.TYPE) {
                context.addDload(index);
            } else if (type == Float.TYPE) {
                context.addFload(index);
            } else if (type == Long.TYPE) {
                context.addLload(index);
            } else if (type == Boolean.TYPE || type == Byte.TYPE || type == Character.TYPE || type == Integer.TYPE || type == Short.TYPE) {
                context.addIload(index);
            } else {
                //Shouldn't happen
                throw new IllegalArgumentException("Unknown primitive " + type);
            }
        }
    }
    
    private static void checkClassModifiers(Class<?> clazz) {
        int modifier = clazz.getModifiers();
        if (Modifier.isPrivate(modifier))
            throw new IllegalArgumentException("Cannot proxy private class " + clazz.getName());
        if (Modifier.isFinal(modifier))
            throw new IllegalArgumentException("Cannot proxy final class " + clazz.getName());
        if (clazz.getDeclaringClass() != null && !Modifier.isStatic(modifier))
            throw new IllegalArgumentException("Cannot proxy non-static inner class " + clazz.getName());
    }

    private static void checkDefaultConstructor(Class<?> clazz) {
        try {
            Constructor<?> ctor = SecurityActions.getDeclaredConstructor(clazz);
            if (Modifier.isPrivate(ctor.getModifiers()))
                throw new IllegalArgumentException("Default constructor is not public " + clazz.getName());
        } catch (NoSuchMethodException e) {
            // AutoGenerated
            throw new IllegalArgumentException("No default constuctor " + clazz.getName());
        }
    }

    private static String getProxyClassName(Class<?> clazz, byte[] methodFilter, byte[] finalCallInWrapperFilter) {
    	StringBuilder sb = new StringBuilder(clazz.getName());
    	sb.append("$$");
    	for (byte b : methodFilter)
    		sb.append(b);
    	sb.append("$");
    	for (byte b : finalCallInWrapperFilter)
    		sb.append(b);
    	return sb.toString();
    }
    
    private static byte[] filterHandledMethods(MethodInformation[] methods, ProxyHandler<?> handler) {
    	byte[] handledMethods = new byte[methods.length];
    	for (int i = 0 ; i < handledMethods.length ; i++) {
    		handledMethods[i] = handler.isHandled(methods[i].getMethod()) ? (byte)1 : (byte)0;
    	}
    	return handledMethods;
    }
    
    private static byte[] filterFinalCallInHandlerMethods(MethodInformation[] methods, ProxyHandler<?> handler) {
    	byte[] handledMethods = new byte[methods.length];
    	for (int i = 0 ; i < handledMethods.length ; i++) {
    		handledMethods[i] = handler.finalCallInHandler(methods[i].getMethod())  ? (byte)1 : (byte)0;
    	}
    	return handledMethods;
    }
}
