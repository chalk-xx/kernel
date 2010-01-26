package org.sakaiproject.kernel.files;

import org.apache.jackrabbit.commons.iterator.RowIteratorAdapter;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.sakaiproject.kernel.api.files.XythosUtils;
import org.sakaiproject.kernel.api.search.AbstractSearchResultSet;
import org.sakaiproject.kernel.api.search.Aggregator;
import org.sakaiproject.kernel.api.search.SearchException;
import org.sakaiproject.kernel.api.search.SearchResultProcessor;
import org.sakaiproject.kernel.api.search.SearchResultSet;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Map;

import javax.jcr.ItemNotFoundException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.query.Query;
import javax.jcr.query.Row;

/**
 * Formats message node search results
 * 
 * @scr.component immediate="true" label="XythosFilesSearchResultProcessor"
 *                description="Formatter for Xythos files search results"
 * @scr.property name="service.vendor" value="The Sakai Foundation"
 * @scr.property name="sakai.search.processor" value="XythosFiles"
 * @scr.service interface="org.sakaiproject.kernel.api.search.SearchResultProcessor"
 */
public class XythosFilesSearchResultProcessor implements SearchResultProcessor {

  public SearchResultSet getSearchResultSet(SlingHttpServletRequest request, Query query)
      throws SearchException {
    Collection<Row> rows = new ArrayList<Row>();
    Collection<Map<String,String>> xythosResults = XythosUtils.searchFiles(query);
    for (Map<String,String> result : xythosResults) {
      final String filename = result.get("sakai:filename");
      rows.add(new Row() {

        public Value getValue(String propertyName) throws ItemNotFoundException,
            RepositoryException {
          return new Value() {

            public boolean getBoolean() throws ValueFormatException,
                IllegalStateException, RepositoryException {
              // TODO Auto-generated method stub
              return false;
            }

            public Calendar getDate() throws ValueFormatException, IllegalStateException,
                RepositoryException {
              // TODO Auto-generated method stub
              return null;
            }

            public double getDouble() throws ValueFormatException, IllegalStateException,
                RepositoryException {
              // TODO Auto-generated method stub
              return 0;
            }

            public long getLong() throws ValueFormatException, IllegalStateException,
                RepositoryException {
              return PropertyType.STRING;
            }

            public InputStream getStream() throws IllegalStateException,
                RepositoryException {
              // TODO Auto-generated method stub
              return null;
            }

            public String getString() throws ValueFormatException, IllegalStateException,
                RepositoryException {
              // TODO Auto-generated method stub
              return null;
            }

            public int getType() {
              // TODO Auto-generated method stub
              return 0;
            }
            
          };
        }

        public Value[] getValues() throws RepositoryException {
          // TODO Auto-generated method stub
          return null;
        }
        public 
      })
    }
    return new AbstractSearchResultSet(new RowIteratorAdapter(xythosResults), xythosResults.size());
  }

  public void writeNode(SlingHttpServletRequest request, JSONWriter write,
      Aggregator aggregator, Row row) throws JSONException, RepositoryException {
    // TODO Auto-generated method stub

  }

}
