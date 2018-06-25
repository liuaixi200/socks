package com.vk.sockes.client.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

/**
 * @author liuax
 * @date 2018/6/8 下午9:42
 * @descripiton
 */
public class NioClient {

	public static void main(String[] args) throws IOException {
		NioClient nio = new NioClient();
		nio.start();
	}

	static int cn = 2;
	public void start() throws IOException {
		Selector selector = Selector.open();
		SocketChannel sc = SocketChannel.open();
		sc.configureBlocking(false);
		sc.connect(new InetSocketAddress("127.0.0.1",9000));
		sc.register(selector,SelectionKey.OP_CONNECT);

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
			if(key.isConnectable()){
				SocketChannel sc = (SocketChannel)key.channel();
				sc.finishConnect();
				sc.register(selector,SelectionKey.OP_READ|SelectionKey.OP_WRITE);

			} else if(key.isReadable()){
				SocketChannel sc = (SocketChannel)key.channel();
				ByteBuffer bb = ByteBuffer.allocate(1024);
				int n = sc.read(bb);
				bb.flip();
				byte[] bys = new byte[bb.remaining()];
				bb.get(bys);
				System.out.println("receive server:"+new String(bys,"utf-8"));
				//写2次
				if(cn>0){
					sc.register(selector,SelectionKey.OP_WRITE|SelectionKey.OP_READ);
					cn--;
				}
			} else if(key.isWritable()){
				String str = "ccc";
				System.out.println("writting on");
				ByteBuffer bb = ByteBuffer.allocate(1024);
				SocketChannel sc = (SocketChannel)key.channel();
				byte[] bys = str.getBytes();
				bb.put(bys);
				bb.flip();
				int n = sc.write(bb);
				System.out.println("n:"+n);
				if(n<=bys.length){
					//stop
					if(cn>0){
						cn--;
					} else {
						//key.cancel();
						sc.register(selector,SelectionKey.OP_READ);

						System.out.println("cancel write1");

					}
				} else {
					//cancel
					key.cancel();
					System.out.println("cancel write");
				}
			}
		}
	}
}
