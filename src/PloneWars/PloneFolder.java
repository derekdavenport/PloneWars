/**
 * 
 */
package PloneWars;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.channels.Channels;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * @author dadave02
 * 
 */
public class PloneFolder extends PloneObject {
	public static final String					TYPE						= "Folder";

	public HashMap<String, PloneFolder>	subFolders;
	public HashMap<String, PlonePage>		subPages;
	public HashMap<String, PloneField>	subFields;
	public HashMap<String, PloneTopic>	subTopics;
	public HashMap<String, PloneFile>		subFiles;

	protected URL xmlURL					= null;
	protected URL defaultViewURL	= null;

	public PloneFolder(String name, PloneFolder parent) {
		
		super(name, parent);
		
		//PloneWarsApp.out.println("new folder: " + name);

		if(parent != null)
			parent.subFolders.put(name, this);

		subFolders = new HashMap<String, PloneFolder>();
		subPages = new HashMap<String, PlonePage>();
		subFields = new HashMap<String, PloneField>();
		subTopics = new HashMap<String, PloneTopic>();
		subFiles = new HashMap<String, PloneFile>();

		try {
			xmlURL = downloadURI.resolve("kupucollection.xml?resource_type=linkable").toURL();
			defaultViewURL = downloadURI.resolve("select_default_page").toURL();
		}
		catch(MalformedURLException e) {
			e.printStackTrace();
		}
	}
	
	protected boolean makeFile(File file) {
		return file.mkdir();
	}

	protected boolean plone4download() {
		
		plone4downloadDetails();

		try {
			URL url = downloadURI.resolve("tinymce-jsonlinkablefolderlisting").toURL();
			Map<String, String> dataMap = new HashMap<String, String>();
			dataMap.put("rooted", "False");
			dataMap.put("document_base_url", downloadURI.toString());
			HttpURLConnection connection = PloneWarsUtils.post(url, dataMap);
			if(connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
				PloneWarsApp.out.println("\tscanning contents of folder " + name);
				JSONParser parser = new JSONParser();
				JSONObject root = (JSONObject)parser.parse(new InputStreamReader(connection.getInputStream(), UTF8));
				JSONArray items = (JSONArray)root.get("items");
				Iterator<?> iterator = items.iterator();
				while(iterator.hasNext()) {
					JSONObject item = (JSONObject)iterator.next();
					String type = (String)item.get("normalized_type");
					String id = (String)item.get("id");
					String title = (String)item.get("title");
					String uid = (String)item.get("uid");
					
					
					Class<? extends PloneObject> ploneClass = PloneObject.getClassFromJSON(type);
					if(ploneClass != null) {
						try {
							PloneWarsApp.out.println("\tfound " + ploneClass + " " + ploneClass.getDeclaredField("TYPE").get(null));
							PloneObject ploneObject = ploneClass.getConstructor(PloneObject.constructorArgs).newInstance(id, this);
							ploneObject.attr("title", title);
						}
						catch(InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException | NoSuchFieldException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
			}
		}
		catch(IOException | ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		
		if (PloneWarsApp.defaultViews)
			getDefaultView();
		
		return true;

	}

	/*
	 * public void getUIDs()
	 * {
	 * try
	 * {
	 * URL url = PloneWarsApp.plone4URI.resolve("tinymce-jsonlinkablefolderlisting").toURL();
	 * Map<String, String> dataMap = new HashMap<String, String>();
	 * dataMap.put("rooted","False");
	 * dataMap.put("document_base_url", plone4URI.toString());
	 * HttpURLConnection connection = PloneWarsApp.post(url, dataMap);
	 * if(connection.getResponseCode() == HttpURLConnection.HTTP_OK)
	 * {
	 * JSONParser parser = new JSONParser();
	 * JSONObject root = (JSONObject)parser.parse(new InputStreamReader(connection.getInputStream()));
	 * JSONArray items = (JSONArray)root.get("items");
	 * Iterator<JSONObject> iterator = items.iterator();
	 * while(iterator.hasNext())
	 * {
	 * JSONObject item = iterator.next();
	 * String type = (String)item.get("normalized_type");
	 * String id = (String)item.get("id");
	 * String uid = (String)item.get("uid");
	 * PloneWarsApp.out.println("looking for " + id);
	 * if((Boolean)item.get("is_folderish"))
	 * {
	 * PloneWarsApp.out.println("sub folder: " + subFolders.get(id));
	 * }
	 * else if(type.equals("file"))
	 * {
	 * PloneWarsApp.out.println("file: " + subFiles.get(id));
	 * }
	 * else
	 * {
	 * PloneWarsApp.out.println("page: " + subPages.get(id));
	 * }
	 * 
	 * }
	 * 

	 * }
	 * else
	 * {
	 * PloneWarsApp.out.println("Unexpected response while getting UIDs: " + connection.getResponseCode() + " " + connection.getResponseMessage());
	 * }
	 * 
	 * 
	 * }
	 * catch(IOException | ParseException e)
	 * {
	 * e.printStackTrace();
	 * }
	 * }
	 */

	public static PloneFolder getByPath(String path) {
		PloneFolder folder = null;
		if(path.indexOf('/' + PloneWarsApp.rootPloneFolder.name) == 0) { //see if path is a part of this site
			folder = PloneWarsApp.rootPloneFolder;
			if(path.length() > folder.name.length() + 1) {
				String testPath = path.substring(PloneWarsApp.rootPloneFolder.name.length() + 2); //remove the root
				for(String folderName : testPath.split("/")) { // loop over folder names
					if(folder == null)
						break;
					folder = folder.subFolders.get(folderName);
				}
			}
		}
		return folder;
	}

	public boolean build() {
		subFolders.clear();
		subPages.clear();
		class ErrFilter implements FilenameFilter {
			public boolean accept(File dir, String name) {
				return !name.endsWith(".err");
			}
		}

		File[] subFiles;
		Class<?>[] constructorArgs = new Class<?>[] { String.class, PloneFolder.class };
		if(file.isDirectory() && file.canRead() && (subFiles = file.listFiles(new ErrFilter())) != null) {
			for(File subFile : subFiles) {
				Class<? extends PloneObject> ploneClass = PloneObject.getClassFor(subFile);

				if(ploneClass != null) {
					if(subFile.isDirectory() && PloneFolder.class.isAssignableFrom(ploneClass)) {
						PloneFolder subFolder = new PloneFolder(subFile.getName(), this);
						PloneWarsApp.out.println("\tfound Folder " + subFile.getName());
						subFolder.build();
					}
					else {
						/* */
						try {
							PloneObject ploneObject = ploneClass.getConstructor(constructorArgs).newInstance(subFile.getName(), this);
							PloneWarsApp.out.println("\tfound " + ploneObject.getType() + " " + subFile.getName());
						}
						catch(InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
							e.printStackTrace();
						}
					}
					/* */
				}
				else {
					System.out.println("\tunknown file " + subFile.getPath());
					BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));
					System.out.println("What is this file? Folder, Page, Event, News, File, or Image?");
					PloneObject ploneObject = null;
					try {
						switch(consoleReader.readLine().toLowerCase()) {
							case "folder":
								ploneObject = new PloneFolder(subFile.getName(), this);
								break;
							case "page":
								ploneObject = new PlonePage(subFile.getName(), this);
								break;
							case "event":
								ploneObject = new PloneEvent(subFile.getName(), this);
								break;
							case "news":
								ploneObject = new PloneNews(subFile.getName(), this);
								break;
							case "file":
								ploneObject = new PloneFile(subFile.getName(), this);
								break;
							case "image":
								ploneObject = new PloneImage(subFile.getName(), this);
								break;
							default:
								System.out.println("I don't recognize that.");
						}

						if(ploneObject != null) {
							System.out.println("give this object a title");
							ploneObject.attr("title", consoleReader.readLine());

							System.out.println("give this object a description");
							ploneObject.attr("description", consoleReader.readLine());

							System.out.println("give this object a stage: Private, Published, Public Draft, or Login Required");
							ploneObject.attr("stage", consoleReader.readLine());

							ploneObject.save();
						}
					}
					catch(IOException e) {
						e.printStackTrace();
					}

				}
			}
			return true;
		}
		else {
			return false;
		}
	}

	// TODO: should not use attr because defaultView is not set through the edit page.
	// TODO: use getDefaultPage url
	public String getDefaultView() {
		PloneWarsApp.out.println("\tChecking Default View");
		if (attr("defaultView") == null) {
			attr("defaultView", PloneWarsUtils.ploneUriToString(downloadURI.resolve("getDefaultPage")));
		}
		else {
			PloneWarsApp.out.println("\tDefault view already set as "+ attr("defaultView"));
		}
		return attr("defaultView");
	}

	public boolean setDefaultView() {
		PloneWarsApp.out.println("Set default view");
		boolean success = false;

		if (attr("defaultView") == null) {
			PloneWarsApp.out.println("\tno default view");
		}
		else {
			try {
				URL selectDefaultViewURL = uploadURI.resolve("select_default_page").toURL();

				Map<String, String> dataMap = new HashMap<String, String>();
				dataMap.put("objectId", attr("defaultView"));
				dataMap.put("form.submitted", "1");

				HttpURLConnection connection = PloneWarsUtils.post(selectDefaultViewURL, dataMap);
				if (connection.getResponseCode() == HttpURLConnection.HTTP_MOVED_TEMP) {
					PloneWarsApp.out.println("\tdefault view set to " + attr("defaultView"));
					success = true;
				}
				else {
					PloneWarsApp.out.println("\tERROR: Could not set default view, unexpected response: " + connection.getResponseCode() + " " + connection.getResponseMessage());
					return false;
				}
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}

		return success;
	}

	// because classes can't be overridden
	protected HTMLEditorKit.ParserCallback getDefaultViewCallBack() {
		return new DefaultViewCallBack();
	}

	protected class DefaultViewCallBack extends HTMLEditorKit.ParserCallback {
		String defaultView = null;
		public void handleSimpleTag(HTML.Tag tag, MutableAttributeSet attr, int pos) {
			if (	defaultView == null &&
					tag.equals(HTML.Tag.INPUT) &&
					attr.getAttribute(HTML.Attribute.TYPE).equals("radio") &&
					attr.getAttribute(HTML.Attribute.CHECKED) != null
			) {
				defaultView = (String)attr.getAttribute(HTML.Attribute.VALUE);
				attr("defaultView", defaultView);
			}
		}
	}
	
	public boolean download() {
		super.download();
		if(PloneWarsApp.downloadVersion > 3)
			return plone4download();
		else
			return plone3download();
	}

	protected boolean plone3download() {
		try {
			InputSource is = new InputSource(new InputStreamReader(new DataInputStream(xmlURL.openConnection().getInputStream()), UTF8));
			// is.setEncoding("UTF-8");
			Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(is);

			Element root = doc.getDocumentElement();
			root.normalize();

			//String path = root.getElementsByTagName("uri").item(0).getChildNodes().item(0).getNodeValue();
			//name = path.substring(path.lastIndexOf('/') + 1, path.length());
			//root.getElementsByTagName("title").item(0).getChildNodes().item(0).getNodeValue();
			attr("title", getAttrValue(root, "title"));
			attr("description", getAttrValue(root, "description"));
			attr("stage", getAttrValue(root, "status"));

			NodeList entries = root.getElementsByTagName("resource");// .item(0).getChildNodes();
			for(int i = 0; i < entries.getLength(); i++) {
				Element entry = (Element)entries.item(i);
				
				Class<? extends PloneObject> ploneClass = PloneObject.getClassFromEntry(entry);

				if (ploneClass != null) {
					try {
						PloneObject ploneObject = ploneClass.getConstructor(PloneObject.constructorArgs).newInstance(getNameFromURI(getAttrValue(entry, "uri")), this);
						if(ploneObject instanceof PloneFolder) { // folder
							// in plone 3, we can get the folder details from the folder's download call
						}
						else {
							ploneObject.attr("title", getAttrValue(entry, "title"));
							ploneObject.attr("description", getAttrValue(entry, "description"));
							ploneObject.attr("stage", getAttrValue(entry, "status"));
							//ploneObject.title = getAttrValue(entry, "title");
							//ploneObject.description = ;
							//ploneObject.stage = ;
						}
					}
					catch(InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
		catch (IOException | SAXException | ParserConfigurationException e) {
			return false;
		}

		if (PloneWarsApp.defaultViews)
			getDefaultView();

		return true;
	}

	protected static String getAttrValue(Element elm, String attr) {
		String value;
		try {
			value = elm.getElementsByTagName(attr).item(0).getChildNodes().item(0).getNodeValue();
		}
		catch(NullPointerException e) {
			value = "";
		}
		return value;
	}

	protected static String getNameFromURI(String uri) {
		String name = uri.substring(uri.lastIndexOf('/') + 1);
		try {
			name = URLDecoder.decode(name, UTF8.name());
		}
		catch(UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return name;
	}

	protected String getId() {
		String superId = super.getId();
		int lastSlashPos = superId.lastIndexOf('/');
		return lastSlashPos >= 0 ? superId.substring(lastSlashPos + 1) : superId;
	}

	/*
	protected Map<String, String> getDataMap() {
		Map<String, String> dataMap = super.getDataMap();

		//if(excludeFromNav != null)
			//dataMap.put("excludeFromNav", excludeFromNav);

		return dataMap;
	}
	*/

	public boolean upload() {
		/* */
		PloneWarsApp.out.println("upload: " + uploadURI.getPath() + " ... ");

		try {
			URL editURL = getEditURL();
			if(editURL == null) {
				PloneWarsApp.out.println("\tERROR: could not get the edit URL.");
				return false;
			}

			//TODO: get special dataMap for site root?
			HttpURLConnection connection = PloneWarsUtils.post(editURL, getDataMap());
			if(connection == null) {
				PloneWarsApp.out.println("\tERROR: could not open connection to " + editURL);
				return false;
			}

			if(connection.getResponseCode() != HttpURLConnection.HTTP_MOVED_TEMP) {
				PloneWarsApp.out.println("\tERROR: Could not save folder, unexpected response: " + connection.getResponseCode() + " " + connection.getResponseMessage());
				return false;
			}
			else {
				PloneWarsApp.out.println("\tupload successful");
			}

			/* can't do this in Plone 4 until pages are uploaded
			if(PloneWarsApp.defaultViews)
				setDefaultView();
				*/

		}
		catch(IOException e) {
			e.printStackTrace();
			return false;
		}
		/* */
		return true;
	}

	/**
	 * does nothing
	 */
	public boolean save() {
		return super.save();
	}

	/*
	public String toString() {
		String returnS = "[PloneFolder " + super.toString();
		if(parent != null)
			returnS += "; parent: " + parent.name;
		returnS += " ]";
		return returnS;
	}
	*/
}
