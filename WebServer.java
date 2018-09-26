import java.net.*;
import java.nio.ByteBuffer;
import java.nio.file.*;
import java.io.*;
import java.util.*;

public class WebServer {

	public static void main(String[] args) throws IOException {
        // dummy value that is overwritten below
		int port = 8080;
		try {
			port = Integer.parseInt(args[0]);
		} catch (Exception e) {
			System.out.println("Usage: java WebServer <port> ");
			System.exit(0);
		}

		WebServer serverInstance = new WebServer();
		serverInstance.start(port);
	}

	private void start(int port) throws IOException {

		System.out.println("Starting server on port " + port);
		ServerSocket welcomeSocket = new ServerSocket(port);

		while (true) {
			Socket connSocket = welcomeSocket.accept();
			System.out.println("Client Connected!\n");

			handleClientSocket(connSocket);

			connSocket.close();
			System.out.println("Connection closed.");
		}  
	}

    /**
     * Handles requests sent by a client
     * @param  client Socket that handles the client connection
     */
    private void handleClientSocket(Socket client) throws IOException {
		
		boolean valid = false;
    	
    	HttpRequest request = new HttpRequest();

    	BufferedReader br = new BufferedReader(new InputStreamReader(client.getInputStream()));
    	StringBuilder line = new StringBuilder();
    	String content;

    	String header = content = br.readLine();

    	if (header != null && header != "") {
    	   valid = request.parseRequest(header);
    	}

		byte response[];

		if (valid) {
		  	response = formHttpResponse(request);

		} else { //400 ERROR
			byte[] response400 = get400response(request.getFilePath());
			StringBuilder str = new StringBuilder();
			str.append("HTTP/");
		    str.append(request.getVersion()+" ");
		    str.append("400 Bad Request\r\n\r\n");
		    response = concatenate(str.toString().getBytes(), response400);
		}

		sendHttpResponse(client, response);

    	if (request.getVersion().equals("1.1")) {
    		try {
    			client.setSoTimeout(2000); //PERSISTANCY
    			handleClientSocket(client);
    		} catch (Exception e) {
    			br.close();
    			client.close();
    		}
    		finally{
    			client.close();
    			br.close();
    		}
    	} else {
    		client.close();
    		br.close();
    	}
    }

    /**
     * Sends a response back to the client
     * @param  client Socket that handles the client connection
     * @param  response the response that should be send to the client
     */
    private void sendHttpResponse(Socket client, byte[] response) throws IOException {
  
    	DataOutputStream bos = new DataOutputStream(client.getOutputStream());
    	bos.write(response);
    	bos.flush();
    }

    /**
     * Form a response to an HttpRequest
     * @param  request the HTTP request
     * @return a byte[] that contains the data that should be send to the client
     */
    private byte[] formHttpResponse(HttpRequest request) {

    	byte response[];
    	
    	try {
    		String path = request.getFilePath();
    		byte content[];

    		content = Files.readAllBytes(Paths.get(path));
    		StringBuilder str = new StringBuilder();
    		str.append("HTTP/");
    		str.append(request.getVersion() + " ");
    		str.append("200 OK\r\n\r\n");
    		byte tempbuf[] = str.toString().getBytes();

    		return concatenate(tempbuf, content);

	    } catch (Exception e) { //404 ERROR
	    	byte[] response404;
	    	response404 = get404response(request.getFilePath());
	    	StringBuilder str = new StringBuilder();
	    	str.append("HTTP/");
	    	str.append(request.getVersion()+ " ");
	    	str.append("404 Not Found\r\n\r\n");

	    	return concatenate(str.toString().getBytes(), response404);
	    }	    
	}


    /**
     * Concatenates 2 byte[] into a single byte[]
     * This is a function provided for your convenience.
     * @param  buffer1 a byte array
     * @param  buffer2 another byte array
     * @return concatenation of the 2 buffers
     */
    private byte[] concatenate(byte[] buffer1, byte[] buffer2) {
    	byte[] returnBuffer = new byte[buffer1.length + buffer2.length];
    	System.arraycopy(buffer1, 0, returnBuffer, 0, buffer1.length);
    	System.arraycopy(buffer2, 0, returnBuffer, buffer1.length, buffer2.length);
    	return returnBuffer;
    }


	public byte[] get404response(String path) { //HTML Page
		StringBuilder result = new StringBuilder();
		result.append("<html>");
		result.append("<h1>");
		result.append("ERROR 404: PAGE NOT FOUND");
		result.append("</h1>");
		result.append("<p>");
		result.append(path + " page could not be found. Please try again.");
		result.append("</p>");
		result.append("</html>");
		result.append("\r\n");
		return result.toString().getBytes();
	}

	public byte[] get400response(String path) { //HTML page
		StringBuilder result = new StringBuilder();
		result.append("<html>");
		result.append("<h1>");
		result.append("ERROR 400: BAD REQUEST");
		result.append("</h1>");
		result.append("<p>");
		result.append(path + " page could not be found. Please try again.");
		result.append("</p>");
		result.append("</html>");
		result.append("\r\n");
		return result.toString().getBytes();
	}
}



class HttpRequest {
	private String filePath, version;
	private Hashtable<String, String> headers;

	public HttpRequest() {
		filePath = null;
		version = "1.0";
		headers = null;
	}

	String getFilePath() {
		return filePath;
	}

	String getVersion() {
		return version;
	}

	public boolean parseRequest(String request) {

		String line[] = request.split(" ");

		if (!line[0].equals("GET") || line.length!=3 ) {
			return false;
		}

    	// EG: GET /demo.html HTTP/1.0
		filePath = line[1].substring(1);
		version = line[line.length - 1].substring(5,8); 

		String protocol = line[line.length - 1].substring(0,5);

		if (protocol == null || !protocol.equals("HTTP/") || 
			!(version.equals("1.1") || version.equals("1.0"))){
			return false;
		}
	return true;
	}
}
