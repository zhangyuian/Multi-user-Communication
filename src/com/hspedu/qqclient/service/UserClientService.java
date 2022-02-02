package com.hspedu.qqclient.service;

import com.hspedu.qqcommon.Message;
import com.hspedu.qqcommon.MessageType;
import com.hspedu.qqcommon.User;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;

/**
 * @author Zhang Yu
 * @version 1.0
 * 该类完成用户登录验证和用户注册等功能
 */
public class UserClientService {
    //因为我们可能在其他地方使用user信息，因此作为成员属性
    private User u = new User();
    //因为Socket在其他敌方也可能使用，因此作出属性
    private Socket socket;

    //根据UserId 和 pwd 到服务器验证该用户是否合法
    public boolean checkUser(String userId, String pwd) {
        boolean b = false;
        //创建User对象
        u.setUserId(userId);
        u.setPasswd(pwd);
        //连接到服务端，发送u对象

        try {
            socket = new Socket(InetAddress.getByName("192.168.0.100"), 9999);
            //得到ObjectOutputStream对象
            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
            oos.writeObject(u);//发送User对象

            //读取从服务器回复的Message对象
            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
            Message ms = (Message) ois.readObject();

            if(ms.getMesType().equals(MessageType.MESSAGE_LOGIN_SUCCEED)) {//登录ok


                //创建一个和服务器端保持通信的线程-> 创建一个类 ClientConnectServerThread
                //等待..
                ClientConnectServerThread clientConnectServerThread = new ClientConnectServerThread(socket);
                //启动客户端的线程
                clientConnectServerThread.start();
                //这里为了后面客户端的扩展，我们将线程放入到集合管理
                ManageClientConnectServerThread.addClientConnectServerThread(userId, clientConnectServerThread);
                b = true;
            } else {
                //如果登陆失败，我们就不能启动和服务器通信的线程，关闭socket
                socket.close();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return b;
    }

    //向服务器端请求在线用户列表
    public void onlineFriendList() {

        //发送一个Message，类型MESSAGE_GET_ONLINE_FRIEND
        Message message = new Message();
        message.setMesType(MessageType.MESSAGE_GET_ONLINE_FRIEND);
        message.setSender(u.getUserId());

        //发送给服务器
        //应该得到当前线程的socket 对应的ObjectOutputStream对象
        try {
            ClientConnectServerThread clientConnectServerThread = ManageClientConnectServerThread.getClientConnectServerThread(u.getUserId());
            Socket socket = clientConnectServerThread.getSocket();
            ObjectOutputStream oos =
                    new ObjectOutputStream(socket.getOutputStream());
            oos.writeObject(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //编写方法，退出客户端，并给服务端发送一个退出系统的message对象
    public void logout() {
        Message message = new Message();
        message.setMesType(MessageType.MESSAGE_CLIENT_EXIT);
        message.setSender(u.getUserId());//一定要指定我是那个客户端id
        try {
            //ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
            //使用下面这种方式获取的话可以适用于一个客户端有多个线程的情况
            ObjectOutputStream oos =
                    new ObjectOutputStream(ManageClientConnectServerThread.getClientConnectServerThread(u.getUserId()).getSocket().getOutputStream());
            oos.writeObject(message);
            System.out.println(u.getUserId() + "退出了系统 ");
            System.exit(0);//结束进程
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}