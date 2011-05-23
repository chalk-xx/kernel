package org.sakaiproject.nakamura.search.solr;

import com.google.common.collect.UnmodifiableIterator;

import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.util.NamedList;
import org.sakaiproject.nakamura.api.search.solr.Result;
import org.sakaiproject.nakamura.api.search.solr.SolrQueryResponseWrapper;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchResultSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;


public class SolrSearchResultSetImpl implements SolrSearchResultSet, SolrQueryResponseWrapper {

  private static final Logger LOGGER = LoggerFactory.getLogger(SolrSearchResultSetImpl.class);

  private final QueryResponse queryResponse;
  private SolrDocumentList responseList;

  public SolrSearchResultSetImpl(QueryResponse queryResponse) {
    LOGGER.debug("new SolrSearchResultSetImpl(QueryResponse {})", queryResponse);
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
  private void loadGroupedResponse(NamedList<Object> response) {
    NamedList<Object> grouped = (NamedList<Object>) response.get("grouped");
    if (grouped.size() > 0) {
      NamedList<Object> groupings = (NamedList<Object>) grouped.getVal(0);
      // have to set this manually
      long numFound = (Integer) groupings.get("matches");
      responseList.setNumFound(numFound);
      List<NamedList<Object>> groups = (List<NamedList<Object>>) groupings.get("groups");

      for (NamedList<Object> group : groups) {
        SolrDocumentList docList = (SolrDocumentList) group.get("doclist");
        responseList.addAll(docList);
      }
    }
  }


  @SuppressWarnings("unchecked")
  private void loadMoreLikeThisResponse(NamedList<Object> response) {
    List<SolrDocument> resultDocs = new ArrayList<SolrDocument>();

    NamedList<Object> mlts = (NamedList<Object>) response.get("moreLikeThis");

    if (mlts.size() > 0) {
      for (Map.Entry<String,Object> mlt : mlts) {
        for (SolrDocument doc : (SolrDocumentList) mlt.getValue()) {
          resultDocs.add(doc);
        }
      }
    }

    Comparator scoreSorter = new Comparator<SolrDocument>() {
      public int compare(SolrDocument doc1, SolrDocument doc2) {
        return ((Float) doc1.getFieldValue("score")).compareTo((Float) doc2.getFieldValue("score"));
      }
    };

    // Sort our doc list by score (from lowest score to highest)
    Collections.sort(resultDocs, scoreSorter);

    Map<String,SolrDocument> deDupedDocs = new HashMap<String,SolrDocument>();
    // And de-dupe based on the ID field.  Where there are multiple occurrences
    // of a document, we keep the instance with the highest score.
    for (SolrDocument doc : resultDocs) {
      deDupedDocs.put((String) doc.getFieldValue("id"), doc);
    }

    responseList.setNumFound(deDupedDocs.values().size());
    responseList.addAll(deDupedDocs.values());

    // One final sort to get the results into descending score order...
    Collections.sort(responseList, scoreSorter);
    Collections.reverse(responseList);
  }


  private void loadResponse() {
    if (responseList == null) {
      // null list so let's try to load it
      NamedList<Object> response = queryResponse.getResponse();
      responseList = new SolrDocumentList();

      if (response.get("moreLikeThis") != null) {
        // The moreLikeThis response will contain a regular result set, but it's
        // the extra stuff we're interested in.
        loadMoreLikeThisResponse(response);
      } else {
        responseList = queryResponse.getResults();
        if (responseList == null) {
          // will be null if search was grouped
          responseList = new SolrDocumentList();
          // Must be one of our alternative query types.
          if (response.get("grouped") != null) {
            loadGroupedResponse(response);
          }
        }
      }
    }
  }


  public QueryResponse getQueryResponse() {
    return this.queryResponse;
  }


}
