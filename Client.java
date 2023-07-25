import java.io.*;
import java.net.*;
import java.util.HexFormat;
import java.util.Random;

public class Client {


    public static void main(String[] args)throws Exception {

        if (args.length < 3){
            System.out.println("Error: Too few arguments");
            System.out.println("Client resolved_ip resolver_port query_name [timeout=5]");
            return;
        }

        InetAddress IPAddress = InetAddress.getByName(args[0]);
        int serverPort = Integer.parseInt(args[1]); 
        String name = args[2];

        int timeout = 5000;

        if (args.length > 3){
            timeout = Integer.parseInt(args[3]) * 1000;
        }

        Random rand = new Random();

        int id = rand.nextInt(64511);
        id += 1024;

        DatagramSocket clientSocket;

        try{
            clientSocket = new DatagramSocket(id);
        }
        catch (Exception e) {
            id = rand.nextInt(64511);
            id += 1024;
            clientSocket = new DatagramSocket(id);
        }

        clientSocket.setSoTimeout(timeout);

        Request request = new Request(name);

        DatagramPacket sendPacket=new DatagramPacket(request.getRequest(),request.getCursor(),IPAddress,serverPort);
        

        long startTime = System.nanoTime();

        clientSocket.send(sendPacket);

        byte[] receiveData=new byte[508];
        
        DatagramPacket receivePacket = new DatagramPacket(receiveData,receiveData.length);

        long duration = 0;

        try {
            clientSocket.receive(receivePacket);
            long endTime = System.nanoTime();

            duration = (endTime - startTime);
        }catch (SocketTimeoutException ex){
            System.out.println("\n<-> " + args[2] + " <->");

            System.out.println("SERVER TIMEOUT");
            clientSocket.close();
            return;
        }


        Response response = new Response(receivePacket);
        /*
        System.out.println("\n <-> " + args[2] + " <->");
        
        System.out.println(";; ->>HEADER<<-  status: " + response.errorName() + ", id: " + response.getId());

        System.out.print(";; flags: ");
        if (response.isAuthority()){
            System.out.print("aa ");
        }
        
        if (response.isTruncated()){
            System.out.print("tc ");
        }

        System.out.println("\n" + ";; ANSWER: " + response.getAnswersCount() + "\n");

        System.out.println(";; ANSWER SECTION: ");
        for (int i = 0; i < response.getAnswersCount(); i++){
            System.out.println(response.getAnswersNames()[i] + "    " + response.getAnswersAddress()[i]);
        }
        System.out.println(";; Time: " + duration/1000000 + "ms");

        */
        System.out.println(duration/1000000);

        clientSocket.close();
    }
}
