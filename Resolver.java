import java.io.*;
import java.net.*;
import java.util.HexFormat;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;
import java.util.Arrays;

class NetworkService implements Runnable {

    private final ExecutorService pool;
    private final String[] roots;    
    private final int port;

    public NetworkService(int port, int poolSize, String[] roots) throws IOException {
        pool = Executors.newFixedThreadPool(poolSize);
        this.roots = roots;
        this.port = port;
    }

    public void run() {
        for (;;){
            try (DatagramSocket socket = new DatagramSocket(port)) {
                byte[] queryData =new byte[1024];
                    
                DatagramPacket queryPacket = new DatagramPacket(queryData,queryData.length);

                try {
                    socket.receive(queryPacket);
                    socket.close();
                    System.out.println("RECEIEVED");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                
                try {
                    pool.execute(new Handler(roots, queryPacket));
                } catch (SocketException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            } catch (SocketException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

}

class Handler implements Runnable {

    private final DatagramSocket socket;
    private final String[] roots;    
    private final DatagramPacket queryPacket;    

    Handler(String[] roots, DatagramPacket queryPacket) throws SocketException {
        this.socket =  new DatagramSocket(53);
        this.roots = roots;
        this.queryPacket = queryPacket;
        System.out.println(queryPacket.getData());
    }

    static int trim(byte[] bytes)
    {
        int i = bytes.length - 1;
        while (i >= 0 && bytes[i] == 0)
        {
            i-=1;
        }
        return i + 1;
    }


    public Response makeRequest(String address, int serverPort, byte[] data) throws IOException{
        InetAddress IPAddress = InetAddress.getByName(address);
        
        DatagramPacket sendPacket=new DatagramPacket(data,trim(data),IPAddress,serverPort);

        this.socket.send(sendPacket);

        byte[] receiveData=new byte[1024];

        DatagramPacket receivePacket = new DatagramPacket(receiveData,receiveData.length);
        
        this.socket.setSoTimeout(10000);
        try{
            this.socket.receive(receivePacket);
        }
        catch(SocketTimeoutException e){
            throw e;
        }

        Response response = new Response(receiveData);

        return response;
    }

    public Response recursion(Response data, byte[] queryData, int recursionCount) throws IOException {
        if (recursionCount > 3){
            return null;
        }
        for (String nameServer : data.getNameServersAddress()){
            Response response = this.makeRequest(nameServer, 53, queryData);
            if (response.getAnswersCount() > 0) {
                return response;
            }
            else if (response.isError()){
                if (response.errorCode() == 3 && !response.isAuthority()){
                    return recursion(response, queryData, recursionCount+1);
                }
                else{
                    return response;
                }
            }
            else {
                return recursion(response, queryData, recursionCount+1);
            }
        }
        return data;
    }
    @Override
    public void run() {

        try {
            for (int i = 0; i < roots.length; i++){

                try{
                    Response response = makeRequest(roots[i], 53, queryPacket.getData());

                    if (response.getAnswersCount() > 0){
                        DatagramPacket sendPacket=new DatagramPacket(response.getRawResponse(), response.getRawResponse().length, queryPacket.getAddress(), 3000);
                
                        socket.send(sendPacket);
                        socket.close();
                        return;
                    }
                    else if (response.getNameServerCount() > 0){
                        response = recursion(response, queryPacket.getData(), 0);
                        if (response != null){
                            if (response.getAnswersCount() > 0 || (response.errorCode() == 3 && response.isAuthority())){
                                
                                DatagramPacket sendPacket=new DatagramPacket(response.getRawResponse(), response.getRawResponse().length,queryPacket.getAddress(), 3000);
                    
                                socket.send(sendPacket);
                                socket.close();
                                return;
                            }
                        }
                    }

                    if (i == roots.length-1){
                        DatagramPacket sendPacket=new DatagramPacket(response.getRawResponse(), response.getRawResponse().length,queryPacket.getAddress(), 3000);
                
                        socket.send(sendPacket);
                        socket.close();
                        return;
                    }
                }
                catch(SocketTimeoutException e){
                    continue;
                }
            }
            socket.close();
            return;
        } catch (IOException ex) {
            
        }
    }
    
}


public class Resolver {

    public static void main(String[] args)throws Exception {
        String[] roots = new String[14];

        BufferedReader reader;

		try {
			reader = new BufferedReader(new FileReader("named.root"));
			String line = reader.readLine();
            int rootCursor = 0;
			while (line != null) {
                if (line.charAt(0) != ';' && line.charAt(0) != '.' && line.charAt(39) != 'A'){
                    roots[rootCursor] = line.substring(44);
                    rootCursor += 1;
                }
				line = reader.readLine();
			}

			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

        NetworkService service = new NetworkService(5300, 10, roots);
        service.run();
    }

}
