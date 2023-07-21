import java.io.*;
import java.net.*;
import java.util.HexFormat;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;
import java.util.Arrays;

class NetworkService implements Runnable {

    private final ExecutorService pool;
    private final String[] roots;    
    private final DatagramSocket socket;
    private final int timeout;

    public NetworkService(int port, int poolSize, String[] roots, int timeout) throws IOException {
        pool = Executors.newFixedThreadPool(poolSize);
        this.socket = new DatagramSocket(port);
        this.roots = roots;
        this.timeout = timeout;
    }

    public void run() {
        for (;;){
            byte[] queryData =new byte[1024];
                
            DatagramPacket queryPacket = new DatagramPacket(queryData,queryData.length);

            try {
                socket.receive(queryPacket);
            } catch (IOException e) {
                e.printStackTrace();
            }
            
            try {
                pool.execute(new Handler(roots, queryPacket, timeout));
            } catch (SocketException e) {
                e.printStackTrace();
            }
        }
    }

}

class Handler implements Runnable {

    private final DatagramSocket socket;
    private final String[] roots;    
    private final DatagramPacket queryPacket;    

    Handler( String[] roots, DatagramPacket queryPacket, int timeout) throws SocketException {
        Random rand = new Random();

        int id = rand.nextInt(64511);
        id += 1024;
        DatagramSocket outputSocket;
        try{
            outputSocket = new DatagramSocket(id);
        }
        catch (Exception e) {
            id = rand.nextInt(64511);
            id += 1024;
            outputSocket = new DatagramSocket(id);
        }
        this.socket = outputSocket;

        this.socket.setSoTimeout(timeout);
        this.roots = roots;
        this.queryPacket = queryPacket;
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
            try{
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
            catch (SocketTimeoutException ex){
                continue;
            }
        }
        return data;
    }

    private void rootSearch(byte[] requestData) {
        try {
            for (int i = 0; i < roots.length; i++){

                try{
                    Response response = makeRequest(roots[i], 53, requestData);

                    if (response.errorCode() == 1 || response.errorCode() == 2){
                        DatagramPacket sendPacket=new DatagramPacket(response.getRawResponse(), response.getRawResponse().length, queryPacket.getAddress(), queryPacket.getPort());
                
                        socket.send(sendPacket);
                        break;                    
                    }

                    if (response.getAnswersCount() > 0){
                        if (response.getAnswerType() == 5){
                            Request CNAMErequest = new Request(response.getAnswersAddress()[1]);
                            rootSearch(CNAMErequest.getRequest());
                        }

                        DatagramPacket sendPacket=new DatagramPacket(response.getRawResponse(), response.getRawResponse().length, queryPacket.getAddress(), queryPacket.getPort());
                
                        socket.send(sendPacket);
                        break;
                    }
                    else if (response.getNameServerCount() > 0){
                        response = recursion(response, requestData, 0);
                        if (response != null){
                            if (response.getAnswersCount() > 0 || (response.errorCode() == 3 && response.isAuthority())){
                                
                                if (response.getAnswerType() == 5){
                                    Request CNAMErequest = new Request(response.getAnswersAddress()[0]);
                                    rootSearch(CNAMErequest.getRequest());
                                }

                                DatagramPacket sendPacket=new DatagramPacket(response.getRawResponse(), response.getRawResponse().length,queryPacket.getAddress(), queryPacket.getPort());
                    
                                socket.send(sendPacket);
                                break;
                            }
                        }
                    }

                    if (i == roots.length-1){
                        DatagramPacket sendPacket=new DatagramPacket(response.getRawResponse(), response.getRawResponse().length,queryPacket.getAddress(), queryPacket.getPort());
                
                        socket.send(sendPacket);
                        break;
                    }
                }
                catch (SocketTimeoutException e){
                    continue;
                }
            }
            return;
        } catch (IOException ex) {
            
        }
    }

    @Override
    public void run() {
        rootSearch(queryPacket.getData());
    }
    
}


public class Resolver {

    public static void main(String[] args)throws Exception {
        
        if (args.length < 1){
            System.out.println("Error: Too few arguments");
            System.out.println("Resolver port [timeout = 5]");
            return;
        }


        int timeout = 5000;

        int port = 5300;

        try {
            port = Integer.parseInt(args[0]); 

    
            if (args.length > 3){
                timeout = Integer.parseInt(args[2]) * 1000;
            }
        }
        catch (Exception e) {
            System.out.println("Error: Invalid Arguments");
            System.out.println("Resolver port [timeout = 5]");
            return;
        }



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

        NetworkService service = new NetworkService(port, 6, roots, timeout);
        System.out.println("Listening on port " + port);
        service.run();
    }

}
