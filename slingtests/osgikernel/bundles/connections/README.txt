The Connections management service manages connections for the user.

It uses a bigstore underneath the users public space protected by a group to store 
the connections.
The group consists of all those who are able to see the connections owned by the user.

When a connection is created a node is created in the bigstore that contains the 
properties of the connection.
There are servlets that manage the state of these connections.

The big store implementation follows the pattern used in messaging.


Friends connections have connection status on each side of the connection.

connect request

 User A requests Connection to User B
 A record is added to user A and User B's space as pending
 User A's state marked as pending
 User B's state marked as invited
 

connect accept
 Only on a local connection state of invited and remote of pending.
 User B can accept the Connection in which case the status of both sides of the connection is set to connected.
 

connect reject
 Only on a local connection state of invited and remote of pending.
 User B can reject the Connection in which case the status of both sides of the connection is set to rejected.

connect ignore 
 Only on a local connection state of invited and remote of pending.
 User B can ignore the Connection in which case the status of the local side is marked ignored, and the remote side remains pending.

connect block 
 Only on a local connection state of invited and remote of pending.
 User B can block the Connection in which case the status of the local side is marked blocked, and the remote side remains pending.
 Once a local connection state is in blocked state, it cannot be changed except by an admin user.

connect cancel
 Only on a local connection state of pending and remote of invited or ignore.
 User A can cancel the setting both sides to canceled.
 
connect remove
 Only on a local connection = remote = connected | rejected
 
 

