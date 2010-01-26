package org.sakaiproject.kernel.api.files;

import com.caucho.hessian.client.HessianProxyFactory;
import com.caucho.hessian.client.HessianRuntimeException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.util.Collection;

import edu.nyu.XythosRemote;

import javax.jcr.Node;
import javax.jcr.query.Query;

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

  public static Collection searchFiles(Query query) {
    try {
      HessianProxyFactory factory = new HessianProxyFactory();
      XythosRemote xythos = (XythosRemote) factory.create(XythosRemote.class, url, XythosUtils.class.getClassLoader());
      Collection rv = xythos.findFiles(query.getStatement());
      return rv;
    } catch (MalformedURLException e) {
      throw new RuntimeException("MalformedURLException: " + e.getMessage());
    }
  }

}
