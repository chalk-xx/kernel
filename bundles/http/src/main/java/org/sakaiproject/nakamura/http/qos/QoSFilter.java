package org.sakaiproject.nakamura.http.qos;

import org.apache.commons.lang.StringUtils;
import org.apache.felix.http.api.ExtHttpService;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.sling.commons.osgi.OsgiUtil;
import org.mortbay.util.ajax.Continuation;
import org.mortbay.util.ajax.ContinuationSupport;
import org.osgi.service.component.ComponentContext;
import org.sakaiproject.nakamura.api.http.qos.QoSToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Provides a configurable QoS filter that manages request in and out of the server, based on number of concurrent requests. Pending requests are prioritiezed into queue, taken out of the queue based on priority and time of entry. If the request does not match any Quality of Service Configuration, the default configuration will be used.
 * Configuration is via the
 */
@Component(immediate = true, metatype = true)
@Properties(value = {
    @Property(name = "service.description", value = "Nakamura Quality of Service Filter"),
    @Property(name = "service.vendor", value = "The Sakai Foundation") })
public class QoSFilter implements Filter {


  /**
   * Priority of this filter, higher number means sooner
   */
  @Property(intValue=10)
  private static final String FILTER_PRIORITY_CONF = "filter.priority";
  /**
   * Default time (ms) to wait for the semaphore request to be handled.
   */
  @Property(longValue=50)
  private static final String QOS_SEMAPHOREWAIT_CONF = "qos.semaphorewait";
  /**
   * Default timeout for a request.
   */
  @Property(longValue=60000)
  private static final String QOS_DEFAULT_REQUEST_TIMEOUT_CONF = "qos.default.requestTimeout";
  /**
   * Default number of concurrent requests, where no configuration matches the request.
   */
  @Property(intValue=10)
  private static final String QOS_DEFAULT_LIMIT_CONF = "qos.default.limit";
  /**
   * Default priority for requests not configured.
   */
  @Property(intValue=2)
  private static final String QOS_DEFAULT_PRIORITY_CONF = "qos.default.priority";
  /**
   * The maximum priority queue number (default , 0 is the lowest priority)
   */
  @Property(intValue=2)
  private static final String QOS_MAX_PRIORITY_CONF = "qos.priority";
  /**
   * The default timeout to be used on suspended requests (default 60000, 60s)
   */
  @Property(longValue=60000L)
  private static final String QOS_TIMEOUT_CONFIG = "qos.timeout";
  /**
   * List of categories, formatted as pathSpec;maxrequests;priority;timeout
   * pathSpec is of the form method:firstPathElement
   * method is * or a HTTP method in lower case (get,post,etc)
   * maxrequests is the maximum number of concurrent requests
   * priority is the queue the request is put into if suspended (0 is the lowest) (optional)
   * timeout is the time (ms) the request will wait if suspended. (optional)
   */
  @Property(value={})
  private static final String QOS_CATEGORIES_CONFIG = "qos.categories";
  private static final Logger LOGGER = LoggerFactory.getLogger(QoSFilter.class);
  private String suspendedAttributeName=this.getClass().getName()+this.hashCode();

  @Reference
  protected ExtHttpService extHttpService;

  private long waitMs;


  private Queue<Continuation>[] priorityQueue;
  private QoSControl defaultQoSControl;
  private Map<String, QoSControl> qoSControMap = new ConcurrentHashMap<String, QoSControl>();

  public void init(FilterConfig filterConfig) throws ServletException {
  }

  public void destroy() {

  }

  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    boolean accepted = false;
    QoSToken qoSToken  = getQoSControl(request);
    try {
      if (request.getAttribute(suspendedAttributeName) == null) {
        accepted = qoSToken.acquire(waitMs);
        if (accepted) {
          request.setAttribute(suspendedAttributeName, Boolean.FALSE);
        } else {
          request.setAttribute(suspendedAttributeName, Boolean.TRUE);
          Continuation continuation = ContinuationSupport.getContinuation((HttpServletRequest)request, qoSToken.getMutex());
          continuation.suspend(qoSToken.getSuspendTime());
          qoSToken.queue(continuation);
          return;
        }
      } else {
        Boolean suspended = (Boolean) request.getAttribute(suspendedAttributeName);

        if (suspended.booleanValue()) {
          request.setAttribute(suspendedAttributeName, Boolean.FALSE);
          if (request.getAttribute("javax.servlet.resumed") == Boolean.TRUE) {
            qoSToken.acquire();
            accepted = true;
          } else {
            // Timeout! try 1 more time.
            accepted = qoSToken.acquire(waitMs);
          }
        } else {
          // pass through resume of previously accepted request
          qoSToken.acquire();
          accepted = true;
        }
      }

      if (accepted) {
        chain.doFilter(request, response);
      } else {
        ((HttpServletResponse) response)
            .sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
      }
    } catch (InterruptedException e) {
      LOGGER.warn("QoS", e);
      ((HttpServletResponse) response)
          .sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
    } finally {
      if (accepted) {
        qoSToken.release();
      }
    }
  }

  private QoSToken getQoSControl(ServletRequest request) {
    QoSToken control = (QoSToken) request.getAttribute(QoSToken.CONTROL_ATTR);
    if  ( control != null ) {
      return control;
    }

    QoSControl qoSControl = null;

    HttpServletRequest hrequest = (HttpServletRequest) request;
    String requestPath = hrequest.getRequestURI();
    String[] pathElements = StringUtils.split(requestPath, "/", 1);
    if ( pathElements != null && pathElements.length > 0 ) {
      String method = StringUtils.lowerCase(hrequest.getMethod());

      qoSControl = qoSControMap.get(method+":"+pathElements[0]);
      if ( qoSControl == null ) {
        qoSControl = qoSControMap.get("*:"+pathElements[0]);
      }
    }
    if ( qoSControl == null ) {
      qoSControl =  defaultQoSControl;
    }
    QoSToken qoSToken = new QoSTokenImpl(qoSControl, request);
    request.setAttribute(QoSToken.CONTROL_ATTR, qoSToken);
    return qoSToken;
  }


  /**
   * @param componentContext
   * @throws ServletException
   */
  @SuppressWarnings("unchecked")
  @Activate
  protected void activate(ComponentContext componentContext) throws ServletException {

    Dictionary<String, Object> properties = componentContext.getProperties();

    long defaultTimeout = OsgiUtil.toInteger(properties.get(QOS_TIMEOUT_CONFIG),-1);
    int maxPriorityNumber = OsgiUtil.toInteger(properties.get(QOS_MAX_PRIORITY_CONF),2);
    priorityQueue = new Queue[maxPriorityNumber+1];
    for ( int i = 0; i < priorityQueue.length; i++ ) {
      priorityQueue[i] = new ConcurrentLinkedQueue<Continuation>();
    }

    // path, max requests, priority, timeout
    qoSControMap.clear();
    String[] qosLocations = OsgiUtil.toStringArray(properties.get(QOS_CATEGORIES_CONFIG));
    if ( qosLocations != null ) {
      for ( String qosLocation : qosLocations ) {
        String[] settings = StringUtils.split(qosLocation,";");
        if ( settings != null ) {
          if ( settings.length > 3 ) {
            qoSControMap.put(settings[0], new QoSControl(priorityQueue, Integer.parseInt(settings[1]), Integer.parseInt(settings[2]), Long.parseLong(settings[3])));
          } else if ( settings.length > 2 ) {
            qoSControMap.put(settings[0], new QoSControl(priorityQueue, Integer.parseInt(settings[1]), Integer.parseInt(settings[2]), defaultTimeout));
          } else if ( settings.length > 2 ) {
            qoSControMap.put(settings[0], new QoSControl(priorityQueue, Integer.parseInt(settings[1]), maxPriorityNumber, defaultTimeout));
          }
        }
      }
    }

    // defaults
    int qosDefaultPriority = OsgiUtil.toInteger(properties.get(QOS_DEFAULT_PRIORITY_CONF),0);
    int qosDefaultLimit = OsgiUtil.toInteger(properties.get(QOS_DEFAULT_LIMIT_CONF),10);
    long qosDefaultTimeout = OsgiUtil.toLong(properties.get(QOS_DEFAULT_REQUEST_TIMEOUT_CONF),defaultTimeout);
    waitMs = OsgiUtil.toLong(properties.get(QOS_SEMAPHOREWAIT_CONF),50);

    defaultQoSControl = new QoSControl(priorityQueue, qosDefaultLimit, qosDefaultPriority, qosDefaultTimeout);

    int filterPriority = OsgiUtil.toInteger(properties.get(FILTER_PRIORITY_CONF),10);

    extHttpService.registerFilter(this, ".*", null, filterPriority, null);

  }

  @Deactivate
  public void deactivate(ComponentContext componentContext) {
    extHttpService.unregisterFilter(this);
    // resume any continuations held by the filter
    for (int p = priorityQueue.length; p-- > 0;) {
      Continuation continutaion = priorityQueue[p].poll();
      if (continutaion != null && continutaion.isResumed()) {
        continutaion.resume();
        break;
      }
    }
  }

}
