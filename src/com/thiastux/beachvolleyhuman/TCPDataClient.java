/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.thiastux.beachvolleyhuman;

import com.jme3.math.Quaternion;
import com.sun.org.apache.xpath.internal.functions.WrongNumberArgsException;
import com.thiastux.beachvolleyhuman.model.Const;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author mathias
 */
public class TCPDataClient extends Thread {

    private final Object lock;
    private Socket socket;
    private BufferedReader inputBuffer;
    private Quaternion[] animationPacket;
    private HashMap<Integer, Float[]> columnIndexMap;
    private Quaternion[] priorQuaternions;
    private boolean isExecuted = false;

    TCPDataClient(Object lock, String[] args) {
        this.lock = lock;
        animationPacket = new Quaternion[12];
        initializeSocket(args[0], args[1]);
        parseParameters(args);
    }

    @Override
    public void run() {
        String line;

        String[] values;

        float qw;
        float qx;
        float qy;
        float qz;

        while (isExecuted) {
            try {
                line = inputBuffer.readLine();
                if(line!=null){
                    values = line.split(" ");
                    if(values.length>=4){
                        for (int i = 0; i < 12; i++) {
                            try {
                                Float[] columnValues = columnIndexMap.get(i);
                                qw = Float.parseFloat(values[columnValues[0].intValue()]);
                                qx = Float.parseFloat(values[columnValues[1].intValue()]);
                                qy = Float.parseFloat(values[columnValues[2].intValue()]);
                                qz = Float.parseFloat(values[columnValues[3].intValue()]);
                                synchronized (lock) {
                                    Const.animationStart = true;
                                    animationPacket[i] = new Quaternion(qx / 1000.0f, qy / 1000.0f, qz / 1000.0f, qw / 1000.0f);
                                }
                            } catch (NullPointerException | ArrayIndexOutOfBoundsException e) {
                                animationPacket[i] = null;
                            }
                        }
                    }
                }
            } catch (IOException ex) {
                Logger.getLogger(TCPDataClient.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        try {
            inputBuffer.close();
            socket.close();
        } catch (IOException ex) {
            Logger.getLogger(TCPDataClient.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public Quaternion[] getData() {
        synchronized (lock) {
            return animationPacket;
        }
    }

    /**
     * Parse parameters to bind column of the dataset with the quaternion of the
     * correct limb.
     *
     * @param args
     */
    private void parseParameters(String[] args) {
        columnIndexMap = new HashMap<>();
        for (int i = 0; i < 12; i++) {
            columnIndexMap.put(i, new Float[4]);
        }
        try {
            //If the numbers of the parameters (-1 for the port number) isn't
            //multiple of 5, throw an exception
            if ((args.length - 2) % 5 != 0) {
                throw new WrongNumberArgsException("Wrong number of parameters!");
            }
            for (int i = 2; i < args.length; i += 5) {

                //Read the command
                String param = args[i];
                
                if(param.endsWith("l"))
                    Const.useLegs=true;

                //Get the index of the array corresponding to the command
                Float[] paramsValues = new Float[4];
                Integer limbColIndex = null;
                Integer limbPriorIndex = null;
                try {
                    limbColIndex = Const.BindColumIndex.get(param).getCode();
                } catch (NullPointerException e) {
                    limbPriorIndex = Const.PriorQuatIndex.get(param).getCode();
                }
                paramsValues[0] = Float.parseFloat(args[i + 1]);
                paramsValues[1] = Float.parseFloat(args[i + 2]);
                paramsValues[2] = Float.parseFloat(args[i + 3]);
                paramsValues[3] = Float.parseFloat(args[i + 4]);

                columnIndexMap.put(limbColIndex, paramsValues);
                if (limbPriorIndex != null) {
                    priorQuaternions[limbPriorIndex] = new Quaternion(paramsValues[0], paramsValues[1], paramsValues[2], paramsValues[3]);
                }
            }
        } catch (NullPointerException | IndexOutOfBoundsException e) {
            Logger.getLogger(BeachVolleyballSimulator.class.getName()).log(Level.SEVERE, null, e);
            System.exit(-1);
        } catch (WrongNumberArgsException | NumberFormatException e) {
            Logger.getLogger(TCPDataClient.class.getName()).log(Level.SEVERE, null, e);
            System.exit(-1);
        }
    }

    private void initializeSocket(String address, String port) {
        try {
            socket = new Socket(address, Integer.parseInt(port));
            inputBuffer = new BufferedReader(
                    new InputStreamReader(
                            socket.getInputStream()));
        } catch (IOException ex) {
            Logger.getLogger(TCPDataClient.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    void stopExecution() {
        synchronized (lock) {
            isExecuted = false;
        }
    }

    void startExecution() {
        synchronized (lock) {
            isExecuted = true;
        }
        this.start();
    }

    private Quaternion preProcessingQuaternion(Quaternion quaternion, int i) {
        //Normalize quaternion to adjust lost of precision using mG.
        Quaternion outputQuat = normalizeQuaternion(quaternion);
        //Quaternion outputQuat = Quaternion.IDENTITY;
        //Convert the reference system from left hand (Xsense) to right hand (opengl), swapping x-axis with z-axis
        //outputQuat = new Quaternion(outputQuat.getZ(), outputQuat.getY(), outputQuat.getX(), outputQuat.getW());
        // Compose two rotations:
        // First, rotate the rendered model to face inside the screen (negative z)
        // Then, rotate the rendered model to have the torso horizontal (facing downwards, leg facing north)
        Quaternion quat1 = new Quaternion((float) Math.sin(Math.toRadians(-90.0 / 2.0)), 0.0f, 0.0f, (float) Math.cos(Math.toRadians(-90.0 / 2.0)));
        quat1.normalizeLocal();
        Quaternion quat2 = new Quaternion(0.0f, (float) Math.sin(Math.toRadians(+180.0 / 2.0)), 0.0f, (float) Math.cos(Math.toRadians(180.0 / 2.0)));
        quat2.normalizeLocal();
        Quaternion preRot = quat1.mult(quat2);
        if (i < 3) {
            float[] rArmsAngles = {0.0f, (float) Math.toRadians(-90), 0.0f};
            preRot = preRot.mult(new Quaternion(rArmsAngles));
            return outputQuat.mult(preRot);
        } else if(i<7){
            float[] lArmsAngles = {0.0f, (float) Math.toRadians(90), 0.0f};
            preRot = preRot.mult(new Quaternion(lArmsAngles));
            return outputQuat.mult(preRot);
        }
        return outputQuat.mult(preRot);
    }

    private Quaternion normalizeQuaternion(Quaternion quaternion) {
        return quaternion.normalizeLocal();
    }

}
