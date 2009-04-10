package org.sakaiproject.kernel.persistence.dbcp;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

public class DataSourceWrapper implements DataSource {

  DataSource wrapped;
  
  public Connection getConnection() throws SQLException {
    return wrapped.getConnection();
  }

  public Connection getConnection(String username, String password) throws SQLException {
    return wrapped.getConnection();
  }

  public int getLoginTimeout() throws SQLException {
    return wrapped.getLoginTimeout();
  }

  public PrintWriter getLogWriter() throws SQLException {
    return wrapped.getLogWriter();
  }

  public void setLoginTimeout(int seconds) throws SQLException {
    wrapped.setLoginTimeout(seconds);
  }

  public void setLogWriter(PrintWriter out) throws SQLException {
    wrapped.setLogWriter(out);
  }

  public DataSourceWrapper(DataSource wrapped)
  {
    this.wrapped = wrapped;
  }
  
  public boolean isWrapperFor(Class<?> c)
  {
    if (c.equals(wrapped.getClass()))
    {
      return true;
    }
    return false;
  }
  
  @SuppressWarnings("unchecked")
  public <T> T unwrap(Class<T> c) throws SQLException
  {
    if (isWrapperFor(c))
    {
      return (T)wrapped;
    }
    throw new SQLException();
  }
}
