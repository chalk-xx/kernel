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
package org.sakaiproject.nakamura.eventexplorer;

import org.apache.cassandra.thrift.Cassandra;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;

public class Base {

  protected static final String KEYSPACE = "SAKAI";
  protected static final String COLUMN_FAMILY_USERS = "Users";

  protected static final String ENCODING = "utf-8";
  static long timestamp;
  protected static TTransport tr = null;

  /**
   * Open up a new connection to the Cassandra Database.
   * 
   * @return the Cassandra Client
   */
  protected static Cassandra.Client setupConnection() {
    try {
      tr = new TSocket("localhost", 9160);
      TProtocol proto = new TBinaryProtocol(tr);
      Cassandra.Client client = new Cassandra.Client(proto);
      tr.open();

      return client;
    } catch (TTransportException exception) {
      exception.printStackTrace();
    }

    return null;
  }

  /**
   * Close the connection to the Cassandra Database.
   */
  protected static void closeConnection() {
    try {
      tr.flush();
      tr.close();
    } catch (TTransportException exception) {
      exception.printStackTrace();
    }
  }

}
