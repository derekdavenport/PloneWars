package PloneWars;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.swing.text.html.HTML;

public class PloneURI {
	public HTML.Attribute						attr;
	public URI											downloadURI, uploadURI;
	public int											tagStart, origUriLength;
	public String										append;
	public boolean wasUID;

	private static Map<URI, String>	uidLookup	= new HashMap<URI, String>();

	public PloneURI(URI downloadURI, URI uploadURI, HTML.Attribute attr, int tagStart, int origUriLength, String append, boolean wasUID) {
		this.attr = attr;
		this.downloadURI = downloadURI;
		this.uploadURI = uploadURI;
		this.tagStart = tagStart;
		this.origUriLength = origUriLength;
		this.append = append;
		this.wasUID = wasUID;

		// why do I do this? seems better to use a null entry to indicate error
		if(!uidLookup.containsKey(uploadURI))
			uidLookup.put(uploadURI, null);

	}

	public String getUID() {
		String uid = uidLookup.get(uploadURI);
		if (uid == null) {
			try {
				URL url = uploadURI.resolve("uuid").toURL();
				//PloneWarsApp.out.println("looking up uid from " + url);
				HttpURLConnection connection = (HttpURLConnection)url.openConnection();
				if(connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
					BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream(), PloneObject.UTF8));
					String inputLine;
					while((inputLine = in.readLine()) != null) {
						inputLine = inputLine.trim();
						if(inputLine.matches("[a-f0-9]+")) {
							uid = inputLine;
							uidLookup.put(uploadURI, uid);
							break;
						}
					}
					in.close();
				}
				else {
					PloneWarsApp.out.println("\tunexpected response when trying to fetch uid for " + url + " : " + connection.getResponseCode() + " " + connection.getResponseMessage());
				}
			}
			catch(IOException e) {
				e.printStackTrace();
			}
		}
		return uid;
	}

	//public static PloneURI getInstance(String uri, int pos, String append);

}
