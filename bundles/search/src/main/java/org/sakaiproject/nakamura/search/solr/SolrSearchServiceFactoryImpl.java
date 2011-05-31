package org.sakaiproject.nakamura.search.solr;

import com.google.common.collect.Maps;

import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.commons.osgi.OsgiUtil;
import org.sakaiproject.nakamura.api.search.solr.Query;
import org.sakaiproject.nakamura.api.search.solr.ResultSetFactory;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchException;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchResultSet;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentMap;

@Component
@Service
public class SolrSearchServiceFactoryImpl implements SolrSearchServiceFactory {

  private static final Logger LOGGER = LoggerFactory.getLogger(SolrSearchServiceFactoryImpl.class);

  @Reference(referenceInterface = ResultSetFactory.class, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC)
  private ConcurrentMap<String, ResultSetFactory> resultSetFactories = Maps.newConcurrentHashMap();

  protected void bindResultSetFactories(ResultSetFactory factory, Map<?, ?> props) {
    String type = OsgiUtil.toString(props.get("type"), null);
    if (!StringUtils.isBlank(type)) {
      resultSetFactories.put(type, factory);
    } else {
      LOGGER.warn("Unable to bind result set factory: no 'type' property [{}]", factory);
    }
  }

  protected void unbindResultSetFactories(ResultSetFactory factory, Map<?, ?> props) {
    String type = OsgiUtil.toString(props.get("type"), null);
    if (!StringUtils.isBlank(type)) {
      resultSetFactories.remove(type);
    }
  }

  public SolrSearchResultSet getSearchResultSet(SlingHttpServletRequest request,
      Query query, boolean asAnon) throws SolrSearchException {
    SolrSearchResultSet rs = null;
    ResultSetFactory factory = resultSetFactories.get(query.getType());
    if (factory != null) {
      rs = factory.processQuery(request, query, asAnon);
    }
    return rs;
  }

  public SolrSearchResultSet getSearchResultSet(SlingHttpServletRequest request,
      Query query) throws SolrSearchException {
    return getSearchResultSet(request, query, false);
  }
}
