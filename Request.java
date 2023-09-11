import java.util.Random;

public class Request {
    private int cursor;
    private String[] domains;
    private byte[] requestData; 

    public Request(String name) {
         
        domains = name.split("\\.", 0);        
        
        this.requestData=new byte[512];

        Random rand = new Random();

        int id = rand.nextInt(256);

        requestData[1] = (byte)id;

        requestData[5] = 1;

        cursor = 12;

        for (int i = 0; i < domains.length; i++){
            requestData[cursor] = (byte) domains[i].length();
            cursor++;
            for (int x = 0; x < domains[i].length(); x++){
                requestData[cursor] = (byte)domains[i].charAt(x);
                cursor++;
            }
        }
        cursor += 2;

        requestData[cursor] = 1;

        cursor += 2;
        requestData[cursor] = 1;
        cursor += 1;
    }

    public int getCursor(){
        return this.cursor;
    }

    public byte[] getRequest(){
        return this.requestData;
    }

    

}
