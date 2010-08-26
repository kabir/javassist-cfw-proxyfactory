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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

class Boxing {
	static final Map<String, Boxing> BOXERS;
	static {
		Map<String, Boxing> map = new HashMap<String, Boxing>();
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

	static void addEntry(Map<String, Boxing> map, BytecodePrimitive primitive) {
		String jvmClassName = ClassFileWriterContext.jvmClassName(primitive.getWrapperClassName());
		Boxing boxing = new Boxing(jvmClassName, primitive.getValueMethodName(), "()" + primitive.getArrayComponentName(), "("
		        + primitive.getArrayComponentName() + ")L" + jvmClassName + ";");
		map.put(primitive.getName(), boxing);
		map.put(primitive.getArrayComponentName(), boxing);
	}

	private final String className;
	private final String unboxMethodName;
	private final String unboxMethodDescriptor;
	private final String boxMethodDescriptor;

	static Boxing getUnboxer(Class<?> clazz) {
		return BOXERS.get(clazz.getName());
	}

	Boxing(String className, String methodName, String unboxMethodDescriptor, String boxMethodDescriptor) {
		this.className = className;
		this.unboxMethodName = methodName;
		this.unboxMethodDescriptor = unboxMethodDescriptor;
		this.boxMethodDescriptor = boxMethodDescriptor;
	}

	String getClassName() {
		return className;
	}

	String getUnboxMethodName() {
		return unboxMethodName;
	}

	String getUnboxMethodDescriptor() {
		return unboxMethodDescriptor;
	}

	String getBoxMethodName() {
		return "valueOf";
	}

	String getBoxMethodDescriptor() {
		return boxMethodDescriptor;
	}
}