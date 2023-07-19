import java.io.*;
import java.net.*;
import java.util.HexFormat;
import java.util.concurrent.TimeoutException;
import java.util.Arrays;


public class Resolver {

    private static DatagramSocket socket;

    static int trim(byte[] bytes)
    {
        int i = bytes.length - 1;
        while (i >= 0 && bytes[i] == 0)
        {
            i-=1;
        }
        return i + 1;
    }


    public static Response makeRequest(String address, int serverPort, byte[] data) throws IOException{
        InetAddress IPAddress = InetAddress.getByName(address);
        
        DatagramPacket sendPacket=new DatagramPacket(data,trim(data),IPAddress,serverPort);

        socket.send(sendPacket);

        byte[] receiveData=new byte[1024];

        DatagramPacket receivePacket = new DatagramPacket(receiveData,receiveData.length);
        
        socket.setSoTimeout(10000);
        try{
            socket.receive(receivePacket);
        }
        catch(SocketTimeoutException e){
            throw e;
        }

        Response response = new Response(receiveData);

        return response;
    }

    public static Response recursion(Response data, byte[] queryData, int recursionCount) throws IOException {
        if (recursionCount > 3){
            return null;
        }
        for (String nameServer : data.getNameServersAddress()){
            Response response = makeRequest(nameServer, 53, queryData);
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

        while(true){

            socket = new DatagramSocket(53);

            byte[] queryData =new byte[1024];
            
            DatagramPacket queryPacket = new DatagramPacket(queryData,queryData.length);

            socket.receive(queryPacket);
            
            while(true){
                for (int i = 0; i < roots.length; i++){

                    try{
                        Response response = makeRequest(roots[i], 53, queryData);

                        if (response.getAnswersCount() > 0){
                            DatagramPacket sendPacket=new DatagramPacket(response.getRawResponse(), response.getRawResponse().length,queryPacket.getAddress(),queryPacket.getPort());
                    
                            socket.send(sendPacket);
                            break;
                        }
                        else if (response.getNameServerCount() > 0){
                            response = recursion(response, queryData, 0);
                            if (response != null){
                                if (response.getAnswersCount() > 0 || (response.errorCode() == 3 && response.isAuthority())){
                                    
                                    DatagramPacket sendPacket=new DatagramPacket(response.getRawResponse(), response.getRawResponse().length,queryPacket.getAddress(),queryPacket.getPort());
                        
                                    socket.send(sendPacket);
                                    break;
                                }
                            }
                        }

                        if (i == roots.length-1){
                            DatagramPacket sendPacket=new DatagramPacket(response.getRawResponse(), response.getRawResponse().length,queryPacket.getAddress(),queryPacket.getPort());
                    
                            socket.send(sendPacket);

                        }
                    }
                    catch(SocketTimeoutException e){
                        continue;
                    }
                }
                socket.close();
                break;
            }


        }
        
    }
}