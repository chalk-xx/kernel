package org.sakaiproject.nakamura.docproxy.url;

import org.apache.commons.httpclient.HttpMethodBase;

/** Allows any HTTP method for HtttpClient */
public class HttpAnyMethod extends HttpMethodBase {
    private final String methodName;
    
    public HttpAnyMethod(String methodName, String uri) {
        super(uri);
        this.methodName = methodName;
    }

    @Override
    public String getName() {
        return methodName;
    }
}