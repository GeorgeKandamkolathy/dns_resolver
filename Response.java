import java.io.*;
import java.net.*;
import java.util.HexFormat;
import java.util.Random;


public class Response {

    private DatagramPacket packet;
    private int cursor;
    private byte[] data;
    private int response_id;
    private int QR;
    private int ANCOUNT;
    private int NSCOUNT;
    private int ARCOUNT;
    private int authority;
    private int truncated;
    private int rcode;
    private String[] answersName;
    private String[] answersAddress;
    private int answerType;
    private String[] nameServersName;
    private String[] nameServersAddress;
    private String[] additionalResourcesName;
    private String[] additionalResourcesAddress;

    public Response(DatagramPacket responseData) {
        packet = responseData;
        data = packet.getData();
        cursor = 11;
        response_id = this.data[1] & 0xFF;
        QR = this.data[2] >> 7;
        authority = (this.data[2] >> 2) & 1;
        truncated = (this.data[2] >> 1) & 1;
        rcode = this.data[3];
        ANCOUNT = this.data[7];
        NSCOUNT = this.data[9];
        ARCOUNT = this.data[11];
        this.answersName = new String[ANCOUNT];
        this.answersAddress = new String[ANCOUNT];
        this.nameServersName = new String[NSCOUNT];
        this.nameServersAddress = new String[NSCOUNT];
        this.additionalResourcesName = new String[ARCOUNT];
        this.additionalResourcesAddress = new String[ARCOUNT];

        while(this.data[cursor] != 1 && cursor < this.data.length){
            if( cursor == this.data.length - 2){
                return;
            }
            cursor += 1;
        }
        
        cursor += 3;

        for (int resI = 0; resI < ANCOUNT; resI++){

            this.answersName[resI] = getName();
            cursor += 2;
            this.answerType = (int)this.data[cursor];
            cursor += 8;
            
            this.answersAddress[resI] = parseAnswer();

        }

        for (int resI = 0; resI < NSCOUNT; resI++){
            this.nameServersName[resI] = getName();
            cursor += 10;
            
            this.nameServersAddress[resI] = getServerName();
            cursor += 1;
        }

        for (int resI = 0; resI < ARCOUNT; resI++){
            this.additionalResourcesName[resI] = getName();
            cursor += 2;
            int answerType = (int)this.data[cursor];
            cursor += 8;
            String answer = parseAnswer();
            if (answerType == 1){
                this.additionalResourcesAddress[resI] = answer;
            }

        }
    }

    private String getCompressedName(int compressedCursor) {
        String address = "";
        byte byteVal = (byte)HexFormat.fromHexDigits("C0");


        if (this.data[compressedCursor] == byteVal){
            compressedCursor += 1;
            address += getCompressedName(this.data[compressedCursor] & 0xFF);
            return address;
        }

        while (this.data[compressedCursor] != 0) {

            int length = this.data[compressedCursor] & 0xFF ;

            if (this.data[compressedCursor] == byteVal){
                compressedCursor += 1;
                address += getCompressedName(this.data[compressedCursor] & 0xFF);
                return address;
            }
            else {
                //compressedCursor += 1;
                for(int x = 0; x <= length; x++){
                    if (this.data[compressedCursor] == 0) {
                        break;
                    }
                    if (this.data[compressedCursor] < 0x1F) {
                        address += '.';
                    }
                    else{
                        address += (char)this.data[compressedCursor];
                    }
                    compressedCursor += 1;
                }
            }

        }
        
        return address;
    }

    public String getName() {
        String address = "";
        byte byteVal = (byte)HexFormat.fromHexDigits("C0");


        while (this.data[cursor] != 0){

            if (this.data[cursor] == byteVal){
                cursor += 1;
                address += getCompressedName(this.data[cursor] & 0xFF);
                return address;
            }
            else {
                int length = this.data[cursor] & 0xFF;
                cursor += 1;
                for(int x = 0; x < length; x++){
                    if (this.data[cursor] == byteVal){
                        cursor += 1;
                        address += getCompressedName(this.data[cursor] & 0xFF);
                        return address;
                    }
                    else{
                        if (this.data[cursor] < 0x1F && x != 0){
                            address += '.';
                        }
                        else{
                            address += (char)this.data[cursor];
                        }
                        cursor += 1;
                    }
                }
                address += '.';
            }
        }

        return address;
    }

    private String getCompressedServerName(int compressedCursor) {
        String address = "";

        byte byteVal = (byte)HexFormat.fromHexDigits("C0");

        if (this.data[compressedCursor] == byteVal){
            compressedCursor += 1;
            address += getCompressedServerName(this.data[compressedCursor] & 0xFF);
            return address;
        }
        else {
            while (this.data[compressedCursor] != 0){
                if (this.data[compressedCursor] == byteVal){
                    compressedCursor += 1;
                    address += getCompressedServerName(this.data[compressedCursor] & 0xFF);
                    return address;
                }
                int subLength = this.data[compressedCursor] & 0xFF;
                compressedCursor += 1;
                for(int y = 0; y < subLength; y++){
                    address += (char)this.data[compressedCursor];
                    compressedCursor += 1;
                }
                address += '.';
            }
        }
        
        return address;
    }

    private String getServerName() {
        String address = "";
        byte byteVal = (byte)HexFormat.fromHexDigits("C0");


        while (this.data[cursor] != 0){

            if (this.data[cursor] == byteVal){
                cursor += 1;
                address += getCompressedServerName(this.data[cursor] & 0xFF);
                return address;
            }
            else {
                int length = this.data[cursor] & 0xFF;
                cursor += 1;
                for(int x = 1; x < length;){
                    if (this.data[cursor] == byteVal){
                        cursor += 1;
                        x++;
                        address += getCompressedServerName(this.data[cursor] & 0xFF);
                        return address;
                    }
                    else{
                        int subLength = this.data[cursor] & 0xFF;
                        cursor += 1;
                        x++;
                        for(int y = 0; y < subLength; y++){
                            address += (char)this.data[cursor];
                            cursor += 1;
                            x++;
                        }
                        address += '.';
                    }
                }
            }
        }

        return address;
    }





    private String parseAnswer() {
        String address = "";
        byte byteVal = (byte)HexFormat.fromHexDigits("C0");

        while (this.data[cursor] != 0){

            if (this.data[cursor] == byteVal && this.answerType == 5){
                cursor += 1;
                address += getCompressedServerName(this.data[cursor] & 0xFF);
                return address;
            }
            else {
                int length = this.data[cursor] & 0xFF;
                cursor += 1;
                for(int x = 0; x < length; x++){
                    if (this.data[cursor] == byteVal && this.answerType == 5){
                        cursor += 1;
                        address += getCompressedServerName(this.data[cursor] & 0xFF);
                        return address;
                    }
                    else if (this.answerType == 5){
                        if (this.data[cursor] < 0x1F && x != 0){
                            address += '.';
                        }
                        else if (x != 0){
                            address += (char)this.data[cursor];
                        }
                        cursor += 1;
                    }
                    else{
                        address += (int)this.data[cursor] & 0xFF;
                        if (x != length - 1){
                            address += '.';
                        }
                        cursor += 1;
                    }
                }
                break;
            }
        }

        return address;
    }


    public int getAnswersCount(){
        return this.ANCOUNT;
    }

    public int getNameServerCount(){
        return this.NSCOUNT;
    }

    public int getAdditionalResourcesCount(){
        return this.ARCOUNT;
    }

    public String[] getAnswersNames(){
        return this.answersName;
    }

    public String[] getAnswersAddress(){
        return this.answersAddress;
    }

    public String[] getNameServersName(){
        return this.nameServersName;
    }

    public String[] getNameServersAddress(){
        return this.nameServersAddress;
    }

    public String[] getAdditionalResourcesName(){
        return this.additionalResourcesName;
    }

    public String[] getAdditionalResourcesAddress(){
        return this.additionalResourcesAddress;
    }

    public byte[] getRawResponse(){
        return this.data;
    }

    public boolean isTruncated(){
        return this.truncated == 1;
    }

    public boolean isAuthority(){
        return this.authority == 1;
    }

    public boolean isError(){
        return this.rcode != 0;
    }

    public int errorCode(){
        return this.rcode;
    }

    public String errorName(){
        if (this.rcode == 0) {
            return "NOERROR";
        }
        else if ( this.rcode == 1){
            return "Format Error";
        }
        else if (this.rcode == 2){
            return "Server Failure";
        }
        else if (this.rcode == 3){
            return "Name Error";
        }
        else if (this.rcode == 4){
            return "Not Implemented";
        }
        else {
            return "Error";
        }
    }

    public int getId(){
        return this.response_id;
    }

    public int getAnswerType(){
        return this.answerType;
    }

    public DatagramPacket getPacket(){
        return this.packet;
    }
}
