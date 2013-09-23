import java.io.*;
import java.net.*;
import java.util.concurrent.LinkedBlockingQueue;



/* This is the thread to handle the communication on a particular socket. It handles only
 * incoming messages and has no outgoing capacity. When it receives a message, it puts that
 * in the synchronized queue to the broker thread.
 */
public class SocketThread implements Runnable {
	
	
	private Socket socket;
	LinkedBlockingQueue<String> s2m;

	
	
	
	
	public SocketThread (Socket s, LinkedBlockingQueue<String> q) {
		this.socket = s;
		s2m = q;
		if (s2m == null) {
			System.err.println("Received null queue");
		}
	}
	

	
	public String getMessage () {
		return s2m.poll();
	}
	
	
	

	
	/* The run method of this thread has to poll the queue m2s. If
	 * it finds a message there, it has to send it on the socket. It also
	 * has to receive the message on the socket and put that message into
	 * the queue s2m.
	 */
	public void run() {
		
		System.out.println("Starting socket thread handling socket " + socket);
		//BufferedReader in = null;
		ObjectInputStream in = null;
		
		
		/* First create streams on the socket so that we are good to go */
		try {
			//in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			in = new ObjectInputStream(socket.getInputStream());
						
		} catch (IOException e) {
			System.err.println("IO Error while creating streams on socket");
			e.printStackTrace();
			return;
		}
		
		while (true) {
			
			String mi = null;
		
			/* listen on the port and accept the requests then put it in the queue to main thread.
			 */
			try {
				mi = (String) in.readObject();
				if (mi != null) {
					s2m.add(mi);
					System.out.println("Received Message " + mi + " on socket " + socket + "total elements in queue " + s2m.size());
					socket.close();
					return;
				}
			} catch (ClassNotFoundException e) {
				System.err.println("Received class not found");
				e.printStackTrace();
			} catch (IOException ioe) {
				System.err.println("Error while reading Message from socket");
				ioe.printStackTrace();
			}
			
			
			
			if (Thread.interrupted()) {
				System.out.println("Got exit signal.");
			}
		}
	}


}
