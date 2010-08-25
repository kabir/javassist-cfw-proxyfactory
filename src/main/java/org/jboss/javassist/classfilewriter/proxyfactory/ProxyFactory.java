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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public final class ProxyFactory<T> {

    private static final AtomicInteger COUNTER = new AtomicInteger();

    private static final String PROXY_HANDLER_FIELD_NAME = "_proxy$Handler";

    private static final String PROXY_HANDLER_FIELD_TYPE = ProxyHandler.class.getName().replace('.', '/');

    private static final String PROXY_HANDLER_SIGNATURE = "L" + PROXY_HANDLER_FIELD_TYPE + ";";

    private static final String SET_PROXY_HANDLER_SIGNATURE = "(L" + ProxyHandler.class.getName().replace('.', '/')
            + ";)V";

    private static final String[] INTERFACES = new String[] { ProxyHandlerSetter.class.getName().replace('.', '/') };

    private final Class<T> clazz;
    private final ProxyHandler<T> handler;
    private final Set<MethodInformation> methodSet;
    private final ClassFileWriterContext<T> context;

    private ProxyFactory(Class<T> clazz, ProxyHandler<T> handler, Set<MethodInformation> methodSet) {
        this.clazz = clazz;
        this.handler = handler;
        this.methodSet = methodSet;

        // TODO might need an interface on the proxy to set the handler?
        context = new ClassFileWriterContext<T>(clazz.getName() + "$$Proxy$$" + COUNTER.incrementAndGet(), clazz
                .getName(), INTERFACES);
    }

    private Class<T> createProxy() {
        createProxyHandlerFieldAndSetter();

        for (MethodInformation m : methodSet)
            createProxyMethod(m);
        return generateClass();
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

    private void createProxyMethod(MethodInformation methodInformation) {
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

        
        if (!handler.finalCallInHandler(method)) {
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
    
    private Class<T> generateClass() {
        debug(context.getBytes());
        ClassLoader cl = SecurityActions.getClassLoader(clazz);
        if (cl == null)
            cl = SecurityActions.getSystemClassLoader();

        try {
            return context.toClass(cl, clazz.getProtectionDomain());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T createProxy(Class<T> clazz, ProxyHandler<T> handler) {

        // TODO cache classes

        checkClassModifiers(clazz);
        checkDefaultConstructor(clazz);
        Set<MethodInformation> methodSet = MethodInformation.getProxyableMethods(clazz);
        ProxyFactory<T> factory = new ProxyFactory<T>(clazz, handler, methodSet);
        Class<T> proxyClass = factory.createProxy();

        try {
            T proxy = proxyClass.newInstance();
            ((ProxyHandlerSetter) proxy).setProxyHandler(handler);
            handler.setMethods(methodSet);
            return proxy;
        } catch (Exception e) {
            throw new RuntimeException(e);
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

    private void debug(byte[] classbytes) {
        BufferedOutputStream out = null;
        try {
            out = new BufferedOutputStream(new FileOutputStream(new File("Debug.class")));
            out.write(classbytes);
        } catch (Exception e) {
            // AutoGenerated
            throw new RuntimeException(e);
        } finally {
            try {
                out.close();
            } catch (IOException ignore) {
            }
        }

    }
    
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
