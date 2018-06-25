package com.vk.socks;

import com.sun.org.apache.bcel.internal.generic.Select;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.CharsetDecoder;
import java.util.Iterator;
import java.util.Set;

/**
 * @author liuax
 * @date 2018/6/8 下午8:56
 * @descripiton
 */
public class NioSockerServer {

	public static void main(String[] args) throws IOException {
		NioSockerServer nio = new NioSockerServer();
		System.out.println("start success");
		nio.start();
	}

	static int cn = 2;
	public void start() throws IOException {
		Selector selector = Selector.open();
		ServerSocketChannel ssc = ServerSocketChannel.open();
		ssc.configureBlocking(false);

		ssc.bind(new InetSocketAddress("127.0.0.1",9000));
		ssc.register(selector,SelectionKey.OP_ACCEPT);

		handleSelect(selector);
	}

	public void handleSelect(Selector selector) throws IOException {

		while(true){
			selector.select();
			Set<SelectionKey> set =  selector.selectedKeys();
			Iterator<SelectionKey> it = set.iterator();
			while(it.hasNext()){
				SelectionKey sk = it.next();
				it.remove();
				handleSelectKey(selector,sk);
			}
		}
	}

	public void handleSelectKey(Selector selector,SelectionKey key) throws IOException {

		if(key.isValid()){
			if(key.isAcceptable()){
				ServerSocketChannel ssc = (ServerSocketChannel)key.channel();
				SocketChannel sc = ssc.accept();
				sc.configureBlocking(false);
				//sc.finishConnect();
				InetSocketAddress rsa = (InetSocketAddress) sc.getRemoteAddress();
				System.out.println(rsa);
				cn=2;
				sc.register(selector,SelectionKey.OP_READ);

			} else if(key.isReadable()){
				SocketChannel sc = (SocketChannel)key.channel();
				ByteBuffer bb = ByteBuffer.allocate(1024);
				int n = sc.read(bb);
				bb.flip();
				byte[] bys = new byte[bb.remaining()];
				System.out.println(n);
				bb.get(bys);
				System.out.println("receive client:"+new String(bys,"utf-8"));
				//写2次
				if(cn>0){
					sc.register(selector,SelectionKey.OP_WRITE);
					System.out.println("rc write");
					cn--;
				}
				if(n<0){
					System.out.println("close client");
					key.cancel();
					sc.close();
				}
			} else if(key.isWritable()){
				String str = "sss";
				System.out.println("writting on");
				ByteBuffer bb = ByteBuffer.allocate(1024);
				SocketChannel sc = (SocketChannel)key.channel();
				byte[] bys = str.getBytes();
				bb.put(bys);
				bb.flip();
				int n = sc.write(bb);
				if(n<=bys.length){
					//stop
					if(cn>0){
						cn--;
					} else {
						key.cancel();
						System.out.println("cancel write");

					}
				} else {
					//cancel
					key.cancel();
				}
			}
		}
	}
}
