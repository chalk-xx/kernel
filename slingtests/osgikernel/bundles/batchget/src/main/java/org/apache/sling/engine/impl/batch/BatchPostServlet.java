package org.apache.sling.engine.impl.batch;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.commons.json.io.JSONWriter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;

/**
 * Allows for bundled POST requests.
 * 
 * @scr.component immediate="true" label="BatchPostServlet"
 *                description="servlet to return multiple batch requests"
 * @scr.service interface="javax.servlet.Servlet"
 * @scr.property name="service.description"
 *               value="Bundles multiple post requests into a single response."
 * @scr.property name="service.vendor" value="The Sakai Foundation"
 * @scr.property name="sling.servlet.paths" value="/system/batch/post"
 * @scr.property name="sling.servlet.methods" value="POST"
 */
public class BatchPostServlet extends SlingAllMethodsServlet {

  /**
   * 
   */
  private static final long serialVersionUID = -7984383562558102040L;

  @Override
  protected void doPost(SlingHttpServletRequest request,
      SlingHttpServletResponse response) throws ServletException, IOException {

    RequestParameter p = request.getRequestParameter("p");
    if (p != null) {
      //InputStream in = p.getInputStream();
      //String json = IOUtils.readFully(in, "UTF-8");
      String json = p.getString();
      List<PostOperation> operations = new ArrayList<PostOperation>();
      try {
        JSONArray arr = new JSONArray(json);
        for (int i = 0; i < arr.length(); i++) {
          JSONObject obj = arr.getJSONObject(i);
          PostOperation operation = new PostOperation(obj);
          operations.add(operation);
        }
      } catch (JSONException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }

      try {
        JSONWriter write = new JSONWriter(response.getWriter());
        write.array();

        for (PostOperation operation : operations) {
          doRequest(request, response, operation, write);
        }
        write.endArray();
      } catch (JSONException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }

    }

  }

  /**
   * Performs a POST request with the data contained in operation.
   * Writes the result of the request back to the jsonwriter.
   * @param request
   * @param response
   * @param operation
   * @param write
   * @throws ServletException
   * @throws IOException
   * @throws JSONException
   */
  private void doRequest(SlingHttpServletRequest request,
      SlingHttpServletResponse response, PostOperation operation,
      JSONWriter write) throws ServletException, IOException, JSONException {
    // 

    RequestWrapper requestWrapper = new RequestWrapper(request);
    requestWrapper.setPostOperation(operation);
    ResponseWrapper responseWrapper = new ResponseWrapper(response);

    request.getRequestDispatcher(operation.getUrl()).forward(requestWrapper,
        responseWrapper);

    write.object();

    write.key("url");
    write.value(operation.getUrl());
    write.key("body");
    write.value(responseWrapper.getDataAsString());
    write.key("status");
    write.value(responseWrapper.getStatus());

    write.endObject();

  }
}
