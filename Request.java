/* The class for a request. It has the id of the client who has made a request,
 * type of request (BUY/SELL), offer that the client is making, number of shares
 * he wants to tread and the symbol.
 */
public class Request {

	int client;
	String sym;
	int offer;
	int quant;
	String type;
	
	public Request (int c, int o, int q, String sym, String type) {
		this.client = c;
		this.offer = o;
		this.quant = q;
		this.sym = sym;
		this.type = type;
	}
	
	public int getOffer () {
		return offer;
	}
	
	public String getSym () {
		return sym;
	}
	
	public String getType () {
		return type;
	}
	
	
	public int getCli () {
		return client;
	}
	
	
	public int getQuant () {
		return quant;
	}
	
	public String printReq () {
		return "C" + client + " " + type + " " + sym + " " + offer + " " + quant;
	}
}
