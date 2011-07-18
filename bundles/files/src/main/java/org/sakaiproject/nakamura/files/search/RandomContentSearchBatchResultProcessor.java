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
package org.sakaiproject.nakamura.files.search;

import static org.sakaiproject.nakamura.api.search.solr.SolrSearchConstants.PARAMS_ITEMS_PER_PAGE;

import com.google.common.collect.Lists;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.commons.osgi.OsgiUtil;
import org.sakaiproject.nakamura.api.search.solr.Query;
import org.sakaiproject.nakamura.api.search.solr.Result;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchBatchResultProcessor;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchConstants;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchException;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchResultSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Batch result processor for a random selection of content. It is expected that this
 * processor will never have to deal with more than just a few entries. As of the time of
 * writing, the defaults are to return 4 entries but search for 4x the number of items to
 * return (16 total) for a better selection of random content. These settings were
 * specified by the UI team as what is needed for the random content carousel.
 */
@Component(inherit = true, metatype=true)
@Properties(value = {
    @Property(name = "service.vendor", value = "The Sakai Foundation"),
    @Property(name = SolrSearchConstants.REG_BATCH_PROCESSOR_NAMES, value = "RandomContent")
})
@Service(value = SolrSearchBatchResultProcessor.class)
public class RandomContentSearchBatchResultProcessor extends LiteFileSearchBatchResultProcessor {


  /*
   * Increase the numbers of items the solr query returns by this amount.
   */
  @Property(name = "solrItemsMultiplier", intValue = 4)
  private int solrItemsMultiplier;

  public static final Logger LOGGER = LoggerFactory
  .getLogger(RandomContentSearchBatchResultProcessor.class);


  public SolrSearchResultSet getSearchResultSet(SlingHttpServletRequest request, Query query) throws SolrSearchException {

    Map<String, String> options = query.getOptions();

    // find the number of items solr has be requested to return.
    String originalItems = options.get(PARAMS_ITEMS_PER_PAGE); // items
    int originalItemsInt = Integer.parseInt(originalItems);

    // increase the number of items solr will return.
    int newItemsInt = originalItemsInt * solrItemsMultiplier;

    // increase the maximum items returned by solr
    query.getOptions().put(PARAMS_ITEMS_PER_PAGE, Integer.toString(newItemsInt));

    // do the query
    SolrSearchResultSet rs = searchServiceFactory.getSearchResultSet(request, query);

    // set query option back to original value
    query.getOptions().put(PARAMS_ITEMS_PER_PAGE, originalItems);

    // split up the results into prioritized (has description, tag or preview)
    // and standard results so we can return a randomized list with as many
    // prioritized results as possible.
    List<Result> priorityResults = Lists.newArrayList();
    List<Result> standardResults = Lists.newArrayList();

    Iterator<Result> results = rs.getResultSetIterator();
    while (results.hasNext()) {
      Result result = results.next();
      if (result.getFirstValue("description") != null
          || result.getFirstValue("tag") != null
          || result.getFirstValue("taguuid") != null
          || result.getFirstValue("hasPreview") != null) {
        priorityResults.add(result);
      } else {
        standardResults.add(result);
      }
    }

    // choose random entries to return
    List<Result> retval = chooseRandomResults(priorityResults, originalItemsInt);

    // fill up the pick list with extras if we don't have enough priority results.
    retval.addAll(chooseRandomResults(standardResults, originalItemsInt - retval.size()));

    // create new SolrSearchResultSet object, to be returned by this method.
    SolrSearchResultSet randomSolrResultSet = new RandomContentSolrSearchResultSetImpl(retval);
    return randomSolrResultSet;
  }

  /**
   * When the bundle gets activated we retrieve the OSGi properties.
   *
   * @param props
   */
  protected void activate(Map<?, ?> props) {
    solrItemsMultiplier = OsgiUtil.toInteger(props.get("solrItemsMultiplier"), 4);
  }

  // inner class, use by method getSearchResultSet(..),
  // to return a object of type SolrSearchResultSet
  private class RandomContentSolrSearchResultSetImpl implements SolrSearchResultSet {
    private List<Result> results;

    public RandomContentSolrSearchResultSetImpl(List<Result> results) {
      this.results = results;
    }

    public Iterator<Result> getResultSetIterator() {
      return results.iterator();
    }

    public long getSize() {
      return results.size();
    }
  }

  /**
   * Randomly choose a set of entries.
   *
   * @param results
   *          The list to pick from. This list is not modified.
   * @param numToChoose
   *          The number of entries to pick. The limit is set to Math.min(results.size(),
   *          numToChoose) to not go out of bounds.
   * @return
   */
  private List<Result> chooseRandomResults(List<Result> results, int numToChoose) {
    List<Result> picks = Lists.newArrayList();
    if (numToChoose > 0) {
      ArrayList<Result> pickList = Lists.newArrayList(results);
      Random rand = new Random();
      int limit = Math.min(pickList.size(), numToChoose);
      for (int i = 0; i < limit; i++) {
        // Pick the next integer limited to the original limit minus the number
        // removed from the list. This keeps the picks from going out of bounds.
        int choose = rand.nextInt(limit - i);
        picks.add(pickList.remove(choose));
      }
    }
    return picks;
  }
}
