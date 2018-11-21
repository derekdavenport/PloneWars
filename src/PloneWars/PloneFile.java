/**
 * 
 */
package PloneWars;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @author dadave02
 * 
 */
public class PloneFile extends PloneObject {
	public static final String	TYPE	= "File";

	//public boolean used;
	//protected String etag;

	/**
	 * @param name
	 * @param parent
	 */
	public PloneFile(String name, PloneFolder parent) {
		super(name, parent);

		//used = false;

		parent.subFiles.put(name, this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see plonewars.PloneObject#download()
	 */
	@Override
	public boolean download() {
		PloneWarsApp.out.println("download: " + downloadURI.getPath());
		try {
			HttpURLConnection connection = (HttpURLConnection)downloadURI.toURL().openConnection();
			connection.setInstanceFollowRedirects(true); // but will not go from http to https

			boolean tryAgain = true;
			while(tryAgain) {
				tryAgain = false; // default only try once
				if(file.exists() && !PloneWarsApp.forceDownload) {
					//TODO: this doesn't work because I create the file in the constructor
					//connection.setRequestProperty("If-Modified-Since", MODIFIED_DATE_FORMAT.format(new Date(file.lastModified())));
				}
				/* if(connection.getResponseCode() == HttpURLConnection.HTTP_NOT_MODIFIED) {
					PloneWarsApp.out.println("\tfile up to date, skipping ... ");
				}
				else */
				if(connection.getResponseCode() == HttpURLConnection.HTTP_OK) { // HttpURLConnection.HTTP_MOVED_TEMP) {
					//etag = connection.getHeaderField("ETag");
					PloneWarsUtils.streamCopy(connection.getInputStream(), new FileOutputStream(file), true);
				}
				else if(connection.getResponseCode() >= HttpURLConnection.HTTP_MULT_CHOICE && connection.getResponseCode() <= HttpURLConnection.HTTP_SEE_OTHER) {
					connection = (HttpURLConnection)new URL(connection.getHeaderField("Location")).openConnection();
					tryAgain = true;
				}
				else {
					PloneWarsApp.out.println("\tfile error: " + connection.getResponseCode() + " " + connection.getResponseMessage());
					return false;
				}
			}
		}
		catch(Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	public boolean save() {
		PloneWarsApp.out.println("\tsaving ... ");
		return super.save();
	}

	protected Map<String, File> getFileMap() {
		Map<String, File> fileMap = new HashMap<String, File>();

		if(createdNew || PloneWarsApp.forceUpload)
			fileMap.put(getType().toLowerCase() + "_file", file);

		return fileMap;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see plonewars.PloneObject#upload()
	 */
	@Override
	public boolean upload() {
		PloneWarsApp.out.println("upload: " + uploadURI.getPath() + " ... ");

		URL editURL = getEditURL();
		if(editURL == null) {
			PloneWarsApp.out.println("\tERROR: could not get the edit URL.");
			return false;
		}

		Map<String, String> dataMap = getDataMap();
		Map<String, File> fileMap = getFileMap();

		HttpURLConnection connection = PloneWarsUtils.post(editURL, dataMap, fileMap);
		if(connection == null) {
			PloneWarsApp.out.println("\tERROR: could not open connection to " + editURL);
			return false;
		}

		connection.setInstanceFollowRedirects(false);

		try {
			if(connection.getResponseCode() == HttpURLConnection.HTTP_MOVED_TEMP) {
				PloneWarsApp.out.println("\tdone");
			}
			else if(connection.getResponseCode() == HttpURLConnection.HTTP_INTERNAL_ERROR) {
				PloneWarsApp.out.println("\tServer error: an object of the same name may already exist at this location.");
				return false;
			}
			else {
				PloneWarsApp.out.println("\tfailed permanently: " + connection.getResponseCode() + " " + connection.getResponseMessage());
				return false;
			}
		}
		catch(IOException e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}
}
