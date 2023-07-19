import java.io.*;
import java.net.*;
import java.util.HexFormat;
import java.util.Random;

public class Client {


    public static void main(String[] args)throws Exception {

        if (args.length < 3){
            System.out.println("Too few arguments");
            return;
        }

        String[] domains = args[2].split("\\.", 0);

        InetAddress IPAddress = InetAddress.getByName(args[0]);
        int serverPort = Integer.parseInt(args[1]); 
        
        DatagramSocket clientSocket = new DatagramSocket();
        
        Request request = new Request(domains);

        DatagramPacket sendPacket=new DatagramPacket(request.getRequest(),request.getCursor(),IPAddress,serverPort);
        
        clientSocket.send(sendPacket);
        
        byte[] receiveData=new byte[508];
        
        DatagramPacket receivePacket = new DatagramPacket(receiveData,receiveData.length);

        clientSocket.receive(receivePacket);

        byte[] responseData = receivePacket.getData();

        Response response = new Response(responseData);
        
        for (int i = 0; i < response.getAnswersCount(); i++){
            System.out.print("Is Authority: " );
            if (response.isAuthority()){
                System.out.println("True");
            }
            else {
                System.out.println("False");
            }

            System.out.print("Is Truncated: " );
            if (response.isTruncated()){
                System.out.println("True");
            }
            else {
                System.out.println("False");
            }
            System.out.println(response.getAnswersNames()[i]);
            System.out.println(response.getAnswersAddress()[i]);
        }

        System.out.println("NO ANSWERS");
        System.out.println(response.getAnswersCount());



        clientSocket.close();
    }
}
