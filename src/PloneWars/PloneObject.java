/**
 * 
 */
package PloneWars;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.attribute.UserDefinedFileAttributeView;
import java.text.*;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimeZone;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.DomSerializer;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * @author dadave02
 * 
 */
public abstract class PloneObject {
	public static final String TYPE = "error";
	public static final String EXT  = "";

	public static final int STAGE_UNKNOWN      = 0;
	public static final int STAGE_PRIVATE      = 1;
	public static final int STAGE_PUBLIC_DRAFT = 2;
	public static final int STAGE_PUBLISHED    = 3;
	
	public static final Class<?>[] constructorArgs = new Class<?>[] { String.class, PloneFolder.class };
	//public static final String[] externalEditKeys = new String[] { "title", "description", "subject", "effectiveDate", "expirationDate", "creation_date", "modification_date", "allowDiscussion", "excludeFromNav", "nextPreviousEnabled", "localCss", "localCssBlockParent" };
	
	protected File																							file;
	public PloneFolder																					parent;
	//protected URL  url;
	protected URI																								downloadURI, uploadURI, editURI;
	public String																								name;
	protected String contents;

	public static final Charset																	UTF8									= StandardCharsets.UTF_8; //Charset.forName("UTF-8");																//Charset.defaultCharset();
	protected static final Class<UserDefinedFileAttributeView>	UDC										= UserDefinedFileAttributeView.class;

	protected static final SimpleDateFormat											MODIFIED_DATE_FORMAT	= new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss 'GMT'");
	protected static final SimpleDateFormat											ICS_DATE_FORMAT				= new SimpleDateFormat("yyyyMMdd'T'HHmmssX");
	protected static final SimpleDateFormat											PLONE_DATE_S_FORMAT   = new SimpleDateFormat("yyyy'/'MM'/'dd HH:mm:ss.S z");
	protected static final SimpleDateFormat											PLONE_DATE_FORMAT     = new SimpleDateFormat("yyyy'/'MM'/'dd HH:mm:ss z");
	protected static final SimpleDateFormat											PLONE_DAY_FORMAT      = new SimpleDateFormat("yyyy'/'MM'/'dd");
	protected static final SimpleDateFormat											YEAR_FORMAT						= new SimpleDateFormat("yyyy");
	protected static final SimpleDateFormat											MONTH_FORMAT					= new SimpleDateFormat("MM");
	protected static final SimpleDateFormat											DAY_FORMAT						= new SimpleDateFormat("dd");
	protected static final SimpleDateFormat											HOUR_FORMAT						= new SimpleDateFormat("hh");
	protected static final SimpleDateFormat											MINUTE_FORMAT					= new SimpleDateFormat("mm");
	protected static final SimpleDateFormat											AMPM_FORMAT						= new SimpleDateFormat("a");
	
	protected static URI internalURI, publishExternallyURI, publishInternallyURI, publishRestrictedURI, privateURI, submitForReviewURI;
	static {
		try {
			internalURI = new URI("content_status_modify?workflow_action=show_internally");
			publishExternallyURI = new URI("content_status_modify?workflow_action=publish_externally");
			publishInternallyURI = new URI("content_status_modify?workflow_action=publish_internally");
			publishRestrictedURI = new URI("content_status_modify?workflow_action=publish_restricted");
			privateURI = new URI("content_status_modify?workflow_action=hide");
			submitForReviewURI = new URI("content_status_modify?workflow_action=submit");
		}
		catch(URISyntaxException e) {
			e.printStackTrace();
		}
		MODIFIED_DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT"));
	}

	protected UserDefinedFileAttributeView view = null;
	protected Map<String, String> data;

	protected boolean																						createdNew;
	public boolean																							loaded;

	public PloneObject(String name, PloneFolder parent) {
		if(name == null || name == "")
			System.err.println("PloneObject has to have a name");
		
		this.name = name;
		this.parent = parent;

		if(parent != null) {
			file = new File(parent.file, fileName());
			URI relativeURI;
			try {
				relativeURI = new URI(null, null, name + '/', null, null);
				downloadURI = parent.downloadURI.resolve(relativeURI);
				uploadURI = parent.uploadURI.resolve(relativeURI);
			}
			catch(URISyntaxException e) {
				e.printStackTrace();
			}
		}
		else { // this is the root
			//System.out.println("This is the root"); // TODO: overwrite base folder with base URI?
			file = PloneWarsApp.rootFolder;
			downloadURI = PloneWarsApp.downloadRootURI;
			uploadURI = PloneWarsApp.uploadRootURI;
		}
		
		try {
			editURI =  new URI("atct_edit");
		}
		catch(URISyntaxException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		makeFile(file);
		view = getViewFor(file);

		// create data map and load it
		data = new HashMap<String, String>();
		
		try {
			if(view != null) {
				String encodedName = URLEncoder.encode("plone.", UTF8.name());
				for(String key : view.list()) {
					// look for tags that start with plone.
					if(key.startsWith(encodedName)) {
						ByteBuffer attrBuffer = ByteBuffer.allocate(view.size(key));
						view.read(key, attrBuffer);
						attrBuffer.flip();
						String value = UTF8.decode(attrBuffer).toString();
						key = URLDecoder.decode(key.substring(encodedName.length()), UTF8.name());
						data.put(key, value);
					}
				}
			}
		}
		catch(IOException e) {
			e.printStackTrace();
		}
		// TODO: don't use atr for this
		//attr("type", TYPE);
		//tag("plonewars.type", getType());

		createdNew = false;
		loaded = false;
	}
	
	public String getExt() {
		String ext = EXT;
		try {
			Class<? extends PloneObject> thisClass = this.getClass();
			Field extField = thisClass.getField("EXT");
			ext = (String)extField.get(null);
		}
		catch(IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
			e.printStackTrace();
		}
		return ext;
	}
	
	protected String fileName() {
		if (PloneWarsApp.addPageExt && getExt().length() > 0 && !name.endsWith(getExt())) {
			return name + getExt();
		}
		else
			return name;
	}
	
	protected boolean makeFile(File file) {
		try {
			return file.createNewFile();
		}
		catch(IOException e) {
			e.printStackTrace();
		}
		return false;
	}
	
	public static Date ploneDateStringToDate(String ploneDateString, Date defaultDate) {
		Date date;
		try {
			date = PLONE_DATE_S_FORMAT.parse(ploneDateString + ":00"); // TODO: this may only have the year
		}
		catch(java.text.ParseException noMilliException) {
			try {
				date = PLONE_DATE_FORMAT.parse(ploneDateString + ":00"); // No milliseconds
			}
			catch(java.text.ParseException noTimeException) {
				try {
					date = PLONE_DAY_FORMAT.parse(ploneDateString); // No hour/minute/second
				}
				catch(java.text.ParseException e) {
					PloneWarsApp.out.println("\tcould not parse " + ploneDateString + " as a date, using defaut " + defaultDate.toString());
					date = defaultDate;
				}
			}
		}
		return date;
	}

	
	public static Class<? extends PloneObject> getClassFromEntry(Element entry) {
		Class<? extends PloneObject> ploneClass = null;
		
		String icon = entry.getElementsByTagName("icon").item(0).getChildNodes().item(0).getNodeValue();
		icon = icon.substring(icon.lastIndexOf('/') + 1, icon.lastIndexOf('.'));
		
		if(icon.equals("folder_icon")) { 
			if(entry.getElementsByTagName("label").getLength() > 0 && entry.getElementsByTagName("label").item(0).getChildNodes().item(0).getNodeValue().equals(".. (Parent folder)")) {
				// ignore this one
			}
			else {
				ploneClass =  PloneFolder.class;
			}
		}
		else if(icon.equals("document_icon") && entry.getElementsByTagName("anchor").getLength() > 0) { //document with anchor is an HTML page
			ploneClass = PlonePage.class;
		}
		else if(icon.equals("document_icon") && entry.getElementsByTagName("preview").getLength() > 0) { //a homepage document
			ploneClass = PlonePage.class;
		}
		else if(icon.equals("file_icon") || icon.equals("document_icon")) { //document without anchor or preview is a file
			ploneClass = PloneFile.class;
		}
		else if(icon.equals("image_icon")) {
			ploneClass = PloneImage.class;
		}
		else if(icon.equals("event_icon")) {
			ploneClass = PloneEvent.class;
		}
		else if(icon.equals("newsitem_icon")) {
			ploneClass = PloneNews.class;
		}
		else if(icon.equals("topic_icon")) {
			ploneClass = PloneTopic.class;
		}
		// Form stuff
		else if(icon.equals("Form")) {
			ploneClass =  PloneForm.class;
		}
		else if(icon.equals("CheckBoxField")) {
			ploneClass = PloneBooleanField.class;
		}
		else if(icon.equals("scriptaction")) {
			PloneWarsApp.out.println("scriptaction not supported");
		}
		else if(icon.equals("DateTimeField")) {
			ploneClass = PloneDateField.class;
		}
		else if(icon.equals("Fieldset")) {
			ploneClass = PloneFieldset.class;
		}
		else if(icon.equals("FileField")) {
			ploneClass = PloneFileField.class;
		}
		else if(icon.equals("FloatField")) {
			PloneWarsApp.out.println("FloatField not supported");
		}
		else if(icon.equals("IntegerField")) {
			ploneClass =  PloneIntegerField.class;
		}
		else if(icon.equals("LabelField")) {
			ploneClass = PloneLabelField.class;
		}
		else if(icon.equals("LinesField")) {
			ploneClass = PloneLinesField.class;
		}
		else if(icon.equals("mailaction")) {
			ploneClass = PloneMailerAdapter.class;
		}
		else if(icon.equals("MultipleListField")) {
			ploneClass = PloneMultiSelectionField.class;
		}
		else if(icon.equals("PasswordField")) {
			PloneWarsApp.out.println("PasswordField not supported");
		}
		else if(icon.equals("LikertField")) { //rating-scale
			ploneClass = PloneLikertField.class;
		}
		else if(icon.equals("RichLabelField")) {
			PloneWarsApp.out.println("RichLabelField not supported");
		}
		else if(icon.equals("RichTextField")) {
			ploneClass = PloneRichTextField.class;
		}
		else if(icon.equals("FormAction")) {
			ploneClass = PloneSaveDataAdapter.class;
		}
		else if(icon.equals("ListField")) {
			ploneClass = PloneSelectionField.class;
		}
		else if(icon.equals("StringField")) {
			ploneClass = PloneStringField.class;
		}
		else if(icon.equals("TextAreaField")) {
			ploneClass = PloneTextareaField.class;
		}
		else if(icon.equals("ThanksPage")) {
			ploneClass = PloneThanksPage.class; // [uri]/thanksEpilogue
		}
		else if(icon.equals("icon_directory")) {
			ploneClass = PloneDirectory.class;
		}
		else if(icon.equals("icon_department")) {
			ploneClass = PloneDepartment.class;
		}
		else if(icon.equals("icon_person")) {
			System.out.println("this is a person");
			ploneClass = PlonePerson.class;
		}
		return ploneClass;
	}
	
	public static Class<? extends PloneObject> getClassFromJSON(String type) {
		Class<? extends PloneObject> ploneClass = null;
		
		switch(type) {
			case "folder" :
				ploneClass = PloneFolder.class;
				break;
			case "document" :
				ploneClass = PlonePage.class;
				break;
			case "file" :
				ploneClass = PloneFile.class;
				break;
			case "image" :
				ploneClass = PloneImage.class;
				break;
			case "event" :
				ploneClass = PloneEvent.class;
				break;
			case "news-item" :
				ploneClass = PloneNews.class;
				break;
			case "topic" :
				ploneClass = PloneTopic.class;
				break;
			case "formfolder" :
				ploneClass = PloneForm.class;
				break;
			default:
				System.err.println("unknown type in JSON file: " + type);
		}
		
		return ploneClass;
	}

	public static Class<? extends PloneObject> getClassFor(File file) {
		Class<? extends PloneObject> ploneClass = null;
		UserDefinedFileAttributeView view = getViewFor(file);
		try {
			if(file != null && view != null) {//file.exists() && Files.getFileStore(file.toPath()).supportsFileAttributeView(UDC))

				String encodedName = URLEncoder.encode("plonewars.type", UTF8.name());
				System.out.println("file: " + file.getPath());
				System.out.println(view.list());
				//UserDefinedFileAttributeView view = Files.getFileAttributeView(file.toPath(), UDC);
				//PloneWarsApp.out.println(view.list());
				if(view.list().contains(encodedName)) {
					ByteBuffer attrBuffer = ByteBuffer.allocate(view.size("plonewars.type"));
					view.read(encodedName, attrBuffer);
					attrBuffer.flip();
					String type = UTF8.decode(attrBuffer).toString();
					//PloneWarsApp.out.println(type);
					switch(type) {
						case PloneFolder.TYPE:
							ploneClass = PloneFolder.class;
							break;
						case PlonePage.TYPE:
							ploneClass = PlonePage.class;
							break;
						case PloneEvent.TYPE:
							ploneClass = PloneEvent.class;
							break;
						case PloneNews.TYPE:
							ploneClass = PloneNews.class;
							break;
						case PloneFile.TYPE:
							ploneClass = PloneFile.class;
							break;
						case PloneImage.TYPE:
							ploneClass = PloneImage.class;
							break;
						case PloneForm.TYPE:
							ploneClass = PloneForm.class;
							break;
						case PloneBooleanField.TYPE:
							ploneClass = PloneBooleanField.class;
							break;
						//scriptaction
						case PloneDateField.TYPE:
							ploneClass = PloneDateField.class;
							break;
						case PloneFieldset.TYPE:
							ploneClass = PloneFieldset.class;
							break;
						case PloneFileField.TYPE:
							ploneClass = PloneFileField.class;
							break;
						//FloatField
						case PloneIntegerField.TYPE:
							ploneClass = PloneIntegerField.class;
							break;
						//LabelField
						case PloneLinesField.TYPE:
							ploneClass = PloneLinesField.class;
							break;
						case PloneMailerAdapter.TYPE:
							ploneClass = PloneMailerAdapter.class;
							break;
						case PloneMultiSelectionField.TYPE:
							ploneClass = PloneMultiSelectionField.class;
							break;
						//PasswordField
						case PloneLikertField.TYPE:
							ploneClass = PloneLikertField.class;
							break;
						//RichLabelField
						case PloneRichTextField.TYPE:
							ploneClass = PloneRichTextField.class;
							break;
						case PloneSaveDataAdapter.TYPE:
							ploneClass = PloneSaveDataAdapter.class;
							break;
						case PloneSelectionField.TYPE:
							ploneClass = PloneSelectionField.class;
							break;
						case PloneStringField.TYPE:
							ploneClass = PloneStringField.class;
							break;
						case PloneTextareaField.TYPE:
							ploneClass = PloneTextareaField.class;
							break;
						case PloneThanksPage.TYPE:
							ploneClass = PloneThanksPage.class;
							break;
						case PloneTopic.TYPE:
							ploneClass = PloneTopic.class;
							break; /*
						case PloneDirectory.TYPE:
							ploneClass = PloneDirectory.class;
							break;
						case PloneDepartment.TYPE:
							ploneClass = PloneDepartment.class;
							break;
						case PlonePerson.TYPE:
							ploneClass = PlonePerson.class;
							break; */
						default:
							PloneWarsApp.out.println("how did you get this " + type);
					}
				}
				else {
					System.out.println("non-Plone file: " + file.getPath());
				}
			}

		}
		catch(IOException e) {
			e.printStackTrace();
			ploneClass = null;
		}
		return ploneClass;
	}

	protected static UserDefinedFileAttributeView getViewFor(File file) {
		UserDefinedFileAttributeView view = null;
		try {
			if(file.exists() && Files.getFileStore(file.toPath()).supportsFileAttributeView(UDC)) {
				view = Files.getFileAttributeView(file.toPath(), UDC);
			}
		}
		catch(IOException e) {
			e.printStackTrace();
			view = null;
		}
		return view;
	}

	protected Map<String, String> getDataMap() {
		Map<String, String> dataMap = new HashMap<String, String>();
		dataMap.putAll(data); // getting real id, plone.id and plone.plone.id, also no id
		dataMap.put("id", name);
		dataMap.put("form.submitted", "1");
		
		// me!
		//dataMap.put("creators",  "1760332");

		return dataMap;
	}

	protected Map<String, File> getFileMap() {
		Map<String, File> fileMap = new HashMap<String, File>();
		return fileMap;
	}

	private boolean downloadExcludeFromNav() {
		boolean success = false;

		PloneWarsApp.out.println("\tchecking Exclude From Navigation.");
		try {
			URL excludeFromNavURL = downloadURI.resolve("exclude_from_nav").toURL();
			HttpURLConnection connection = (HttpURLConnection)excludeFromNavURL.openConnection();
			connection.setInstanceFollowRedirects(true); // does not work from http to https

			boolean tryAgain = true;
			while(tryAgain) {
				tryAgain = false;

				if(connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
					attr("excludeFromNav", "checked");
					PloneWarsApp.out.println("\tExclude From Navigation: true");
					success = true;
				}
				else if(connection.getResponseCode() == HttpURLConnection.HTTP_NO_CONTENT) {
					attr("excludeFromNav",  "");
					PloneWarsApp.out.println("\tExclude From Navigation: false");
					success = true;
				} /*
					 * else if(connection.getResponseCode() == HttpURLConnection.HTTP_MOVED_TEMP)
					 * {
					 * PloneWarsApp.out.println("\tExclude From Navigation: Unable to check. This URL appears to require SSL connections.");
					 * }
					 */
				else if(connection.getResponseCode() >= HttpURLConnection.HTTP_MULT_CHOICE && connection.getResponseCode() <= HttpURLConnection.HTTP_SEE_OTHER) {
					connection = (HttpURLConnection)new URL(connection.getHeaderField("Location")).openConnection(); // for shift to https
					tryAgain = true;
				}
				else {
					PloneWarsApp.out.println("\tExclude From Navigation: Unexpected response from server: " + connection.getResponseCode() + " " + connection.getResponseMessage());
				}
			}
		}
		catch(IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return success;
	}

	public boolean download() {
		boolean success = false;

		PloneWarsApp.out.println("download: " + downloadURI.getPath());
		if(PloneWarsApp.excludeFromNavs)
			success = downloadExcludeFromNav();
		
		if(PloneWarsApp.downloadVersion >= 4) {
			plone4downloadStage();
		}

		return success;
	}
	
	protected boolean plone4downloadStage() {
		boolean success = false;
		try {
			URL xmlURL = downloadURI.resolve("content_status_history").toURL();
			CleanerProperties cp = new CleanerProperties();
			HtmlCleaner cleaner = new HtmlCleaner(cp);
			TagNode page = cleaner.clean(xmlURL.openStream(), UTF8.name());

			DomSerializer ds = new DomSerializer(cp);
			try {
				Document doc = ds.createDOM(page);
				XPath xpath = XPathFactory.newInstance().newXPath();
				try {
					attr("stage", (String)xpath.evaluate("//table[@id='sortable']//td[last()]/span/@class", doc, XPathConstants.STRING));
					success = true;
				}
				catch(XPathExpressionException e) {
					e.printStackTrace();
				}
			}
			catch(ParserConfigurationException e) {
				e.printStackTrace();
			}
		}
		catch(IOException e) {
			e.printStackTrace();
		}
		
		
		return success;
	}
	
	protected boolean plone4downloadDetails() {
		boolean success = false;
		try {
			URL detailsURL = downloadURI.resolve("tinymce-jsondetails").toURL();
			HttpURLConnection connection = (HttpURLConnection)detailsURL.openConnection();
			if(connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
				JSONParser parser = new JSONParser();
				JSONObject root = (JSONObject)parser.parse(new InputStreamReader(connection.getInputStream(), UTF8));

				attr("title", (String)root.get("title"));
				attr("description", (String)root.get("description"));
				success = true;
			}
		}
		catch(IOException | org.json.simple.parser.ParseException e) {
			e.printStackTrace();
		}
		return success;
	}

	public boolean save() {
		boolean success = false;

		PloneWarsApp.out.println("\tsave:");

		for(Entry<String, String> i : data.entrySet()) {
			String key   = i.getKey();
			String value = i.getValue();
			tag("plone." + key, value);
			PloneWarsApp.out.println("\t\t" + key + ": \t" + value + ";");
		}
		tag("plonewars.type", getType());
		success = true;
	
		return success;
	}

	public boolean publish() {
		boolean success = false;
		try {
			HttpURLConnection connection = null;
			String stage = attr("stage").toLowerCase();
			if(stage != null) {
				switch(stage) {
				//publishInternallyURI;
				//submitForReviewURI;
					case "private":
					case "state-private": // from Plone 4 content_status_history page
						connection = (HttpURLConnection)uploadURI.resolve(privateURI).toURL().openConnection();
						PloneWarsApp.out.println("\tmaking Private");
						break;
					case "state-internal": //  from Plone 4 content_status_history page
						connection = (HttpURLConnection)uploadURI.resolve(internalURI).toURL().openConnection();
						PloneWarsApp.out.println("\thiding as Internal");
						break;
					case "state-internally_published": //  from Plone 4 content_status_history page
						connection = (HttpURLConnection)uploadURI.resolve(publishInternallyURI).toURL().openConnection();
						PloneWarsApp.out.println("\tpublishing as Internal");
						break;
					case "published":
					case "public draft":
					case "state-external": // from Plone 4 content_status_history page
						connection = (HttpURLConnection)uploadURI.resolve(publishExternallyURI).toURL().openConnection();
						PloneWarsApp.out.println("\tpublishing as External");
						break;
					case "login required":
					case "state-internally_restricted": // from Plone 4 content_status_history page
						connection = (HttpURLConnection)uploadURI.resolve(publishRestrictedURI).toURL().openConnection();
						PloneWarsApp.out.println("\tpublishing as Restricted");
						break;
					default:
						PloneWarsApp.out.println("\tError: no stage found.");
				}
			}

			if(connection == null) {
				PloneWarsApp.out.println("\tunknown stage: " + stage + "; could not publish");
			}
			else {
				connection.setInstanceFollowRedirects(false);
				if(connection.getResponseCode() == HttpURLConnection.HTTP_MOVED_TEMP) {
					success = true;
					PloneWarsApp.out.println("\tpublished succesfully");
				}
				else {
					PloneWarsApp.out.println("\tUnexpected response from server while trying to publish to " + connection.getURL() + ": " + connection.getResponseCode() + ' ' + connection.getResponseMessage());
				}
			}
		}
		catch(IOException e) {
			e.printStackTrace();
		}
		return success;
	}

	public abstract boolean upload();
	
	/**
	 * used for saving anything
	 * @param name
	 * @return
	 */
	protected String tag(String name) {
		String tag = null;
		
		// fall back to name if encoding fails
		String encodedName = name;
		try {
			encodedName = URLEncoder.encode(name, UTF8.name());
		}
		catch(UnsupportedEncodingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		try {
			if(view.list().contains(encodedName)) {
				ByteBuffer attrBuffer = ByteBuffer.allocate(view.size(encodedName));
				view.read(encodedName, attrBuffer);
				attrBuffer.flip();
				tag = UTF8.decode(attrBuffer).toString();
			}
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		return tag;
	}
	
	protected boolean tag(String name, String value) {
		boolean success = false;
		
		// fall back to name if encoding fails
		String encodedName = name;
		try {
			encodedName = URLEncoder.encode(name, UTF8.name());
		}
		catch(UnsupportedEncodingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		if(value == null)
			value = "";
		
		try {
			
			view.write(encodedName, UTF8.encode(value));
			success = true;
		}
		catch(Exception e) {
			System.err.println(name);
			System.err.println(value);
			System.err.println(view);
			System.err.println(file);
			//e.printStackTrace();
		}
		return success;
	}
	
	public boolean tag(String name, File file) {
		boolean success = false;
		if(file != null && file.exists()) {

			// fall back to name if encoding fails
			String encodedName = name;
			try {
				encodedName = URLEncoder.encode(name, UTF8.name());
			}
			catch(UnsupportedEncodingException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
			ByteBuffer buffer = ByteBuffer.allocate((int)file.length());
			try {
				Channels.newChannel(new FileInputStream(file)).read(buffer);
				view.write(encodedName, buffer);
				success = true;
			}
			catch(IOException e) {
				e.printStackTrace();
				success = false;
			}
		}
		return success;
	}

	/**
	 * Used for saving plone data to send in form
	 * @param name
	 * @return
	 */
	public String attr(String name) {
		String attr = data.get(name);
		
		if (attr == null) {
			attr = tag("plone." + name);
		}
		return attr;
	}
	
	public String attr(String name, String value) {
		if (value != null)
			data.put(name, value);
		else
			data.remove(name);
		return value;
	}
	
	public File attr(String name, File file) {
		
		return file;
	}

	/*
	public String getPath() {
		String path = "";
		if(parent != null)
			path = parent.getPath();
		try {
			path += '/' + URLEncoder.encode(name, UTF8.name());
		}
		catch(UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return path;
	}
	*/

	protected String getId() {
		return name;
	}

	public String getType() {
		String type = null;
		try {
			type = (String)this.getClass().getField("TYPE").get(null);
		}
		catch(IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
			e.printStackTrace();
		}
		return type;
	}

	protected URL getNewEditURL() {
		URL newEditURL = null;
		URL createURL;

		createdNew = true;

		try {
			String type = getType();
			//createURL = new URI("http", "stage.louisville.edu", parent.getPath() + "/createObject", "type_name=" + type, null).toURL();
			createURL = uploadURI.resolve(new URI(null, null, "../createObject", "type_name=" + type, null)).toURL();
			HttpURLConnection connection = (HttpURLConnection)createURL.openConnection();
			connection.setInstanceFollowRedirects(false);

			if(connection.getResponseCode() != HttpURLConnection.HTTP_MOVED_TEMP) {
				PloneWarsApp.out.print("Unexpected response while trying to create new " + type + " " + connection.getURL() + ": " + connection.getResponseCode() + " " + connection.getResponseMessage());
			}
			else {
				URI location = new URI(connection.getHeaderField("Location").replace(" ", "%20"));
				
				if(!location.getPath().endsWith("/edit")) { //should end in /edit
					PloneWarsApp.out.print("Could not create new " + type + ", bad location: " + location);
				}
				else {
					newEditURL = location.resolve("atct_edit").toURL();
				}
			}
		}
		catch(IOException | URISyntaxException e) {
			e.printStackTrace();
		}
		return newEditURL;
	}
	
	private boolean isRoot() {
		return PloneWarsApp.uploadRootURI.equals(uploadURI);
	}

	protected URL getEditURL() {
		URL editURL = null;
		try {
			// test if site root
			if(isRoot()) {
				System.out.println("this is the root");
				//http://stage.louisville.edu/artsandsciences/advising/@@site-controlpanel
				//form.site_title
				//form.site_description
				editURL = uploadURI.resolve("@@site-controlpanel").toURL();
				//PloneWarsApp.out.println("\tupdating");
			}
			else {
				byte tries = 4;
				while(editURL == null && tries --> 0) {
					//check the "external_edit" link because it's faster, and we can see if plone is lying
					URL testURL = uploadURI.resolve("external_edit").toURL();
					HttpURLConnection connection = (HttpURLConnection)testURL.openConnection();
					connection.setInstanceFollowRedirects(false); //so that we can tell 301 from 302
	
					if(connection.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND) {
						// if does not exist, create a new object
						PloneWarsApp.out.println("\tnot found, creating new");
						editURL = getNewEditURL();
					}
					// plone appears to keep a record of renames. this item has been renamed
					else if(connection.getResponseCode() == HttpURLConnection.HTTP_MOVED_PERM) {
						//TODO: have an option to update renames or leave them alone?
						editURL = new URI(connection.getHeaderField("location")).resolve(editURI).toURL();
						PloneWarsApp.out.print("\tWarning: wrong id, updating at " + editURL);
					}
					// plone claims that it already exists
					else if(connection.getResponseCode() == HttpURLConnection.HTTP_MOVED_TEMP) {
						//now test if Plone is lying: if there is another folder by the same name in another location, plone will use it
						connection.disconnect();
						connection = (HttpURLConnection)new URL(connection.getHeaderField("location")).openConnection();
						connection.setInstanceFollowRedirects(false);
						if(connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
							BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream(), UTF8));
							String inputLine;
							while((inputLine = in.readLine()) != null) {
								// this line will tell us what location plone is 
								if(inputLine.indexOf("url:") == 0) {
									URI actualURI = new URI(inputLine.substring(4) + '/');
									if(uploadURI.equals(actualURI)) { // everything checks out
										editURL = uploadURI.resolve(editURI).toURL();
										PloneWarsApp.out.println("\tupdating");
									}
									else { // plone is a dirty liar; this page doesn't really exist!
										editURL = getNewEditURL();
										PloneWarsApp.out.println("\tPlone lied, creating new");
									}
									break; //that's the only line we need
								}
							}
							if(editURL == null) {
								PloneWarsApp.out.println("Could not find URL in external_edit file.");
							}
							in.close();
						}
						else {
	
							PloneWarsApp.out.println("Unexpected response from server while trying to open " + connection.getURL() + " : " + connection.getResponseCode() + ' ' + connection.getResponseMessage());
						}
					}
					else {
						PloneWarsApp.out.println("Unexpected response from server while trying to get an edit URL: " + connection.getResponseCode() + ' ' + connection.getResponseMessage());
					}
				}
			}
		}
		catch(IOException | URISyntaxException e) {
			e.printStackTrace();
		}
		return editURL;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("{");
		for(Entry<String, String> e : data.entrySet())
			sb.append(e.getKey() + ": " + e.getValue() + "; ");
		sb.append("}");
		return TYPE + " " + sb.toString();
	}

}
