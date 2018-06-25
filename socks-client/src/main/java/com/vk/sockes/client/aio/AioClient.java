package com.vk.sockes.client.aio;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.TimeUnit;

/**
 * @author liuax
 * @date 2018/6/9 上午10:20
 * @descripiton
 */
public class AioClient {

	public static void main(String[] args) throws IOException, InterruptedException {
		new AioClient().start();
	}

	public void start() throws IOException, InterruptedException {
		AsynchronousSocketChannel asc = AsynchronousSocketChannel.open();

		asc.connect(new InetSocketAddress("127.0.0.1",9000),asc,new MyAccept());
		TimeUnit.MINUTES.sleep(5);
	}

	static class MyAccept implements CompletionHandler<Void,AsynchronousSocketChannel> {

		@Override
		public void completed(Void it, AsynchronousSocketChannel asc) {
			try {
				System.out.println("connected");
				ByteBuffer bb = ByteBuffer.allocate(1024);
				bb.put("i am client".getBytes());
				bb.flip();
				ByteBuffer bb2 = ByteBuffer.allocate(1024);
				asc.read(bb2,asc,new MyRead(bb2));
				asc.write(bb,asc,new MyWrite(bb));
				System.out.println("end");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		@Override
		public void failed(Throwable exc, AsynchronousSocketChannel assc) {
			System.out.println("connect faild");
		}
	}

	static class MyRead implements CompletionHandler<Integer,AsynchronousSocketChannel>{

		private ByteBuffer bb;

		public MyRead(ByteBuffer bb){
			this.bb = bb;
		}

		@Override
		public void completed(Integer result, AsynchronousSocketChannel attachment) {
			if(result>=0){
				System.out.println("read:"+result);
				try {
					System.out.println("read:"+new String(bb.array(),0,result,"utf-8"));
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
			} else {
				System.out.println("read error");
				try {
					attachment.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			//continue
			bb.clear();
			attachment.read(bb,attachment,this);

		}

		@Override
		public void failed(Throwable exc, AsynchronousSocketChannel attachment) {
			System.out.println("read faild");
			try {
				attachment.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	static class MyWrite implements CompletionHandler<Integer,AsynchronousSocketChannel>{

		private ByteBuffer bb;

		public MyWrite(ByteBuffer bb){
			this.bb = bb;
		}

		@Override
		public void completed(Integer result, AsynchronousSocketChannel attachment) {
			if(result>0){
				System.out.println("write:"+result);
			}
			if(bb.hasRemaining()){
				attachment.write(bb,attachment,this);
			}
			//continue
			while(true){
				try {
					TimeUnit.SECONDS.sleep(2);
					bb.clear();
					bb.put("i am client2".getBytes());
					bb.flip();
					attachment.write(bb,attachment,this);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

		}

		@Override
		public void failed(Throwable exc, AsynchronousSocketChannel attachment) {
			System.out.println("write faild");

		}
	}
}
