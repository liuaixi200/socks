package com.vk.socks;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * @author liuax
 * @date 2018/6/8 上午10:08
 * @descripiton
 */
public class MySocketServer {

	static volatile int cn = 0 ;
	public static void main(String[] args) throws Exception {
		test2();
	}

	public static void test2() throws Exception {
		ServerSocket ss = new ServerSocket(9000,1);
		System.out.println("start success");
		while(true){
			Socket s = ss.accept();
			System.out.println("port:"+s.getInetAddress().getHostAddress()+":"+s.getRemoteSocketAddress());
			new Thread(()->hand1(s)).start();
		}
	}

	public static void test1() throws Exception {
		ServerSocket ss = new ServerSocket(9000);
		System.out.println("start success");
		Socket s = ss.accept();
		hand1(s);
		TimeUnit.SECONDS.sleep(20);
	}

	public static void hand1(Socket s) {
		try {
			InputStream is = s.getInputStream();
			byte[] bys = new byte[1024];
			int n = is.read(bys);
			System.out.println("收到信息：" + new String(bys, 0, n, "utf-8"));
			OutputStream os = s.getOutputStream();
			byte[] oys = "i am server".getBytes();
			os.write(oys);
			os.flush();
			oys = "i am server2".getBytes();
			os.write(oys);
			os.flush();
			cn++;
			System.out.println("current n:"+cn);
			TimeUnit.SECONDS.sleep(5);
			s.close();
		} catch (Exception e){
			e.printStackTrace();
		} finally {
			cn--;
		}
	}
}
