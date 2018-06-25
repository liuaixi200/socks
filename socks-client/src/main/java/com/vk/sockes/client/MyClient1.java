package com.vk.sockes.client;

import jdk.internal.util.xml.impl.Input;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * @author liuax
 * @date 2018/6/8 上午11:30
 * @descripiton
 */
public class MyClient1 {

	public static void main(String[] args) throws IOException {
		SocksClientOneThread sc = new SocksClientOneThread();
		Socket s = sc.connect(new InetSocketAddress("127.0.0.1",1080),new InetSocketAddress("127.0.0.1",9000),
				10000,"user","123456");
		InputStream is = s.getInputStream();
		OutputStream os = s.getOutputStream();
		os.write("aa".getBytes());
		os.write("bb".length());
		while (true){
			byte[] bs = new byte[1024];
			int n = is.read(bs);
			System.out.println(new String(bs,0,n,"utf-8"));
		}

	}
}
