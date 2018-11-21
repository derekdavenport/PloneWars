package PloneWars;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PloneWarsUtils {
	private static CookieManager cookieManager = new CookieManager();
	static {
		CookieHandler.setDefault(cookieManager);
	}
	
	public static String decodeHTML(String encodedHTML) {
		StringBuffer buffer = new StringBuffer();
		Matcher htmlEntityMatcher = Pattern.compile("&#(\\d+);").matcher(encodedHTML);
		while(htmlEntityMatcher.find()) {
			char[] temp = { (char)Integer.parseInt(htmlEntityMatcher.group(1)) };
			htmlEntityMatcher.appendReplacement(buffer, new String(temp));
		}
		htmlEntityMatcher.appendTail(buffer);
		return new String(buffer);
	}

	public static HttpURLConnection post(URL postURL, Map<String, String> dataMap, Map<String, File> fileMap) {
		//if(fileMap == null || fileMap.size() == 0)
		//return post(postURL, dataMap);

		HttpURLConnection connection = null;
		String lineEnd = "\r\n";
		String twoHyphens = "--";
		String boundary = "*****";
		int bytesRead;
		byte[] buffer = new byte[32 * 1024]; //32 KB

		try {
			connection = (HttpURLConnection)postURL.openConnection();

			connection.setDoInput(true);
			connection.setDoOutput(true);
			connection.setUseCaches(false);

			connection.setRequestMethod("POST");
			connection.setRequestProperty("Connection", "Keep-Alive");
			connection.setRequestProperty("Content-Type", "multipart/form-data; charset=utf-8; boundary=" + boundary);

			DataOutputStream dos = new DataOutputStream(connection.getOutputStream());

			if(dataMap != null) {
				dataMap.remove(null);
				dataMap.remove("");
				
				//write simple data
				for(Map.Entry<String, String> i : dataMap.entrySet()) {
					String key = i.getKey();
					String value = i.getValue();
					// useless
					assert key != null;
					assert !key.isEmpty();
					if(key == null || key.isEmpty()) {
						continue;
					}
					// can't call getBytes on null
					if(value == null) {
						value = "";
					}
					
					dos.writeBytes(twoHyphens + boundary + lineEnd);
					dos.writeBytes("Content-Disposition: form-data; name=\"");
					dos.write(key.getBytes("UTF-8"));
					dos.writeBytes("\"" + lineEnd + lineEnd);
					dos.write(value.getBytes("UTF-8"));
					dos.writeBytes(lineEnd);
				}
			}

			//write files
			if(fileMap != null) {
				fileMap.remove(null);
				fileMap.remove("");
				for(Map.Entry<String, File> i : fileMap.entrySet()) {
					String name = i.getKey();
					File file = i.getValue();
					
					if(name == null) {
						name = "";
					}

					if(file == null || !file.exists() || file.length() == 0) {
						PloneWarsApp.out.println("\tcannot post file, file does not exist");
						continue;
					}

					dos.writeBytes(twoHyphens + boundary + lineEnd);
					dos.writeBytes("Content-Disposition: form-data; name=\"");
					dos.write(name.getBytes("UTF-8"));
					dos.writeBytes("\"; filename=\"");
					dos.write(file.getName().getBytes("UTF-8"));
					dos.writeBytes("\"" + lineEnd);
					dos.writeBytes("Content-Type: " + Files.probeContentType(file.toPath()) + lineEnd + lineEnd);

					FileInputStream fis = new FileInputStream(file);
					while((bytesRead = fis.read(buffer, 0, buffer.length)) >= 0) {
						dos.write(buffer, 0, bytesRead);
					}
					fis.close();
					dos.writeBytes(lineEnd);
				}
			}

			dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

			dos.close();
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		return connection;
	}

	public static HttpURLConnection post(URL postURL, Map<String, String> dataMap) {
		HttpURLConnection connection = null;

		dataMap.remove(null);
		dataMap.remove("");
		try {
			String data = "";
			for(Map.Entry<String, String> i : dataMap.entrySet()) {
				data += URLEncoder.encode(i.getKey(), "UTF-8") + "=" + URLEncoder.encode(i.getValue() != null ? i.getValue() : "", "UTF-8") + "&";
			}
			data = data.substring(0, data.length() - 1); // remove trailing &

			// prepare connection
			connection = (HttpURLConnection)postURL.openConnection();
			connection.setInstanceFollowRedirects(false);
			connection.setDoOutput(true);
			connection.setRequestMethod("POST");
			connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			connection.setRequestProperty("Content-Length", Integer.toString(data.getBytes().length));

			// put data
			DataOutputStream dwriter = new DataOutputStream(connection.getOutputStream());
			dwriter.writeBytes(data);
			dwriter.close();
		}
		catch(IOException | NullPointerException e) {
			System.err.println(postURL);
			System.err.println(dataMap);
			e.printStackTrace();
		}
		return connection;
	}

	public static HttpCookie ploneLogin(URI loginURI, String username, String password) {
		HttpCookie loginCookie = null;
		// build data string
		Map<String, String> dataMap = new HashMap<String, String>();
		dataMap.put("__ac_name", username);
		dataMap.put("__ac_password", password);
		dataMap.put("form.submitted", "1");

		try {
			HttpURLConnection connection = post(loginURI.toURL(), dataMap);
			connection.getResponseCode(); // send the post
			// System.out.println("login response: " + connection.getResponseCode());
			List<HttpCookie> cookies = cookieManager.getCookieStore().get(loginURI);
			if (cookies.size() > 0)
				loginCookie = cookies.get(0);
		}
		catch(IOException e) {
			e.printStackTrace();
		}
		return loginCookie;
	}
	
	public static boolean logout() {
		return cookieManager.getCookieStore().removeAll();
	}
	
	public static boolean logout(URI logoutURI) {
		if (logoutURI == null)
			return logout();
		else {
			return cookieManager.getCookieStore().remove(logoutURI, cookieManager.getCookieStore().get(logoutURI).get(0));
		}
	}
	
	public static String ploneUriToString(URI ploneURI) {
		return ploneUriToString(ploneURI, "");
	}
	
	public static String ploneUriToString(URI ploneURI, String defaultString) {
		String string = null;
		try {
			PloneWarsApp.out.println("\tdownload string: " + ploneURI.getPath() + " ... ");
			HttpURLConnection connection = (HttpURLConnection)ploneURI.toURL().openConnection();
			connection.setInstanceFollowRedirects(true);
			if(connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
				PloneWarsApp.out.println("\tdownloading ... ");
				BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream(), PloneObject.UTF8));
				StringBuilder sb = new StringBuilder();
				String inputLine;
				while((inputLine = in.readLine()) != null) { // should only be one line, but we'll loop anyway
					sb.append(inputLine);
				}
				in.close();
				string = sb.toString();
			}
			else {
				string = defaultString;
				PloneWarsApp.out.println("\tunexpected response from server: " + connection.getResponseCode() + " " + connection.getResponseMessage());
			}
		}
		catch(Exception e) {
			string = defaultString;
			e.printStackTrace();
		}
		return string;
	}
	
	// from http://stackoverflow.com/questions/1574837/connecting-an-input-stream-to-an-outputstream
	public static void streamCopy(InputStream input, OutputStream output, boolean closeStreams) throws IOException {
		byte[] buffer = new byte[1024]; // Adjust if you want
		int bytesRead;
		while ((bytesRead = input.read(buffer)) != -1) {
			output.write(buffer, 0, bytesRead);
		}
		
		if (closeStreams) {
			input.close();
			output.close();
		}
	}

	// from https://thomaswabner.wordpress.com/2007/10/09/fast-stream-copy-using-javanio-channels/
	public static void fastStreamCopy(InputStream input, OutputStream output) throws IOException {
		final ReadableByteChannel inputChannel = Channels.newChannel(input);
		final WritableByteChannel outputChannel = Channels.newChannel(output);
		
		final ByteBuffer buffer = ByteBuffer.allocateDirect(16 * 1024);
		while (inputChannel.read(buffer) != -1) {
			// prepare the buffer to be drained
			buffer.flip();
			// write to the channel, may block
			outputChannel.write(buffer);
			// If partial transfer, shift remainder down
			// If buffer is empty, same as doing clear()
			buffer.compact();
		}
		// EOF will leave buffer in fill state
		buffer.flip();
		// make sure the buffer is fully drained.
		while (buffer.hasRemaining()) {
			outputChannel.write(buffer);
		}
		
		inputChannel.close();
		outputChannel.close();
	}
}
