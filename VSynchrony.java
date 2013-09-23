import java.io.*;
import java.net.*;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingQueue;

import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import org.jgroups.View;
import org.jgroups.util.Util;

/* This application is to maintain a consistent and fault tolerant stock broker
 * system using reliable multi-cast using JGroups tutorial
 */

public class VSynchrony extends ReceiverAdapter {

	JChannel channel;		// Jchannel
	int port;				// port on which this server will listen to
	int pid;				// Process id of this server. (never used)
	int num_cli;			// number of clients this server is going to accept
	
	
	LinkedBlockingQueue<String> in_req_list;						// the queue for messages from sockets to main thread (synchronized)
	private LinkedBlockingQueue<String> req_list;					// the queue in which messages ready for processing are kept
	final LinkedList<Stock> stock_state = new LinkedList<Stock>();	// information having stock state
	final LinkedList<String> req_state = new LinkedList<String>();	// history of all the requests received by all servers to recreate the state
	final Account[] accnt_state;									// account information of all the clients
	
	
	int trade_num = 1;		// the counter for number of successful trades
	int order_num = 1;		// the counter for number of orders placed.
	
	
	
	
	
	
	/* constructor for VSynchrony class. Sets the process id, number of clients,
	 * server port number and initializes all the queues and arrays
	 */
	
	public VSynchrony (int process, int num_clients, int serv_port) {
		this.port = serv_port;
		this.pid = process;
		this.num_cli = num_clients;
		this.req_list = new LinkedBlockingQueue<String>();
		this.accnt_state = new Account[num_cli];
		this.in_req_list = new LinkedBlockingQueue<String>();
		
	}
	
	
	
	
	
	
	
	
	
	/* main: The main program execution starts here. This method itself just 
	 * calls the start method after creating the object of this class. 
	 * The setup of the JGroups framework is done in start method
	 */
	
	
	public static void main(String[] argv) throws Exception {
		
		int process = Integer.parseInt(argv[0]);
		int num_clients = Integer.parseInt(argv[1]);
		int serv_port = Integer.parseInt(argv[2]);
		
		new VSynchrony(process, num_clients, serv_port).start();
	}
	
	
	
	
	
	
	
	
	
	
	
	/* start: This method creates initial account and stock information,
	 * creates a channel, set receiver, join the cluster and hence get the
	 * state information. It then calls the method eventloop which has
	 * the logic of the stock broker. It also closes the channel after
	 * eventloop is finished.
	 */
	
	private void start() throws Exception {
		
		create_stock_state();
		create_accnt_state();
		
		channel=new JChannel("protocol.xml"); 			// added FLUSH in protocol for virtual synchrony
		channel.setReceiver(this);
		channel.connect("Aniket_Oak");
		channel.getState(null, 10000);
		eventLoop();
		channel.close();
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	/* eventloop: This method has the main logic of a stock server application. It 
	 * creates the server socket to listen for the requests from clients. It then
	 * runs in an infinite loop to serve the incoming requests.
	 */
		
	private void eventLoop () {
		
		ServerSocket s = null;
				
		try {
			/* create a server port on given port number. Then we need to create a thread
			 * which listens for the requests from clients. This server port thread will
			 * then create another thread to handle the communication and will continue to
			 * listen for the next client. We pass the server thread a queue for messages
			 * which in turn is passed to socket handling threads. This way the messages
			 * are passed from sockets to queues in consumer producer kind of model.
			 */
			s= new ServerSocket(port);
			Thread servThread = new Thread (new SocketServerThread(s,in_req_list));
			servThread.start();
			
			System.out.println("Now starting the broker loop");
			while (true) {
								
				if (! in_req_list.isEmpty()) {
					
					/* if the queue from sockets to main thread has a message, we have received
					 * a request from the client. We have to send this request to other servers
					 * using JChannel.
					 */
					System.out.println("Main thread received request");
					String req = in_req_list.poll();
					Message msg=new Message(null, null, req);
					channel.send(msg);
					
					/* notice that no processing of that message happens here. It is not put in
					 * process queue also. This is because when we send a message on JChannel, we
					 * also receive it. We will act as if it is new to us too. This will keep the
					 * possible paths from which messages can enter processing queue to 1.
					 */
				}            
	            
				if (! req_list.isEmpty()) {
					/* if the processing queue has something, then we better process
					 * it. :)
					 */
					process_req();
					do_trade();
				}
	            
	            
	            if (Thread.interrupted()) {
	            	/* if we are interrupted, then we should break out of the infinite
	            	 * service loop, do cleanup and exit.
	            	 */
	            	break;
	            }
	            
			}
			
			s.close();
		} catch (IOException ioe) {
			System.err.println("IO Error");
			ioe.printStackTrace();
		} catch (Exception e) {
			System.err.println("Error thrown by channel");
			e.printStackTrace();
		}
		
		
		
	}
	
	
	
	
	
	
	
	
	
	
	
	
	/* viewAccepted: This is a callback method in ReceiverAdapter. It receives the new view which
	 * is installed and prints it.
	 */
	
	public void viewAccepted (View view) {
	
		System.out.println("---------------------------------------------------------------------");
		System.out.println("The view of the cluster has changed");
		System.out.println("[New view] : " + view);
		System.out.println("---------------------------------------------------------------------");
		
	}
	
	
	
	
	
	
	
	
	
	/* receive: This is a callback method which is called when we receive a message from other 
	 * nodes in the cluster.
	 */
	
	
	public void receive (Message m) {
		
		String req_line = (String) m.getObject();
		
		System.out.println("Received request from server " + m.getSrc() + ": " + req_line);
		/* put the received the request in history as well as processing queue. This is the only
		 * path from which requests can enter the request processing queue.
		 */
		synchronized (req_list) {
			req_state.add(req_line);
			req_list.add(req_line);
		}
	}
	
	
	
	
	
	
	/* getState: This is a callback method by which a node in the cluster
	 * sends the state information to newly joining node. 
	 * 
	 * The state is in the form of request history. This way kind of sucks, but Jgroup only
	 * allows sending single data structure. We need both account information and stock state.
	 * This can be recreated from initial state using request history. That is why we need
	 * to send entire history. 
	 */
	
	public void getState (OutputStream out) throws Exception {
		
		System.out.println("Sending request history information");
		synchronized (req_state) {
			Util.objectToStream(req_state, new DataOutputStream(out));
		}
		
	}
	
	
	
	
	
	
	
	
	/* This call back method is called by the node newly joining the cluster.
	 * it receives the request history from other nodes.
	 */
	
	@SuppressWarnings("unchecked")
	public void setState (InputStream in) throws Exception {
		
		System.out.println("Receiving state information");
		LinkedList<String> list;
		list = (LinkedList<String>) Util.objectFromStream(new DataInputStream(in));
		
		
		synchronized (req_state) {
			req_state.clear();
			req_state.addAll(list);
		}
		System.out.println(req_state.size() + " operations in history");
		/* for recreating the state from initial stock and account info using the
		 * request history, we call this function.
		 * Need a check because if there is no request processed by any node when 
		 * this one joins, there will be null pointer exception.
		 */
		if (req_state.size() > 0)
			process_history();
		

	}
	
	
	
	
	
	
	
	
	
	
	/* Read the file index.properties and create the information about the stocks.
	 * this stock info will be modified by received request history if any
	 */
	
	private void create_stock_state () {
		
		FileReader f = null;
		BufferedReader in = null;
		System.out.println("Loading initial stock information");
		try {
			f = new FileReader("index.properties");
			in = new BufferedReader(f);
			
			String line;
			while ((line = in.readLine()) != null) {
				if (line.startsWith("#"))
					continue;
				
				String parts[] = line.split("\\t+");
				Stock s = new Stock(parts[1], parts[0], 10000, 100);
				stock_state.add(s);
			}
			in.close();
			
		} catch (IOException ioe) {
			System.err.println("Error creating initial stock state");
			ioe.printStackTrace();
		}
	}
	
	
	
	
	
	
	
	
	
	/* For all the clients (number received from command line), create an account
	 * this will then be modified by the request history (if any)
	 */
	
	private void create_accnt_state () {
		
		System.out.println("Loading initial account information");
		for (int i=0; i < num_cli; i++) {
			accnt_state[i] = new Account(i,10000);
		}
	}
	
	
	
	
	
	
	
	
	
	
	/* This method checks the processing queue and forms the request objects
	 * from the strings received from the clients or other servers. It then
	 * puts those requests in appropriate stock so that trading function will be
	 * able to process them properly.
	 */
	
	private void process_req() {
		
		
		while (! req_list.isEmpty()) {
			
			/* If we have a request string in processing queue, then we need to 
			 * split it and form proper request object. String is split on spaces
			 * and fields of interest are extracted from it.
			 */
			String req = req_list.poll();
			String[] parts = new String[5];
			parts = req.split("\\s+");
			
			int client = Integer.parseInt(parts[0].substring(1));
			String action = parts[1];
			String sym = parts[2];
			int price = Integer.parseInt(parts[3]);
			int num = Integer.parseInt(parts[4]);
			
			/* Now we need to print the order info as given in pdf. This is a little hard
			 * and the snippet in pdf is ambiguous. But this is how we print:
			 * -------------------------------------------------------------------------
			 * Order #18 C1 BUY GOOG 100 10
			 * Price: 105 (Stock price when this order came in)
			 * Buys: [C3 BUY GOOG 97 10] (Ordered list of buy offers for this stock BEFORE this request
			 * Sells: [c9 SELL GOOG 103 10] (Ordered list of sell offers for this stock BEFORE this request
			 * [ C1 10000 C2 9070 ... ]
			 * -------------------------------------------------------------------------
			 */
			System.out.println("---------");
			System.out.print("Order #" + order_num + " ");
			order_num++;
			
			Request r = new Request(client, price, num, sym, action);
			Iterator<Stock> it = stock_state.iterator();
			boolean match = false;
			
			/* we have to search for the stock in the linked list. so iterate over it */
			
			while (it.hasNext()) {
				Stock temp = it.next();
				if (temp.getSym().equals(r.getSym())) {
					
					System.out.println(r.printReq());
					temp.printStock();
					
					System.out.print("[");
					for (int j=0; j < num_cli; j++) {
						accnt_state[j].printAccnt();
					}
					System.out.print("]");
					System.out.println();
					
					/* after printing the request, push it in the appropriate queue in the
					 * corresponding stock
					 */
					
					temp.pushReq(r);
					match = true;
				}
			}
			
			if (! match) {
				System.err.println("No matching stock found for symbol " + r.getSym());
			}
			
		}
	}
	
	
	
	
	
	
	
	
	/* do_trade: This method iterates over all the stocks and checks if any trade is possible
	 * looking at their buyList and sellList. 
	 * If the trade is possible, it commits the trade, removing the particular requests from the
	 * stock's queues.
	 */
	
	public void do_trade () {
		
		Iterator<Stock> i = stock_state.iterator();
		
		while (i.hasNext()) {

			Stock next_stock = i.next();
			Request[] trade_result = new Request[2];
			
			/* call the trade method on the stock. It will check if a trade
			 * is possible. If so, it will return is the two requests which
			 * are matching and update the price of the stock as appropriate.
			 */
			trade_result = next_stock.trade();
			/* trade_result[0] is seller and trade_result[1] is buyer.
			 * we have to update their account info of the buyer and seller
			 */
			
			if (trade_result[0] == null && trade_result[1] == null) {
				// no possible tread here.
				continue;
			} else {
				System.out.println("----------------------------------------------------------------");
				System.out.println("Trade # " + trade_num);
				System.out.println("----------------------------------------------------------------");
				trade_num++;
				
				Request buyer = trade_result[1];
				Request seller = trade_result[0];
				
				int buyer_id = buyer.getCli();
				int seller_id = seller.getCli();
				
				Account buyer_accnt = accnt_state[buyer_id-1];
				Account seller_accnt = accnt_state[seller_id-1];
				
				/* When the tread is done, call the set stock method on the accounts of buyer and
				 * seller. For buyer, we send the number of stocks as N and seller as -N (for indicating
				 * reduction). This method then will update the number of shares and stock info of
				 * the clients account. Note that that the price at which tread happens is seller's quotation
				 */
				
				// first update the buyer 
				buyer_accnt.setStocks(next_stock.getSym(), buyer.getQuant(), seller.getOffer());
				// then update the seller 
				seller_accnt.setStocks(next_stock.getSym(), -seller.getQuant(), seller.getOffer());
				
				next_stock.printStock();
				System.out.print("[");
				for (int j=0; j < num_cli; j++) {
					accnt_state[j].printAccnt();
				}
				System.out.println("]");
				System.out.println("----------------------------------------------------------------");
				
			}
		}
		//System.out.println("Finished trading round");
	}
	
	
	
	
	/* this method processes the history we have received. We still have to maintain the history,
	 * so we need to pop all the history in request processing queue and still keep the list
	 * having the request history intact.
	 */
	
	private void process_history () {
		
		int h_size  = req_state.size();
		for (int i=0; i < h_size; i++) {
			req_state.add(req_state.peek());
			req_list.add(req_state.poll());
			
			/* call the process request and do trade methods instantly because otherwise
			 * we will find different match than the other nodes.
			 */
			process_req();
			do_trade();
		}
	}
	
}
