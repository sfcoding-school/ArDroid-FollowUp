package castellini.jacopo.ardroidfollowup;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class ArduThread extends Thread {
    String command;
    FileOutputStream mOutputStream;
    FileInputStream mInputStream;

    public ArduThread(String command, FileOutputStream mOutputStream, FileInputStream mInputStream) {
        this.command = command;
        this.mOutputStream = mOutputStream;
        this.mInputStream = mInputStream;
    }

    public void run() {
        sendCommand();
    }

    private boolean sendCommand() {
        byte[] buffer = "null".getBytes();
        if (command.equals("stop"))
            buffer = "0V;".getBytes();
        else if (command.equals("left"))
            buffer = "0V;1V1;2A0;2B3;".getBytes();
        else if (command.equals("right"))
            buffer = "0V;1V1;2A3;2B0;".getBytes();
        else if (command.equals("forward"))
            buffer = "0V;1V0;2V3;".getBytes();
        else if (command.equals("backward"))
            buffer = "0V;1V1;2V3;".getBytes();

        if (mOutputStream != null) {
            try {
                mOutputStream.write(buffer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (mInputStream != null) {
            int ret = 0;
            byte[] response = new byte[2];
            while (ret <= 0) {
                try {
                    ret = mInputStream.read(response);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return response[0] == '4' && response[1] == '2';
        }
        return false;
    }

}