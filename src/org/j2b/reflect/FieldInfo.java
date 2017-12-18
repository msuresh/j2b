package org.j2b.reflect;

import java.lang.reflect.Field;

public class FieldInfo implements Comparable<FieldInfo> {

	private boolean isBoolean;

	private Field field;

	private String fieldName;

	public FieldInfo(Field pfield, int parentLevel) {
		field = pfield;
		isBoolean = pfield.getType() == Boolean.TYPE;
		String fn = pfield.getName();
		if (parentLevel > 0) {
			fn = fn + '$' + parentLevel;
		}
		fieldName = fn;
	}

	@Override
	public int compareTo(FieldInfo o) {
		int result = 0;
		if (isBoolean) {
			result--;
		}
		if (o.isBoolean) {
			result++;
		}
		if (result == 0) {
		    result = fieldName.compareTo(o.fieldName);
		}
		return result;
	}

	public boolean isBoolean() {
		return isBoolean;
	}

	public String getFieldName() {
		return fieldName;
	}

	public Field getField() {
		return field;
	}
}
