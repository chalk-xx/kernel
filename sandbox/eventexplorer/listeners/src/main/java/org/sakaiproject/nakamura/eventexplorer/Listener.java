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
import org.apache.cassandra.thrift.Column;
import org.apache.cassandra.thrift.ColumnOrSuperColumn;
import org.apache.cassandra.thrift.ConsistencyLevel;
import org.apache.cassandra.thrift.SuperColumn;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jms.Message;
import javax.jms.MessageListener;

public class Listener extends Base implements MessageListener {

  private static int id;

  public Listener() {

    id = 0;
  }

  public static String getid() {
    String iden = "Message" + id;
    id = id + 1;
    return iden;
  }

  public void onMessage(Message message) {
    try {

      Cassandra.Client client = setupConnection();
      Map<String, List<ColumnOrSuperColumn>> job = new HashMap<String, List<ColumnOrSuperColumn>>();
      List<ColumnOrSuperColumn> columns = new ArrayList<ColumnOrSuperColumn>();
      List<Column> column_list = new ArrayList<Column>();

      Enumeration en = message.getPropertyNames();
      System.out.println("Message");
      while (en.hasMoreElements()) {
        String prop_name = (String) en.nextElement();
        Object obj = message.getObjectProperty(prop_name);
        String obj_val = obj.toString();
        System.out.println(prop_name + "        " + obj_val);

        long timestamp = System.currentTimeMillis();
        Column col = new Column(prop_name.getBytes(ENCODING), obj_val.getBytes(ENCODING),
            timestamp);
        column_list.add(col);
      }
      System.out.println();

      SuperColumn column = new SuperColumn(getid().getBytes(ENCODING), column_list);
      ColumnOrSuperColumn columnOrSuperColumn = new ColumnOrSuperColumn();
      columnOrSuperColumn.setSuper_column(column);
      columns.add(columnOrSuperColumn);

      job.put(COLUMN_FAMILY_USERS, columns);
      client.batch_insert(KEYSPACE, "User1", job, ConsistencyLevel.ALL);

      closeConnection();

    } catch (Exception e) {
      e.printStackTrace();
    }

  }

}
