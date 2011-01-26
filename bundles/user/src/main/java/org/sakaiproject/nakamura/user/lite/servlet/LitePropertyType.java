package org.sakaiproject.nakamura.user.lite.servlet;

public class LitePropertyType {

  public enum Type {
    STRING(), LONG(), DOUBLE(), DATE(), BOOLEAN(), UNDEFINED()
  }

  public static final String NAME_STRING = "String";
  public static final String NAME_LONG = "Long";
  public static final String NAME_DOUBLE = "Double";
  public static final String NAME_DATE = "Date";
  public static final String NAME_BOOLEAN = "Boolean";
  public static final String NAME_UNDEFINED = "undefined";

  public static Type create(String name) {
    if (name.equals(NAME_STRING)) {
      return Type.STRING;
    } else if (name.equals(NAME_BOOLEAN)) {
      return Type.BOOLEAN;
    } else if (name.equals(NAME_LONG)) {
      return Type.LONG;
    } else if (name.equals(NAME_DOUBLE)) {
      return Type.DOUBLE;
    } else if (name.equals(NAME_DATE)) {
      return Type.DATE;
    } else {
      return Type.UNDEFINED;
    }
  }
}
