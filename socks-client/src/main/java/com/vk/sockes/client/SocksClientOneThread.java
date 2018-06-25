package com.vk.sockes.client;


import java.io.*;
import java.net.*;

import static com.vk.sockes.client.SocksConsts.*;

/**
 * @author liuax
 * @date 2018/6/8 上午10:23
 * @descripiton
 */
public class SocksClientOneThread {

	public static boolean useV4 = false;

	protected Socket connect(InetSocketAddress source,InetSocketAddress endpoint, int timeout, String username, String pass) throws IOException {
		final long deadlineMillis;

		if (timeout == 0) {
			deadlineMillis = 0L;
		} else {
			long finish = System.currentTimeMillis() + timeout;
			deadlineMillis = finish < 0 ? Long.MAX_VALUE : finish;
		}
		Socket s = new Socket();
		s.connect(source,timeout);
		s.setSoTimeout(timeout);
		OutputStream cmdOut = s.getOutputStream();
		InputStream cmdIn = s.getInputStream();

		// cmdIn & cmdOut were initialized during the privilegedConnect() call
		BufferedOutputStream out = new BufferedOutputStream(cmdOut, 512);
		InputStream in = cmdIn;

		if (useV4) {
			// SOCKS Protocol version 4 doesn't know how to deal with

			connectV4(in, out, endpoint);
		} else {
			connectV5(in,out,endpoint,username,pass);
		}


		return s;
	}

	private boolean authenticate(byte method, InputStream in,
	                             BufferedOutputStream out,String userName,String password) throws IOException {
		// No Authentication required. We're done then!
		if (method == NO_AUTH)
			return true;
		/**
		 * User/Password authentication. Try, in that order :
		 * - The application provided Authenticator, if any
		 * - the user.name & no password (backward compatibility behavior).
		 */
		if (method == USER_PASSW) {

			if (userName == null)
				return false;
			out.write(1);
			out.write(userName.length());
			try {
				out.write(userName.getBytes("ISO-8859-1"));
			} catch (java.io.UnsupportedEncodingException uee) {
				assert false;
			}
			if (password != null) {
				out.write(password.length());
				try {
					out.write(password.getBytes("ISO-8859-1"));
				} catch (java.io.UnsupportedEncodingException uee) {
					assert false;
				}
			} else
				out.write(0);
			out.flush();
			byte[] data = new byte[2];
			int i = readSocksReply(in, data);
			if (i != 2 || data[1] != 0) {
                /* RFC 1929 specifies that the connection MUST be closed if
                   authentication fails */
				out.close();
				in.close();
				return false;
			}
			/* Authentication succeeded */
			return true;
		}
		return false;
	}

	private void connectV5(InputStream in, BufferedOutputStream out,
	                       InetSocketAddress endpoint, String username, String pass) throws IOException {

		// This is SOCKS V5
		out.write(PROTO_VERS);
		out.write(2);
		out.write(NO_AUTH);
		out.write(USER_PASSW);
		out.flush();
		byte[] data = new byte[2];
		int i = readSocksReply(in, data);
		if (i != 2 || ((int)data[0]) != PROTO_VERS) {
			// Maybe it's not a V5 sever after all
			// Let's try V4 before we give up
			// SOCKS Protocol version 4 doesn't know how to deal with
			// DOMAIN type of addresses (unresolved addresses here)

			connectV4(in, out, endpoint);
			return;
		}
		if (((int)data[1]) == NO_METHODS)
			throw new SocketException("SOCKS : No acceptable methods");
		if (!authenticate(data[1], in, out,username,pass)) {

			throw new SocketException("SOCKS : authentication failed");
		}
		out.write(PROTO_VERS);
		out.write(CONNECT);
		out.write(0);
		/* Test for IPV4/IPV6/Unresolved */
		if (endpoint.isUnresolved()) {
			out.write(DOMAIN_NAME);
			out.write(endpoint.getHostName().length());
			try {
				out.write(endpoint.getHostName().getBytes("ISO-8859-1"));
			} catch (java.io.UnsupportedEncodingException uee) {
				assert false;
			}
			out.write((endpoint.getPort() >> 8) & 0xff);
			out.write((endpoint.getPort() >> 0) & 0xff);
		} else if (endpoint.getAddress() instanceof Inet6Address) {
			out.write(IPV6);
			out.write(endpoint.getAddress().getAddress());
			out.write((endpoint.getPort() >> 8) & 0xff);
			out.write((endpoint.getPort() >> 0) & 0xff);
		} else {
			out.write(IPV4);
			out.write(endpoint.getAddress().getAddress());
			out.write((endpoint.getPort() >> 8) & 0xff);
			out.write((endpoint.getPort() >> 0) & 0xff);
		}
		out.flush();
		data = new byte[4];
		i = readSocksReply(in, data);
		if (i != 4)
			throw new SocketException("Reply from SOCKS server has bad length");
		SocketException ex = null;
		int len;
		byte[] addr;
		switch (data[1]) {
			case REQUEST_OK:
				// success!
				switch(data[3]) {
					case IPV4:
						addr = new byte[4];
						i = readSocksReply(in, addr);
						if (i != 4)
							throw new SocketException("Reply from SOCKS server badly formatted");
						data = new byte[2];
						i = readSocksReply(in, data);
						if (i != 2)
							throw new SocketException("Reply from SOCKS server badly formatted");
						break;
					case DOMAIN_NAME:
						len = data[1];
						byte[] host = new byte[len];
						i = readSocksReply(in, host);
						if (i != len)
							throw new SocketException("Reply from SOCKS server badly formatted");
						data = new byte[2];
						i = readSocksReply(in, data);
						if (i != 2)
							throw new SocketException("Reply from SOCKS server badly formatted");
						break;
					case IPV6:
						len = data[1];
						addr = new byte[len];
						i = readSocksReply(in, addr);
						if (i != len)
							throw new SocketException("Reply from SOCKS server badly formatted");
						data = new byte[2];
						i = readSocksReply(in, data);
						if (i != 2)
							throw new SocketException("Reply from SOCKS server badly formatted");
						break;
					default:
						ex = new SocketException("Reply from SOCKS server contains wrong code");
						break;
				}
				break;
			case GENERAL_FAILURE:
				ex = new SocketException("SOCKS server general failure");
				break;
			case NOT_ALLOWED:
				ex = new SocketException("SOCKS: Connection not allowed by ruleset");
				break;
			case NET_UNREACHABLE:
				ex = new SocketException("SOCKS: Network unreachable");
				break;
			case HOST_UNREACHABLE:
				ex = new SocketException("SOCKS: Host unreachable");
				break;
			case CONN_REFUSED:
				ex = new SocketException("SOCKS: Connection refused");
				break;
			case TTL_EXPIRED:
				ex =  new SocketException("SOCKS: TTL expired");
				break;
			case CMD_NOT_SUPPORTED:
				ex = new SocketException("SOCKS: Command not supported");
				break;
			case ADDR_TYPE_NOT_SUP:
				ex = new SocketException("SOCKS: address type not supported");
				break;
		}
		if (ex != null) {
			in.close();
			out.close();
			throw ex;
		}
	}

	private void connectV4(InputStream in, OutputStream out,
	                       InetSocketAddress endpoint) throws IOException {
		if (!(endpoint.getAddress() instanceof Inet4Address)) {
			throw new SocketException("SOCKS V4 requires IPv4 only addresses");
		}
		out.write(PROTO_VERS4);
		out.write(CONNECT);
		out.write((endpoint.getPort() >> 8) & 0xff);
		out.write((endpoint.getPort() >> 0) & 0xff);
		out.write(endpoint.getAddress().getAddress());

		out.write(0);
		out.flush();
		byte[] data = new byte[8];
		int n = readSocksReply(in, data);
		if (n != 8)
			throw new SocketException("Reply from SOCKS server has bad length: " + n);
		if (data[0] != 0 && data[0] != 4)
			throw new SocketException("Reply from SOCKS server has bad version");
		SocketException ex = null;
		switch (data[1]) {
			case 90:
				// Success!
				break;
			case 91:
				ex = new SocketException("SOCKS request rejected");
				break;
			case 92:
				ex = new SocketException("SOCKS server couldn't reach destination");
				break;
			case 93:
				ex = new SocketException("SOCKS authentication failed");
				break;
			default:
				ex = new SocketException("Reply from SOCKS server contains bad status");
				break;
		}
		if (ex != null) {
			in.close();
			out.close();
			throw ex;
		}
	}

	private int readSocksReply(InputStream in, byte[] data) throws IOException {
		int len = data.length;
		int received = 0;
		for (int attempts = 0; received < len && attempts < 3; attempts++) {
			int count;
			try {
				count = in.read(data, received, len - received);
			} catch (SocketTimeoutException e) {
				throw new SocketTimeoutException("Connect timed out");
			}
			if (count < 0)
				throw new SocketException("Malformed reply from SOCKS server");
			received += count;
		}
		return received;
	}
}
