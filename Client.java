import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Random;


public class Client {

	/**
	 * Client <Number of clients><Number of requests><Server port number><stock>
	 */
	public static void main(String[] args) throws UnknownHostException, IOException,InterruptedException{
		
		int ServerPortNum = Integer.parseInt(args[2]);
		int RequestNum = Integer.parseInt(args[1]);
		int ClientNum = Integer.parseInt(args[0]);
		String stock = args[3];
		
		Random rand = new Random();
		
		for(int i=0;i<RequestNum;i++){
			String order;
			Socket sock = new Socket("127.0.0.1", ServerPortNum);
			ObjectOutputStream oos = new ObjectOutputStream(sock.getOutputStream());
			Thread.sleep(500);
		    int price = rand.nextInt(20)-10+100;
		    int op = rand.nextInt(2);
		    int clientID = rand.nextInt(ClientNum)+1;
		    if (op==0){
		    	order = "C"+clientID+" BUY "+stock+" "+price+" 10";
		    }
		    else{
		    	order = "C"+clientID+" SELL "+stock+" "+price+" 10";
		    }
			System.out.println("Sending message : " + order);
			oos.writeObject(order);
			oos.flush();	
			oos.close();
			sock.close();
		}
	}

}
