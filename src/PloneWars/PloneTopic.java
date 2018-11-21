/**
 * 
 */
package PloneWars;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * @author dadave02
 * 
 */
public class PloneTopic extends PloneObject {
	public static final String	TYPE	= "Topic";

	/**
	 * @param name
	 * @param parent
	 */
	public PloneTopic(String name, PloneFolder parent) {
		super(name, parent);
		PloneWarsApp.out.println("adding collection " + name + " to " + parent.name);
		parent.subTopics.put(name, this);
		// TODO Auto-generated constructor stub
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see plonewars.PloneObject#download()
	 */
	@Override
	public boolean download() {
		// Not going to bother to get details. :(
		return true;
	}

	public boolean save() {
		try {
			file.createNewFile();
			return super.save();
		}
		catch(IOException e) {
			e.printStackTrace();
		}
		return false;
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

		HttpURLConnection connection = PloneWarsUtils.post(editURL, getDataMap());
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
				PloneWarsApp.out.println("\tERROR: failed permanently: " + connection.getResponseCode() + " " + connection.getResponseMessage());
			}
		}
		catch(IOException e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}

}
