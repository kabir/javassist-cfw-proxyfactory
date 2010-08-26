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

import java.lang.reflect.InvocationTargetException;
import java.security.ProtectionDomain;

import javassist.Modifier;
import javassist.bytecode.Bytecode;
import javassist.bytecode.ClassFile;
import javassist.bytecode.ClassFileWriter;
import javassist.bytecode.Descriptor;
import javassist.bytecode.MethodInfo;
import javassist.bytecode.Opcode;
import javassist.bytecode.ClassFileWriter.ConstPoolWriter;
import javassist.bytecode.ClassFileWriter.FieldWriter;
import javassist.bytecode.ClassFileWriter.MethodWriter;

/**
 * Wrapper around the {@link ClassFileWriter} with some utility methods
 * 
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
class ClassFileWriterContext<T> {
    private static final java.lang.reflect.Method defineClass1, defineClass2;

    static {
        try {
            Class<?> cl = Class.forName("java.lang.ClassLoader");
            defineClass1 = SecurityActions.getDeclaredMethod(cl, "defineClass", new Class[] { String.class,
                    byte[].class, int.class, int.class });

            defineClass2 = SecurityActions.getDeclaredMethod(cl, "defineClass", new Class[] { String.class,
                    byte[].class, int.class, int.class, ProtectionDomain.class });
        } catch (Exception e) {
            throw new RuntimeException("cannot initialize");
        }

        SecurityActions.setAccessible(defineClass1);
        SecurityActions.setAccessible(defineClass2);
    }

    /** The class of the interface we are implementing */
    // final Class<T> type;

    /** The name of the class we are creating */
    final String name;

    /** The underlying class file writer */
    final ClassFileWriter fileWriter;

    /** The underlying class pool writer */
    final ConstPoolWriter poolWriter;

    /** This class's name index in the const pool */
    final int thisClass;

    /** This class's superclass name index in the const pool */
    final int superClass;

    /** The interfaces */
    final int[] interfaces;

    /** The method writer for the methods */
    final MethodWriter mw;

    /** The created bytes */
    byte[] bytes;

    int stackDepth;

    int maxStackDepth;

    ClassFileWriterContext(String name, String superClassName, /* Class<T> type, */String[] interfaceNames) {
        this.name = ClassFileWriterContext.jvmClassName(name);
        superClassName = ClassFileWriterContext.jvmClassName(superClassName);
        for (int i = 0; i < interfaceNames.length; i++)
            interfaceNames[i] = ClassFileWriterContext.jvmClassName(interfaceNames[i]);

        fileWriter = new ClassFileWriter(ClassFile.JAVA_5, 0);
        poolWriter = fileWriter.getConstPool();
        thisClass = poolWriter.addClassInfo(this.name);
        superClass = poolWriter.addClassInfo(superClassName);
        interfaces = poolWriter.addClassInfo(interfaceNames);

        // Add default constructor
        mw = fileWriter.getMethodWriter();
        mw.begin(Modifier.PUBLIC, MethodInfo.nameInit, "()V", null, null);
        mw.add(Opcode.ALOAD_0);
        mw.add(Opcode.INVOKESPECIAL);
        int signature = poolWriter.addNameAndTypeInfo(MethodInfo.nameInit, "()V");
        mw.add16(poolWriter.addMethodrefInfo(superClass, signature));
        mw.add(Opcode.RETURN);
        mw.codeEnd(1, 1);
        mw.end(null, null);
    }

    // String getSimpleType()
    // {
    // return type.getSimpleName();
    // }

    String getName() {
        return name;
    }

    void createField(int accessFlags, String name, String descriptor) {
        FieldWriter fw = fileWriter.getFieldWriter();
        fw.add(accessFlags, name, descriptor, null);
    }

    void beginMethod(int accessFlags, String name, String descriptor, String[] exceptions) {
        mw.begin(Modifier.PUBLIC, name, descriptor, exceptions, null);
    }

    void endMethod(int maxLocals) {
        mw.codeEnd(maxStackDepth, maxLocals);
        mw.end(null, null);
    }

    void addInvokeStatic(String targetClass, String methodName, String descriptor) {
        mw.addInvoke(Opcode.INVOKESTATIC, targetClass, methodName, descriptor);

        // Stolen from Bytecode.addInvokestatic()
        growStack(Descriptor.dataSize(descriptor));
    }

    void addInvokeVirtual(String targetClass, String methodName, String descriptor) {
        mw.addInvoke(Opcode.INVOKEVIRTUAL, targetClass, methodName, descriptor);

        // Stolen from Bytecode.addInvokevirtual()
        growStack(Descriptor.dataSize(descriptor) - 1);
    }

    void addInvokeInterface(String targetClass, String methodName, String descriptor, int count) {
        mw.addInvoke(Opcode.INVOKEINTERFACE, targetClass, methodName, descriptor);
        mw.add(count);
        mw.add(0);

        // Stolen from Bytecode.addInvokeinterface()
        growStack(Descriptor.dataSize(descriptor) - 1);
    }

    void addInvokeSpecial(String targetClass, String methodName, String descriptor) {
        mw.addInvoke(Opcode.INVOKESPECIAL, targetClass, methodName, descriptor);

        // Stolen from Bytecode.addInvokespecial()
        growStack(Descriptor.dataSize(descriptor) - 1);
    }

    void addGetField(String className, String fieldName, String type) {
        mw.add(Opcode.GETFIELD);
        addFieldRefInfo(className, fieldName, type);

        // Stolen from Bytecode.addGetfield()
        growStack(Descriptor.dataSize(type) - 1);
    }

    void addGetStatic(String className, String fieldName, String type) {
        mw.add(Opcode.GETSTATIC);
        addFieldRefInfo(className, fieldName, type);

        // Stolen from Bytecode.addGetstatic()
        growStack(Descriptor.dataSize(type));
    }

    void addPutField(String className, String fieldName, String type) {
        mw.add(Opcode.PUTFIELD);
        addFieldRefInfo(className, fieldName, type);

        // Stolen from Bytecode.addPutfield()
        growStack(1 - Descriptor.dataSize(type));
    }

    void addPutStatic(String className, String fieldName, String type) {
        mw.add(Opcode.PUTSTATIC);
        addFieldRefInfo(className, fieldName, type);

        // Stolen from Bytecode.addPutStatic()
        growStack(-Descriptor.dataSize(type));
    }

    void addAReturn() {
        mw.add(Opcode.ARETURN);

        // From Opcode.STACK_GROW[]
        growStack(-1);
    }
    
    void addDReturn() {
        mw.add(Opcode.DRETURN);

        // From Opcode.STACK_GROW[]
        growStack(-2);
    }
    
    void addFReturn() {
        mw.add(Opcode.FRETURN);

        // From Opcode.STACK_GROW[]
        growStack(-1);
    }
    
    void addLReturn() {
        mw.add(Opcode.LRETURN);

        // From Opcode.STACK_GROW[]
        growStack(-2);
    }
    
    void addIReturn() {
        mw.add(Opcode.IRETURN);

        // From Opcode.STACK_GROW[]
        growStack(-1);
    }
    
    void addReturn() {
        mw.add(Opcode.RETURN);
    }

    void addAConstNull() {
        mw.add(Opcode.ACONST_NULL);

        // From Opcode.STACK_GROW[]
        growStack(1);
    }

    void addAALoad() {
        mw.add(Opcode.AALOAD);

        // From Opcode.STACK_GROW[]
        growStack(-1);
    }

    void addAAStore() {
        mw.add(Opcode.AASTORE);

        // From Opcode.STACK_GROW[]
        growStack(-3);
    }

    void addLdc(int stringIndex) {
        if (stringIndex > 0xFF) {
            mw.add(Opcode.LDC_W);
            addIndex(stringIndex);
        }
        else {
            mw.add(Opcode.LDC);
            mw.add(stringIndex);
        }
    }


    /**
     * Adds the right bytecode to call ALOAD depending on the number of the
     * parameter
     * 
     * @param i
     *            the number of the parameter
     * @see Bytecode#addAload(int)
     */
    void addAload(int i) {
        if (i < 4)
            mw.add(Opcode.ALOAD_0 + i);
        else if (i < 0x100) {
            mw.add(Opcode.ALOAD); // aload
            mw.add(i);
        } else {
            mw.add(Opcode.WIDE);
            mw.add(Opcode.ALOAD);
            addIndex(i);
        }
        // From Opcode.STACK_GROW[]
        growStack(1);
    }

    /**
     * Adds the right bytecode to call ASTORE depending on the number of the
     * parameter
     * 
     * @param i
     *            the number of the parameter
     * @see Bytecode#addAstore(int)
     */
    void addAstore(int i) {
        if (i < 4)
            mw.add(Opcode.ASTORE_0 + i);
        else if (i < 0x100) {
            mw.add(Opcode.ASTORE); // aload
            mw.add(i);
        } else {
            mw.add(Opcode.WIDE);
            mw.add(Opcode.ASTORE);
            addIndex(i);
        }
        // From Opcode.STACK_GROW[]
        growStack(-1);
    }
    
    /**
     * Adds the right bytecode to call ILOAD depending on the number of the
     * parameter
     * 
     * @param i
     *            the number of the parameter
     * @see Bytecode#addIload(int)
     */
    void addIload(int i) {
        if (i < 4) {
            mw.add(Opcode.ILOAD_0 + i);
        } else if (i < 0x100) {
            mw.add(Opcode.ILOAD);
            mw.add(i);
        }
        else {
            mw.add(Opcode.WIDE);
            mw.add(Opcode.ILOAD);
            addIndex(i);
        }
        
        // From Opcode.STACK_GROW[]
        growStack(1);
    }
    
    /**
     * Adds the right bytecode to call DLOAD depending on the number of the
     * parameter
     * 
     * @param i
     *            the number of the parameter
     * @see Bytecode#addDload(int)
     */
    void addDload(int i) {
        if (i < 4) {
            mw.add(Opcode.DLOAD_0 + i);
        } else if (i < 0x100) {
            mw.add(Opcode.DLOAD);
            mw.add(i);
        }
        else {
            mw.add(Opcode.WIDE);
            mw.add(Opcode.DLOAD);
            addIndex(i);
        }
        // From Opcode.STACK_GROW[]
        growStack(2);
    }
    
    /**
     * Adds the right bytecode to call FLOAD depending on the number of the
     * parameter
     * 
     * @param i
     *            the number of the parameter
     * @see Bytecode#addFload(int)
     */
    void addFload(int i) {
        if (i < 4) {
            mw.add(Opcode.FLOAD_0 + i);
        } else if (i < 0x100) {
            mw.add(Opcode.FLOAD);
            mw.add(i);
        }
        else {
            mw.add(Opcode.WIDE);
            mw.add(Opcode.FLOAD);
            addIndex(i);
        }
        // From Opcode.STACK_GROW[]
        growStack(1);
    }
    
    /**
     * Adds the right bytecode to call LLOAD depending on the number of the
     * parameter
     * 
     * @param i
     *            the number of the parameter
     * @see Bytecode#addLload(int)
     */
    void addLload(int i) {
        if (i < 4) {
            mw.add(Opcode.LLOAD_0 + i);
        } else if (i < 0x100) {
            mw.add(Opcode.LLOAD);
            mw.add(i);
        }
        else {
            mw.add(Opcode.WIDE);
            mw.add(Opcode.LLOAD);
            addIndex(i);
        }
        // From Opcode.STACK_GROW[]
        growStack(2);
    }
    
    /**
     * Adds the right bytecode to load a constant depending on the size of the
     * constant
     * 
     * @param i
     *            the number
     * @see Bytecode#addIconst(int);
     */
    void addIconst(int i) {
        if (i < 6 && -2 < i)
            mw.add(Opcode.ICONST_0 + i); // iconst_<i> -1..5
        else if (i <= 127 && -128 <= i) {
            mw.add(Opcode.BIPUSH); // bipush
            mw.add(i);
        } else if (i <= 32767 && -32768 <= i) {
            mw.add(Opcode.SIPUSH); // sipush
            mw.add(i >> 8);
            mw.add(i);
        } else {
            int ref = poolWriter.addIntegerInfo(i);

            if (i > 0xFF) {
                mw.add(Opcode.LDC_W);
                mw.add(i >> 8);
                mw.add(i);
            } else {
                mw.add(Opcode.LDC);
                mw.add(ref);
            }
        }
        // From Opcode.STACK_GROW[]
        growStack(1);
    }

    void addNew(String className) {
        mw.add(Opcode.NEW);
        addIndex(addClassInfo(className));

        // From Opcode.STACK_GROW[]
        growStack(1);
    }
    
    void addAnewArray(String className, int size) {
        addIconst(size);
        mw.add(Opcode.ANEWARRAY);
        addIndex(addClassInfo(className));
    }

    void addDup() {
        mw.add(Opcode.DUP);

        // From Opcode.STACK_GROW[]
        growStack(1);
    }

    void addCheckcast(String clazz) {
        mw.add(Opcode.CHECKCAST);
        int i = poolWriter.addClassInfo(clazz);
        addIndex(i);

        // From Opcode.STACK_GROW[]
        // No change to stack
    }

    byte[] getBytes() {
        if (bytes == null)
            bytes = fileWriter.end(Modifier.PUBLIC, thisClass, superClass, interfaces, null);
        return bytes;
    }

    Class<? extends T> toClass(ClassLoader loader, ProtectionDomain domain) throws InvocationTargetException,
            IllegalAccessException {

        String name = this.name.replace('/', '.');
        byte[] bytes = getBytes();
        if (domain == null)
            return (Class<T>)SecurityActions.invoke(defineClass1, loader, name, bytes, Integer.valueOf(0), Integer.valueOf(bytes.length));
        else
            return (Class<T>) SecurityActions.invoke(defineClass2, loader, name, bytes, Integer.valueOf(0), Integer.valueOf(bytes.length), domain);
    }

    private void addIndex(int i) {
        mw.add(i >> 8);
        mw.add(i);
    }

    private void addFieldRefInfo(String className, String fieldName, String type) {
        addIndex(poolWriter.addFieldrefInfo(poolWriter.addClassInfo(className), poolWriter.addNameAndTypeInfo(
                fieldName, type)));
    }

    private int addClassInfo(String className) {
        return poolWriter.addClassInfo(className.replace('.', '/'));
    }
    
    int addStringInfo(String s) {
       return poolWriter.addStringInfo(s); 
    }
    
    private void growStack(int i) {
        stackDepth += i;
        if (stackDepth > maxStackDepth)
            maxStackDepth = stackDepth;
    }

    static String jvmClassName(Class<?> clazz) {
        return ClassFileWriterContext.jvmClassName(clazz.getName());
    }

    static String jvmClassName(String name) {
        return name.replace('.', '/');
    }
}