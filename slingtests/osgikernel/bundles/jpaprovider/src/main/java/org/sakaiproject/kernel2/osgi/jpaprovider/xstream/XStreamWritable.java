package org.sakaiproject.kernel2.osgi.jpaprovider.xstream;

import com.thoughtworks.xstream.XStream;

public abstract class XStreamWritable {

  protected String xsiLocation;
  protected String schemaLocation;
  protected String version;
  protected String namespace;

  public String getXsiLocation() {
    return xsiLocation;
  }

  public String getSchemaLocation() {
    return schemaLocation;
  }

  public String getVersion() {
    return version;
  }

  public String getNamespace() {
    return namespace;
  }

  protected static void setupNamespaceAliasing(XStream xstream)
  {
    xstream.aliasAttribute(XStreamWritable.class, "xsiLocation", "xmlns:xsi");
    xstream.aliasAttribute(XStreamWritable.class, "schemaLocation", "xsi:schemaLocation");
    xstream.aliasAttribute(XStreamWritable.class, "namespace", "xmlns");
    xstream.useAttributeFor(XStreamWritable.class, "version");
  }

}
