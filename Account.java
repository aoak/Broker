import java.util.Hashtable;

/* The class for the account information for the clients. It has
 * client id, cash, total balance and a hashmap having all the
 * stocks owned by this client.
 */
public class Account {

	int id;
	int balance;
	int cash;
	Hashtable<String,Stock> stocks_owned;
	int stock_count;
	
	
	public Account (int i, int c) {
		this.id = i;
		this.cash = c;
		this.balance = c;
		this.stocks_owned = new Hashtable<String,Stock>();
		this.stock_count = 0;
	}
	
	
	public int getCash () {
		return cash;
	}
	
	public void setCash (int c) {
		cash = c;
	}
	
	
	
	
	/* This method sets the number of shares and the price for the 
	 * particular stock the client has.
	 */
	
	public void setStocks (String stock, int num, int price) {
		Stock curr_stock = stocks_owned.get(stock);
		/* If the client does not have this stock before, then add it
		 * otherwise, update the number of shares and price in the existing
		 * stock.
		 */
		if (curr_stock == null) {
			stocks_owned.put(stock, new Stock("unset",stock,num,price));
		} else {
			curr_stock.setPrice(price);
			curr_stock.setNum(curr_stock.getNum() + num);
		}
		setCash(getCash() - num * price);
		computeBalance(num,price);
	}
	
	
	public void computeBalance (int num, int price) {
		balance += num * price;
	}
	
	
	
	public void printAccnt () {
		System.out.print("C" + id + " " + cash + " ");
	}
	
}
