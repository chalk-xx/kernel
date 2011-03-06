package org.sakaiproject.nakamura.messagebucket;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;

import org.apache.sling.commons.json.JSONException;
import org.sakaiproject.nakamura.api.messagebucket.MessageBucket;
import org.sakaiproject.nakamura.api.messagebucket.MessageBucketException;
import org.sakaiproject.nakamura.api.messagebucket.Waiter;
import org.sakaiproject.nakamura.util.ExtendedJSONWriter;

import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class MessageBucketImpl implements MessageBucket {

  private Map<Waiter, Waiter> waiters = Maps.newConcurrentHashMap();
  private Map<String, HttpServletRequest> bound = Maps.newConcurrentHashMap();
  private boolean ready = false;
  private Map<String, Object> messagePayload;

  public boolean isReady() {
    return ready;
  }

  public void addWaiter(Waiter waiter) {
    waiters.put(waiter, waiter);
  }

  public void removeWaiter(Waiter waiter) {
    waiters.remove(waiter);
  }

  public void send(HttpServletResponse response) throws MessageBucketException {
    try {
      if (ready) {
        ExtendedJSONWriter e = new ExtendedJSONWriter(response.getWriter());
        e.valueMap(messagePayload);
      } else {
        throw new MessageBucketException("Message not ready");
      }
    } catch (IOException e) {
      throw new MessageBucketException(e.getMessage(), e);
    } catch (JSONException e) {
      throw new MessageBucketException(e.getMessage(), e);
    }
  }

  public void unbind(String token, HttpServletRequest request) {
    bound.put(token, request);
  }

  public void bind(String token, HttpServletRequest request) {
    bound.remove(token);
  }

  public void markReady(Map<String, Object> messagePayload) {
    this.messagePayload = messagePayload;
    ready = true;
    for (Waiter w : ImmutableSet.copyOf(waiters.keySet())) {
      w.resume();
    }
  }

}
