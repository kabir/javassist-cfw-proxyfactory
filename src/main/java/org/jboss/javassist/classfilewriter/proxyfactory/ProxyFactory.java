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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

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
    private final List<MethodInformation> methods;
    private final ClassFileWriterContext<T> context;
    private final byte[] handledFilter;
    private final byte[] finalCallInHandlerFilter;
    

    private ProxyFactory(String proxyName, Class<T> clazz, ProxyHandler<T> handler, List<MethodInformation> methods, byte[] handledFilter, byte[] finalCallInHandlerFilter) {
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

        // TODO cache classes

        checkClassModifiers(clazz);
        checkDefaultConstructor(clazz);
        List<MethodInformation> methods = MethodInformation.getSortedProxyableMethods(clazz);
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
        	if (proxyClass == null) {
        		try {
	                proxyClass = getClassLoader(clazz).loadClass(proxyName);
                } catch (ClassNotFoundException e) {
	                throw new RuntimeException("Could not load previously created proxy class " + proxyName, e);
                }
        	}
        	return proxyClass.asSubclass(clazz);
        }
    }
    
    private static <T> Class<? extends T> defineClassAndPutInCache(ProxyFactory<T> factory, String proxyName){
    	synchronized (CACHE) {
    		
    		Class<? extends T>  proxyClass = checkCache(factory.clazz, proxyName);
    		if (proxyClass != null)
    			return proxyClass;

    		
            ClassLoader cl = SecurityActions.getClassLoader(factory.clazz);
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
    
    private static <T> T instantiateProxy(Class<? extends T> proxyClass, List<MethodInformation> methods, ProxyHandler<T> handler) {
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

        for (int i = 0 ; i < methods.size() ; i++)
            createProxyMethod(i, methods.get(i));
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

    private void createProxyMethod(int index, MethodInformation methodInformation) {
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
        context.addLdc(context.addStringInfo(methodInformation.getNameAndFullSignature()));
        context.addAload(argsArrayIndex);
        context.addInvokeVirtual(PROXY_HANDLER_FIELD_TYPE, "invokeMethod", "(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/Object;");

        
        if (finalCallInHandlerFilter[index] == 0) {
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
    
    private static byte[] filterHandledMethods(List<MethodInformation> methods, ProxyHandler<?> handler) {
    	byte[] handledMethods = new byte[methods.size()];
    	for (int i = 0 ; i < handledMethods.length ; i++) {
    		handledMethods[i] = handler.isHandled(methods.get(i).getMethod()) ? (byte)1 : (byte)0;
    	}
    	return handledMethods;
    }
    
    private static byte[] filterFinalCallInHandlerMethods(List<MethodInformation> methods, ProxyHandler<?> handler) {
    	byte[] handledMethods = new byte[methods.size()];
    	for (int i = 0 ; i < handledMethods.length ; i++) {
    		handledMethods[i] = handler.finalCallInHandler(methods.get(i).getMethod())  ? (byte)1 : (byte)0;
    	}
    	return handledMethods;
    }
    
//    private void debug(byte[] classbytes) {
//        BufferedOutputStream out = null;
//        try {
//            out = new BufferedOutputStream(new FileOutputStream(new File("Debug.class")));
//            out.write(classbytes);
//        } catch (Exception e) {
//            // AutoGenerated
//            throw new RuntimeException(e);
//        } finally {
//            try {
//                out.close();
//            } catch (IOException ignore) {
//            }
//        }
//
//    }
    
    private static class Boxing
    {
       static final Map<String , Boxing> BOXERS;
       static
       {
          Map<String , Boxing> map = new HashMap<String, Boxing>();
          addEntry(map, BytecodePrimitive.BOOLEAN);
          addEntry(map, BytecodePrimitive.BYTE);
          addEntry(map, BytecodePrimitive.CHAR);
          addEntry(map, BytecodePrimitive.DOUBLE);
          addEntry(map, BytecodePrimitive.FLOAT);
          addEntry(map, BytecodePrimitive.INT);
          addEntry(map, BytecodePrimitive.LONG);
          addEntry(map, BytecodePrimitive.SHORT);
          BOXERS = Collections.unmodifiableMap(map);
       }
       
       static void addEntry(Map<String, Boxing> map, BytecodePrimitive primitive)
       {
          String jvmClassName = ClassFileWriterContext.jvmClassName(primitive.getWrapperClassName()); 
          Boxing boxing = new Boxing(
                jvmClassName, 
                primitive.getValueMethodName(), 
                "()" + primitive.getArrayComponentName(),
                "(" + primitive.getArrayComponentName() + ")L" + jvmClassName + ";");
          map.put(primitive.getName(), boxing);
          map.put(primitive.getArrayComponentName(), boxing);
       }
       
       private String className;
       private String unboxMethodName;
       private String unboxMethodDescriptor;
       private String boxMethodDescriptor;
       
       static Boxing getUnboxer(Class<?> clazz)
       {
          return BOXERS.get(clazz.getName());
       }
       
       Boxing(String className, String methodName, String unboxMethodDescriptor, String boxMethodDescriptor)
       {
          this.className = className;
          this.unboxMethodName = methodName;
          this.unboxMethodDescriptor = unboxMethodDescriptor;
          this.boxMethodDescriptor = boxMethodDescriptor;
       }

       String getClassName()
       {
          return className;
       }
       
       String getUnboxMethodName()
       {
          return unboxMethodName;
       }
    
       String getUnboxMethodDescriptor()
       {
          return unboxMethodDescriptor;
       }
       
       String getBoxMethodName()
       {
          return "valueOf";
       }
       
       String getBoxMethodDescriptor()
       {
          return boxMethodDescriptor;
       }
    }
    
    static class BytecodePrimitive
    {
       static final BytecodePrimitive BOOLEAN = new BytecodePrimitive("boolean", "Z", Boolean.class, Boolean.TYPE, "booleanValue");

       static final BytecodePrimitive CHAR = new BytecodePrimitive("char", "C", Character.class, Character.TYPE, "charValue");

       static final BytecodePrimitive BYTE = new BytecodePrimitive("byte", "B", Byte.class, Byte.TYPE, "byteValue");

       static final BytecodePrimitive SHORT = new BytecodePrimitive("short", "S", Short.class, Short.TYPE, "shortValue");

       static final BytecodePrimitive INT = new BytecodePrimitive("int", "I", Integer.class, Integer.TYPE, "intValue");

       static final BytecodePrimitive LONG = new BytecodePrimitive("long", "J", Long.class, Long.TYPE, "longValue");

       static final BytecodePrimitive FLOAT = new BytecodePrimitive("float", "F", Float.class, Float.TYPE, "floatValue");

       static final BytecodePrimitive DOUBLE = new BytecodePrimitive("double", "D", Double.class, Double.TYPE, "doubleValue");

       static final BytecodePrimitive VOID = new BytecodePrimitive("void", "V", Void.class, Void.TYPE, null);

       static final Map<String, BytecodePrimitive> primitives;
       static
       {
          Map<String, BytecodePrimitive> map = new HashMap<String, BytecodePrimitive>();
          map.put(BOOLEAN.getName(), BOOLEAN);
          map.put(BOOLEAN.getArrayComponentName(), BOOLEAN);
          map.put(CHAR.getName(), CHAR);
          map.put(CHAR.getArrayComponentName(), CHAR);
          map.put(BYTE.getName(), BYTE);
          map.put(BYTE.getArrayComponentName(), BYTE);
          map.put(SHORT.getName(), SHORT);
          map.put(SHORT.getArrayComponentName(), SHORT);
          map.put(INT.getName(), INT);
          map.put(INT.getArrayComponentName(), INT);
          map.put(LONG.getName(), LONG);
          map.put(LONG.getArrayComponentName(), LONG);
          map.put(FLOAT.getName(), FLOAT);
          map.put(FLOAT.getArrayComponentName(), FLOAT);
          map.put(DOUBLE.getName(), DOUBLE);
          map.put(DOUBLE.getArrayComponentName(), DOUBLE);
          map.put(VOID.getName(), VOID);
          map.put(VOID.getArrayComponentName(), VOID);
          primitives = Collections.unmodifiableMap(map);
       }
       
       final String name;
       
       final Class<?> wrapperClass;
       
       final Class<?> primitiveClass;
       
       final String arrayComponentName;
       
       final String wrapperClassName;
       
       final String valueMethodName;

       private BytecodePrimitive(String name, String arrayComponentName, Class<?> wrapperClass, Class<?> primitiveClass, String valueMethodName)
       {
          this.name = name;
          this.arrayComponentName = arrayComponentName;
          this.wrapperClass = wrapperClass;
          this.primitiveClass = primitiveClass;
          this.wrapperClassName = wrapperClass.getName();
          this.valueMethodName = valueMethodName;
       }

       static BytecodePrimitive valueOf(String name)
       {
          return primitives.get(name);
       }
       
       String getName()
       {
          return name;
       }

       String getArrayComponentName()
       {
          return arrayComponentName;
       }

       String getWrapperClassName()
       {
          return wrapperClassName;
       }

       String getValueMethodName()
       {
          return valueMethodName;
       }
       
       Class<?> getWrapperClass()
       {
          return wrapperClass;
       }
       
       Class<?> getPrimitiveClass()
       {
          return primitiveClass;
       }
    }
}
