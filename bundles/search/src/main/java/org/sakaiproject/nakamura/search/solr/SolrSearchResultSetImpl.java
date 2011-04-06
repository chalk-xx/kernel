package org.sakaiproject.nakamura.search.solr;

import com.google.common.collect.UnmodifiableIterator;

import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.util.NamedList;
import org.sakaiproject.nakamura.api.search.solr.Result;
import org.sakaiproject.nakamura.api.search.solr.SolrQueryResponseWrapper;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchResultSet;

import java.util.Iterator;
import java.util.List;

public class SolrSearchResultSetImpl implements SolrSearchResultSet, SolrQueryResponseWrapper {

  private QueryResponse queryResponse;
  private SolrDocumentList responseList;
  private long numFound = -1L;

  public SolrSearchResultSetImpl(QueryResponse queryResponse) {
    this.queryResponse = queryResponse;
  }

  public Iterator<Result> getResultSetIterator() {
    loadResponse();
    final Iterator<SolrDocument> solrIterator = (responseList != null) ? responseList
        .iterator() : null;
    return new UnmodifiableIterator<Result>() {

      public boolean hasNext() {
        return solrIterator != null && solrIterator.hasNext();
      }

      public Result next() {
        return new ResultImpl(solrIterator.next());
      }
      
    };
  }


  public long getSize() {
    loadResponse();
    return (responseList == null) ? 0 : responseList.getNumFound();
  }
  
  @SuppressWarnings("unchecked")
  private void loadResponse() {
    if (responseList == null) {
      // null list so let's try to load it
      responseList = queryResponse.getResults();
      if (responseList != null) {
        // standard search result when grouping is not applied
        numFound = responseList.getNumFound();
      } else {
        // will be null if search was grouped
        responseList = new SolrDocumentList();

        NamedList<Object> response = queryResponse.getResponse();
        NamedList<Object> grouped = (NamedList<Object>) response.get("grouped");
        if (grouped.size() > 0) {
          NamedList<Object> groupings = (NamedList<Object>) grouped.getVal(0);
          List<NamedList<Object>> groups = (List<NamedList<Object>>) groupings.get("groups");

          for (NamedList<Object> group : groups) {
            SolrDocumentList docList = (SolrDocumentList) group.get("doclist");
            responseList.addAll(docList);
          }
        }

        // have to set this manually
        responseList.setNumFound(responseList.size());
      }
    }
  }

  public QueryResponse getQueryResponse() {
    return this.queryResponse;
  }


}
