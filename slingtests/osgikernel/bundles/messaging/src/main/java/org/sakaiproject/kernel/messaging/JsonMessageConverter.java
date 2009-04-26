/*
 * Licensed to the Sakai Foundation (SF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.sakaiproject.kernel.messaging;

import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.commons.json.JSONTokener;
import org.sakaiproject.kernel.api.messaging.ConversionException;
import org.sakaiproject.kernel.api.messaging.Message;
import org.sakaiproject.kernel.api.messaging.MessageConverter;
import org.sakaiproject.kernel.api.messaging.MessagingService;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

/**
 * @scr.component
 */
public class JsonMessageConverter implements MessageConverter {

  /** @scr.reference */
  private MessagingService messagingService;

  /**
   * Default constructor.
   */
  public JsonMessageConverter() {
  }

  /**
   * Constructor for all required fields.
   *
   * @param messagingService
   */
  public JsonMessageConverter(MessagingService messagingService) {
    this.messagingService = messagingService;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.kernel.api.messaging.MessageConverter#toString(org.sakaiproject.kernel.api.messaging.Message)
   */
  public String toString(Message msg) throws ConversionException {
    try {
      // create the string writer and initial builder
      JSONObject base = new JSONObject();

      // accumulate headers from message
      for (Entry<String, String> header : msg.getHeaders().entrySet()) {
        base.accumulate(header.getKey(), header.getValue());
      }

      // add the body
      if (msg.isBodyText()) {
        base.put(Message.Field.BODY_TEXT.toString(), msg.getText());
      } else {
        base.put(Message.Field.BODY_URL.toString(), msg.getBody()
            .toExternalForm());
      }

      if (msg.getParts().size() > 0) {
        JSONArray parts = new JSONArray();
        // add attachments
        for (Message part : msg.getParts()) {
          parts.put(toString(part));
          // base.accumulate(PARTS, toString(part));
        }
        base.put(Message.Field.PARTS.toString(), parts);
      }

      return base.toString();
    } catch (JSONException e) {
      throw new ConversionException(e.getMessage(), e);
    }
  }

  public Message toMessage(String json) throws ConversionException {
    try {
      JSONTokener tokener = new JSONTokener(json);
      JSONObject jsonObj = new JSONObject(tokener);
      Message msg = toMessage(jsonObj);
      return msg;
    } catch (JSONException e) {
      throw new ConversionException(e.getMessage(), e);
    }
  }

  protected Message toMessage(JSONObject jsonObj) throws JSONException {
    Message msg = messagingService.createMessage();

    // add headers
    Iterator<String> keys = jsonObj.keys();
    while (keys.hasNext()) {
      String key = keys.next();
      Object value = jsonObj.get(key);
      if (Message.Field.BODY_TEXT.toString().equals(key)) {
        msg.setText((String) value);
      } else if (Message.Field.BODY_URL.toString().equals(key)) {
        try {
          msg.setBody(new URL((String) value));
        } catch (MalformedURLException e) {
          msg.setText("Unable to link to body.");
        }
      } else if (Message.Field.PARTS.toString().equals(key)) {
        JSONArray array = (JSONArray) value;
        for (int i = 0; i < array.length(); i++) {
          msg.addPart(toMessage(array.getJSONObject(i)));
        }
      } else {
        msg.setHeader(key, (String) value);
      }
    }
    return msg;
  }

  /**
   * Convert a message to JCR nodes. The conversion traverses all parts of all
   * messages and sets up a similar node structure.
   *
   * @param node
   * @param msg
   * @throws ConversionException
   */
  public void toNode(Node node, Message msg) throws ConversionException {
    if (node != null) {
      try {
        node.setProperty(Message.Field.BODY_TEXT.toString(), msg.getText());
        node.setProperty(Message.Field.BODY_URL.toString(), msg.getBody().toExternalForm());
        for (Entry<String, String> header : msg.getHeaders().entrySet()) {
          node.setProperty(header.getKey(), header.getValue());
        }

        List<Message> parts = msg.getParts();
        for (int i = 0; i < parts.size(); i++) {
          Node next = node.addNode("parts[" + i + "]");
          toNode(next, parts.get(i));
        }
      } catch (RepositoryException e) {
        throw new ConversionException(e.getMessage(), e);
      }
    }
  }
}
