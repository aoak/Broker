Broker
======

Academic project in distributed computing using virtual synchrony


The program VSynchrony is devided into following classes with mentioned functionality.

NOTE: The project does not include the jgroups jar file or the client program.
The compilation using makefile assumes that the jgroup jar file is present in current
directory.


VSynchrony:
-----------

This application is to maintain a consistent and fault tolerant stock broker
system using reliable multi-cast using JGroups. This class creates the account and
stock information, updates them with the state received after connecting jchannel.
It then starts a server thread to listen on a server port accepting connections.
The server listener spawns worker threads to accept client request.

The eventloop method in the VSynchrony class has the main logic of the broker.
It processes incoming requests and updates the stock's buy and sell list. Then
do\_trade method checks the stock for the possibility of a tread using the limit
order logic. If a tread is possible, the tread is commited, updating the accounts
of the clients and the price of the share. Then the two requests are removed
from stock's buy and sell lists.


eventloop(): This method has the main logic of a stock server application. It 
creates the server socket to listen for the requests from clients. It then
runs in an infinite loop to serve the incoming requests.


process\_req(): This method checks the processing queue and forms the request objects
from the strings received from the clients or other servers. It then
puts those requests in appropriate stock so that trading function will be
able to process them properly.


do\_trade(): This method iterates over all the stocks and checks if any trade is possible
looking at their buyList and sellList. 
If the trade is possible, it commits the trade, removing the particular requests from the
stock's queues.


process\_history(): This method processes the history we have received. We still have to maintain the history,
so we need to pop all the history in request processing queue and still keep the list
having the request history intact.
			 	


Account
-------

The class for the account information for the clients. It has
client id, cash, total balance and a hashmap having all the
stocks owned by this client.

The accounts of the clients is updated each time a tread is successful.


SocketServerThread
------------------

This class implements a thread which listens to a server socket provided
to it for connections from clients. When it receives a connection, it spawns
a minion thread to handle the communication and continues to listen for more
clients.


ReqComparator
--------------

This is class which implements the comparator for different requests.
it can be used to sort the priority queue in ascending or descending order.


Request
-------

The class for a request. It has the id of the client who has made a request,
type of request (BUY/SELL), offer that the client is making, number of shares
he wants to tread and the symbol.

Objects of this class are queued in stock's buy and sell lists.


SocketThread
------------

This is the thread to handle the communication on a particular socket. It handles only
incoming messages and has no outgoing capacity. When it receives a message, it puts that
in the synchronized queue to the broker thread.


Stock
-----

The class for the information about a stock. It has name of the
company, symbol, number of shares, the price and two sorted queues having sell and buy lists.

trade() : This method goes through the buy list and sell list. If it has a
matching limit order which makes the tread possible, then it returns those two requests. 
otherwise returns nulls in the array.
