import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingQueue;

/* This class implements a thread which listens to a server socket provided
 * to it for connections from clients. When it receives a connection, it spawns
 * a minion thread to handle the communication and continues to listen for more
 * clients.
 */
public class SocketServerThread implements Runnable {
	
	ServerSocket servSock; 
	LinkedBlockingQueue<String> sock2main;		/* this queue is to be passed to minion threads.
												 * It is a synchronized queue in which they will put the
												 * received messages for the main broker thread
												 */ 
	
	public SocketServerThread (ServerSocket s, LinkedBlockingQueue<String> a) {
		this.servSock = s;
		this.sock2main = a;
	}

	public void run() {
		
		System.out.println("Server now listening on port " + servSock.getLocalPort());
		
		while (true) {
			
			try {
				Socket cliSock = servSock.accept();
				Thread sockThread = new Thread(new SocketThread(cliSock, sock2main));
				sockThread.start();
			} catch (IOException e) {
				System.err.println("Error on the server socket");
				e.printStackTrace();
			}
			
		}
		
	}
	
	
	
}
