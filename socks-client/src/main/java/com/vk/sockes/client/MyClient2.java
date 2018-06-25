package com.vk.sockes.client;

import com.sun.tools.internal.ws.wsdl.document.Output;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

/**
 * @author liuax
 * @date 2018/6/8 下午1:37
 * @descripiton
 */
public class MyClient2 {

	public static void main(String[] args) throws IOException, InterruptedException {
		for(int i=0;i<10;i++){
			Socket s = new Socket("127.0.0.1",9000);

			InputStream is = s.getInputStream();
			OutputStream os = s.getOutputStream();
			byte[] bs = new byte[1024];
			os.write("aa".getBytes());
			os.write("bb".getBytes());
			//os.write(0);
			//int n = is.read(bs);
			//System.out.println("s:"+new String(bs,0,n,"utf-8"));
		}

	}
}
