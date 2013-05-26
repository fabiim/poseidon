package datastore.util;

import java.lang.reflect.Array;
import java.lang.reflect.Field;

public class ClassFieldPrinter {
	  public static String dump(Object o) {
	        StringBuffer buffer = new StringBuffer();
	        Class oClass = o.getClass();
	         if (oClass.isArray()) {
	             System.out.print("Array: ");
	            System.out.print("[");
	            for (int i = 0; i < Array.getLength(o); i++) {
	                Object value = Array.get(o, i);
	                if (value.getClass().isPrimitive() ||
	                        value.getClass() == java.lang.Long.class ||
	                        value.getClass() == java.lang.Integer.class ||
	                        value.getClass() == java.lang.Boolean.class ||
	                        value.getClass() == java.lang.String.class ||
	                        value.getClass() == java.lang.Double.class ||
	                        value.getClass() == java.lang.Short.class ||
	                        value.getClass() == java.lang.Byte.class
	                        ) {
	                    System.out.print(value);
	                    if(i != (Array.getLength(o)-1)) System.out.print(",");
	                } else {
	                    System.out.print(dump(value));
	                 }
	            }
	            System.out.print("]\n");
	        } else {
	             System.out.print("Class: " + oClass.getName());
	             System.out.print("{\n");
	            while (oClass != null) {
	                Field[] fields = oClass.getDeclaredFields();
	                for (int i = 0; i < fields.length; i++) {
	                    fields[i].setAccessible(true);
	                    System.out.print(fields[i].getName());
	                    System.out.print("=");
	                    try {
	                        Object value = fields[i].get(o);
	                        if (value != null) {
	                            if (value.getClass().isPrimitive() ||
	                                    value.getClass() == java.lang.Long.class ||
	                                    value.getClass() == java.lang.String.class ||
	                                    value.getClass() == java.lang.Integer.class ||
	                                    value.getClass() == java.lang.Boolean.class ||
	                                        value.getClass() == java.lang.Double.class ||
	                                    value.getClass() == java.lang.Short.class ||
	                                    value.getClass() == java.lang.Byte.class
	                                    ) {
	                                System.out.print(value);
	                            } else {
	                                System.out.print(dump(value));
	                            }
	                        }
	                    } catch (IllegalAccessException e) {
	                        System.out.print(e.getMessage());
	                    }
	                    System.out.print("\n");
	                }
	                oClass = oClass.getSuperclass();
	            }
	            System.out.print("}\n");
	        }
	        return buffer.toString();
	    }
	  
	  
}
