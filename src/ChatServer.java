
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.HashSet;

/**
 * A multithreaded chat room server.  When a client connects the
 * server requests a screen name by sending the client the
 * text "SUBMITNAME", and keeps requesting a name until
 * a unique one is received.  After a client submits a unique
 * name, the server acknowledges with "NAMEACCEPTED".  Then
 * all messages from that client will be broadcast to all other
 * clients that have submitted a unique screen name.  The
 * broadcast messages are prefixed with "MESSAGE ".
 *
 * Because this is just a teaching example to illustrate a simple
 * chat server, there are a few features that have been left out.
 * Two are very useful and belong in production code:
 *
 *     1. The protocol should be enhanced so that the client can
 *        send clean disconnect messages to the server.
 *
 *     2. The server should do some logging.
 */
public class ChatServer {

    /**
     * The port that the server listens on.
     */
    private static final int PORT = 9001;

    /**
     * The set of all names of clients in the chat room.  Maintained
     * so that we can check that new clients are not registering name
     * already in use.
     */
    private static HashSet<String> names = new HashSet<String>();
    

    /**
     * The set of all the print writers for all the clients.  This
     * set is kept so we can easily broadcast messages.
     */
    private static HashSet<PrintWriter> writers = new HashSet<PrintWriter>();
    
    /**
     * The set of all the print writers and their names as a key value HashMap. Used to send 
     * messages to specific users.
     */
    private static HashMap<String, PrintWriter> writerNames = new HashMap<String, PrintWriter>();
    
    /**
     *Set of temporary print writer objects.  
     */
    private static HashSet<PrintWriter> tempWriters = new HashSet<PrintWriter>();
    
    
    
    /**
     * The appplication main method, which just listens on a port and
     * spawns handler threads.
     */
    public static void main(String[] args) throws Exception {
        System.out.println("The chat server is running.");
        ServerSocket listener = new ServerSocket(PORT);
        try {
            while (true) {
            	Socket socket  = listener.accept();
                Thread handlerThread = new Thread(new Handler(socket));
                handlerThread.start();
            }
        } finally {
            listener.close();
        }
    }

    /**
     * A handler thread class.  Handlers are spawned from the listening
     * loop and are responsible for a dealing with a single client
     * and broadcasting its messages.
     */
    private static class Handler implements Runnable {
        private String name;
        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;

        /**
         * Constructs a handler thread, squirreling away the socket.
         * All the interesting work is done in the run method.
         */
        public Handler(Socket socket) {
            this.socket = socket;
        }
        
        /**
         * Method to check whether a given name is in the names. So that only one thread can access names at a time
         */
        
        private synchronized boolean isNameTaken(String name) {
        	  if (names.contains(name)) {
        		  return true;
              }
        	  else {
        		  return false;
        	  }
        }
        
        /**
         * Method to add a given name to names. So that only one thread can access names at a time
         */
        
        private synchronized void addName(String name) {
        	names.add(name);
        }
        
        /**
         * Method to remove a name from names;
         */
        private synchronized void removeName(String name) {
        	names.remove(name);
        }
        
        /**
         * Method to get all active clients in a single string
         */
        private synchronized String getAllNames(String string) {
        	for(String name: names) {
            	
        		string += name + ",";
            }
        	
        	return string;
        }
       
        /**
         * Services this thread's client by repeatedly requesting a
         * screen name until a unique one has been submitted, then
         * acknowledges the name and registers the output stream for
         * the client in a global set, then repeatedly gets inputs and
         * broadcasts them.
         */
        public void run() {
            try {

                // Create character streams for the socket.
                in = new BufferedReader(new InputStreamReader(
                    socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                // Request a name from this client.  Keep requesting until
                // a name is submitted that is not already used.  Note that
                // checking for the existence of a name and adding the name
                // must be done while locking the set of names.
                while (true) {
                    out.println("SUBMITNAME");
                    name = in.readLine();
                    if (name == null) {
                        return;
                    }
                    
                    // TODO: Add code to ensure the thread safety of the
                    // the shared variable 'names'
                    // Here to ensure thread safety isNameTaken and addName methods are synchronized.
                    
                    if(!isNameTaken(name)) {
                    	addName(name);
                    	break;
                    }
                    
                 }

                // Now that a successful name has been chosen, add the
                // socket's print writer to the set of all writers so
                // this client can receive broadcast messages.
                out.println("NAMEACCEPTED");
                writers.add(out);
                
                //Add the specific print writer and its owners name into writerNames hashmap
                writerNames.put(name, out);
                
                //Broadcast the name of the new client to all the others, and add the new name to the list box
                String newList = "LIST,";
                
                //Get all active clients in a single string
                newList = getAllNames(newList);
                
                
                for (PrintWriter writer : writers) {
                    writer.println(newList);
                }

                
                // Accept messages from this client and broadcast them.
                // Ignore other clients that cannot be broadcasted to.
                while (true) {
                    String input = in.readLine();
                    if (input == null) {
                        return;
                    }
                    
                    /**
                     * If any specific clients are selected using the list box. Direct 
                     * the message to intended users. Else follow the normal procedure
                     */
                    if(input.startsWith("SELECTED")) {
                    	
                    	System.out.println("Message started with selected");
                    	String[] usernames = input.split(",");
                    	String message = usernames[usernames.length -1];
                    	usernames[usernames.length -1] = null;
                    	usernames[0] = null;
                    	
                    	System.out.println("Message is: " + message);
                    	
                    	System.out.println("Usernames are");
                    	
                    	for(String name: usernames) {
                    		System.out.println(name);
                    	}
                    	
                    	//get the print writer object for each of the names
                    	//Empty the tempWriters hashmap
                    	tempWriters.clear();
                    	
                    	for(String name:usernames) {
                    		
                    		if(isNameTaken(name)) {
                    			tempWriters.add(writerNames.get(name));
                    		}
                    	}
                    	
                    	//Direct the message to all the selected clients
                    	for (PrintWriter writer : tempWriters) {
                            writer.println("MESSAGE " + name + ": " + message);
                        }
                    	
                    	
                    }
                    else {
                    	
                    	/**
                    	 * If the list box is not used and a  message is directed at a specific client using the 'name>>'
                    	 * notation direct it to intended user. Else broadcast the message
                    	 */
                    	if(input.contains(">>")) {
                        	
                        	String[] inputArray = input.split(">>");
                        	String userName = inputArray[0];
                        	
                        	if(isNameTaken(userName)) {
                        		
                        		//Get the writer object related to the user
                        		PrintWriter newWriter = writerNames.get(userName);
                        		
                        		//Direct the message
                        		newWriter.println("MESSAGE " + name + ": " + input);
                        	}
                        	else {
                        		for (PrintWriter writer : writers) {
                                    writer.println("MESSAGE " + name + ": " + input);
                                }
                        	}
                        	
                        }
                        else {
                        	for (PrintWriter writer : writers) {
                                writer.println("MESSAGE " + name + ": " + input);
                            }
                        }
                    }
                    
                    
                    
                    
                    
                }
            }//Handle the socket exception is if the client close the application
            catch(SocketException e) {
            	 
            	if(name != null) {
            		removeName(name);
            	}
            	
            	if(out != null) {
            		writers.remove(out);
            	}
            	
            	String newList = "LIST,";
                
                newList = getAllNames(newList);
                
                
                for (PrintWriter writer : writers) {
                    writer.println(newList);
                }
            
            }
            catch (IOException e) {
                System.out.println(e);
            }    
            finally {
                // This client is going down!  Remove its name and its print
                // writer from the sets, and close its socket.
                if (name != null) {
                    removeName(name);
                }
                if (out != null) {
                    writers.remove(out);
                }
                
                String newList = "LIST,";
                
                newList = getAllNames(newList);
                
                
                for (PrintWriter writer : writers) {
                    writer.println(newList);
                }
                
                try {
                    socket.close();
                } catch (IOException e) {
                }
            }
        }
    }
}