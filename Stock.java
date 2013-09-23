import java.io.Serializable;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.concurrent.LinkedBlockingQueue;


/* The class for the information about a stock. It has name of the
 * company, symbol, number of shares, the price and two sorted
 * queues having sell and buy lists.
 */
public class Stock implements Serializable {


	private static final long serialVersionUID = 1L;
	String name;
	String symbol;
	int shares;
	int price;
	ReqComparator comp1;
	ReqComparator comp2;
	PriorityQueue<Request> sellList;
	PriorityQueue<Request> buyList;
	boolean listed;
	
	/* The constructor setting the initial price to 100 and number of
	 * shares equal to 10000.
	 */
	public Stock (String name, String sym, int shares, int price) {
		this.name = name;
		this.symbol = sym;
		this.shares = shares;
		this.price = price;
		this.comp1 = new ReqComparator('a');
		this.comp2 = new ReqComparator('a');
		sellList = new PriorityQueue<Request>(10,comp1);
		buyList = new PriorityQueue<Request>(10,comp2);
		listed = true;
	}
	
	
	public String getSym () {
		return symbol;
	}
	
	
	
	
	/* Put the incoming request object in a proper queue */
	
	public void pushReq (Request r) {
		
		String type = r.getType();
		
		if (type.equals("BUY")) {
			buyList.add(r);
		} else if (type.equals("SELL")) {
			sellList.add(r);
		} else {
			System.err.println("Received unknown request for action " + type);
		}
	}
	
	
	
	
	
	
	public void setPrice (int new_price) {
		this.price = new_price;
	}
	
	
	
	
	
	
	public int getPrice () {
		return this.price;
	}
	
	
	private int getLowestSell () {
		if (sellList.peek() == null)
			return -1;
		else
			return sellList.peek().getOffer();
	}
	
	
	
	
	
	/* This method goes through the buy list and sell list. If it has a
	 * matching limit order which makes the tread possible, then it returns
	 * those two requests. otherwise returns nulls in the array.
	 */
	
	public Request[] trade () {
		
		Request[] req_array = new Request[2];
		req_array[0] = null;
		req_array[1] = null;
		
		int low_sellprice = getLowestSell();
		if (low_sellprice < 0 ) {
			return req_array;
		}
		Iterator<Request> i = buyList.iterator();
		
		
		while (i.hasNext()) {
			Request next_req = i.next();
			if (next_req.getOffer() >= low_sellprice) {
				req_array[0] = sellList.poll();
				req_array[1] = next_req;
				i.remove();
				setPrice(low_sellprice);
				break;
			}
		}
		
		return req_array;
	}
	
	
	public void setNum (int n) {
		shares = n;
	}
	
	public int getNum () {
		return shares;
	}
	
	
	/* following two methods are for printing the stock information and the sorted queues
	 * in the required manner.
	 */
	public void printStock () {
		System.out.println("Price: " + price);
		System.out.print("Buys: ");
		printPriorityQueue(buyList);
		System.out.println();
		System.out.print("Sells: ");
		printPriorityQueue(sellList);
		System.out.println();
	}
	
	
	private void printPriorityQueue (PriorityQueue<Request> q) {
		LinkedBlockingQueue<Request> tempQ = new LinkedBlockingQueue<Request>();
		int size = q.size();
		
		for (int i=0; i < size; i++) {
			Request foo = q.poll();
			System.out.print("[" + foo.printReq() + "] ");
			tempQ.add(foo);
		}
		
		for (int i=0; i < size; i++) {
			q.add(tempQ.poll());
		}
	}
}
