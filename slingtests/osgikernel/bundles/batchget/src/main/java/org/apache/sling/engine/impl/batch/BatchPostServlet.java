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
import javax.servlet.http.HttpServletResponse;

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
        response.sendError(HttpServletResponse.SC_BAD_REQUEST,
            "Failed to parse the JSON parameter");
      }

      try {
        JSONWriter write = new JSONWriter(response.getWriter());
        write.array();

        for (PostOperation operation : operations) {
          doRequest(request, response, operation, write);
        }
        write.endArray();
      } catch (JSONException e) {
        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
            "Failed to write JSON response");
      }

    }

  }

  /**
   * Performs a POST request with the data contained in operation. Writes the result of
   * the request back to the jsonwriter.
   * 
   * @param request
   * @param response
   * @param operation
   * @param write
   * @throws JSONException
   */
  private void doRequest(SlingHttpServletRequest request,
      SlingHttpServletResponse response, PostOperation operation,
      JSONWriter write) throws JSONException {
    // 

    RequestWrapper requestWrapper = new RequestWrapper(request);
    requestWrapper.setPostOperation(operation);
    ResponseWrapper responseWrapper = new ResponseWrapper(response);

    try {
      request.getRequestDispatcher(operation.getUrl()).forward(requestWrapper,
          responseWrapper);

      writeResponse(write, operation.getUrl(), responseWrapper
          .getDataAsString(), responseWrapper.getStatus());
    } catch (ServletException e) {
      writeResponse(write, operation.getUrl(), "", 500);
    } catch (IOException e) {
      writeResponse(write, operation.getUrl(), "", 500);
    }

  }

  /**
   * Returns a JSON object with 3 keys: url, body, status
   * 
   * @param write
   * @param url
   * @param body
   * @param status
   * @throws JSONException
   */
  private void writeResponse(JSONWriter write, String url, String body,
      int status) throws JSONException {
    write.object();
    write.key("url");
    write.value(url);
    write.key("body");
    write.value(body);
    write.key("status");
    write.value(status);
    write.endObject();
  }
}
