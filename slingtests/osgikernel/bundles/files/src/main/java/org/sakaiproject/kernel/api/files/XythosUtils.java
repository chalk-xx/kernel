package org.sakaiproject.kernel.api.files;

import com.caucho.hessian.client.HessianProxyFactory;

import org.apache.jackrabbit.commons.iterator.RowIteratorAdapter;
import org.apache.jackrabbit.value.StringValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import edu.nyu.XythosRemote;

import javax.jcr.ItemNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.query.Query;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;

public class XythosUtils {
  
  public static final Logger log = LoggerFactory.getLogger(XythosUtils.class);
  
  private static final String url = "http://localhost:9090/remoting/remoting/XythosService";
  
  public static Boolean ping() {
    try {
      HessianProxyFactory factory = new HessianProxyFactory();
      XythosRemote xythos = (XythosRemote) factory.create(XythosRemote.class, url, XythosUtils.class.getClassLoader());
      return xythos.ping();
    } catch (MalformedURLException e) {
      throw new RuntimeException(e.getMessage());
    }
  }
  
  /**
   * Save a file.
   * 
   * @param session
   * @param path
   * @param id
   * @param is
   * @param fileName
   * @param contentType
   * @param slingRepository
   * @return
   * @throws MalformedURLException 
   */
  public static String saveFile(String path, String id, byte[] contentBytes, String fileName, String contentType, String userId) {
    
    try {
      HessianProxyFactory factory = new HessianProxyFactory();
      XythosRemote xythos = (XythosRemote) factory.create(XythosRemote.class, url, XythosUtils.class.getClassLoader());
      String rv = xythos.saveFile(path, id, contentBytes, fileName, contentType, userId);
      return rv;
    } catch (MalformedURLException e) {
      throw new RuntimeException("MalformedURLException: " + e.getMessage());
    }
  }

  public static RowIterator searchFiles(Query query) {
    try {
      Collection<Row> rowCollection = new ArrayList<Row>();
      String queryString = composeQueryForXythos(query);
      HessianProxyFactory factory = new HessianProxyFactory();
      XythosRemote xythos = (XythosRemote) factory.create(XythosRemote.class, url, XythosUtils.class.getClassLoader());
      Map<String,String> foo = xythos.getProperties();
      Collection<Map<String,String>> result = xythos.findFilesWithXPath(queryString, "zach");	
      for (Map<String,String> rowMap : result) {
    	  rowCollection.add(rowFromMap(rowMap));
      }
      return new RowIteratorAdapter(rowCollection);
    } catch (Throwable e) {
      throw new RuntimeException("MalformedURLException: " + e.getMessage());
    }
  }

private static Row rowFromMap(final Map<String, String> map) {
	return new Row() {
		
		@Override
		public Value getValue(String propertyName)
				throws ItemNotFoundException, RepositoryException {
			return new StringValue(map.get(propertyName));
		}

		@Override
		public Value[] getValues() throws RepositoryException {
			List<Value> rv = new ArrayList<Value>();
			for(String value : map.values()) {
				rv.add(new StringValue(value));
			}
			return rv.toArray(new Value[rv.size()]);
		}
		
	};
}

private static String composeQueryForXythos(Query query) {
	// TODO do the right thing here
	return "//*[(@jcr:primaryType='nt:file')]";
}

}
