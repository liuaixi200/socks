package com.vk.socks.aio;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.concurrent.TimeUnit;

/**
 * @author liuax
 * @date 2018/6/9 上午8:15
 * @descripiton
 */
public class AioSocketServer {

	public static void main(String[] args) throws IOException, InterruptedException {
		new AioSocketServer().start();
		TimeUnit.MINUTES.sleep(60);
		System.out.println("start end");
	}

	public void start() throws IOException {
		AsynchronousServerSocketChannel assc = AsynchronousServerSocketChannel.open();

		assc.bind(new InetSocketAddress("127.0.0.1",9000));

		assc.accept(assc,new MyAccept());
	}

	static class MyAccept implements CompletionHandler<AsynchronousSocketChannel,AsynchronousServerSocketChannel>{

		@Override
		public void completed(AsynchronousSocketChannel result, AsynchronousServerSocketChannel assc) {
			try {
				System.out.println("remote:"+result.getRemoteAddress());
				assc.accept(assc,this);
				ByteBuffer bb = ByteBuffer.allocate(1024);
				result.read(bb,result,new MyRead(bb));

				ByteBuffer ws = ByteBuffer.allocate(1024);
				ws.put("sss".getBytes());
				ws.flip();
				result.write(ws,result,new MyWrite(ws));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void failed(Throwable exc, AsynchronousServerSocketChannel assc) {
			System.out.println("accept faild");
		}
	}

	static class MyRead implements CompletionHandler<Integer,AsynchronousSocketChannel>{

		private ByteBuffer bb;

		public MyRead(ByteBuffer bb){
			this.bb = bb;
		}

		@Override
		public void completed(Integer result, AsynchronousSocketChannel attachment) {
			if(result>0){
				try {
					System.out.println("read:"+result);
					System.out.println("read:"+ new String(bb.array(),0,result,"utf-8"));
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
			} else {
				System.out.println("read error");
			}

			//continue

			bb.clear();
			attachment.read(bb,attachment,this);

		}

		@Override
		public void failed(Throwable exc, AsynchronousSocketChannel attachment) {
			System.out.println("read faild");

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

		}

		@Override
		public void failed(Throwable exc, AsynchronousSocketChannel attachment) {
			System.out.println("write faild");

		}
	}
}
