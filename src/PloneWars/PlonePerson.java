package PloneWars;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.Channels;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.Map.Entry;

import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;

// In Plone 4 it is actually a folder
public class PlonePerson extends PlonePage {
	
	private Map<String, String> unmapped;
	
	public static final String TYPE = "PDPerson";
	
	private URI		imageURI;
	private File	imageFile;
	
	boolean	inEditForm	= false;
	
	// create to ++add++faculty
	// first edit ++add++faculty
	// edit at @@edit
	
	// map to hold the plon3 name of fields and the plone 4 nearest equivalent
	static Map<String,String> map3to4 = new HashMap<String, String>();
	static {
		map3to4.put("title",           "form.widgets.title");
		map3to4.put("image_file",      "form.widgets.photo");
		// map3to4.put("orgRoles:list",   "form.widgets.position"); // need to join lines
		//map3to4.put("office",          "form.widgets.office_address");
		map3to4.put("phone",           "form.widgets.primary_phone_number");
		map3to4.put("fax",             "form.widgets.secondary_phone_number_fax_");
		map3to4.put("email",           "form.widgets.email_address");
		map3to4.put("website",         "form.widgets.wordpress_address");
		//map3to4.put("education:lines", "form.widgets.education");
		//map3to4.put("bio",             "form.widgets.about");
		//map3to4.put("appointments:lines", "form.widgets.position");
		
		// location:lines + ", " + office

	}
	
	static Map<String,String> about = new HashMap<String,String>();
	static {
		// about.put("bio",               "Biography");
		about.put("teaching:lines",    "Teaching Areas");
		about.put("research:lines",    "Research Areas and Projects");
		about.put("awards:lines",      "Honors and Awards");
		about.put("activities:lines",  "Activities");
		about.put("memberships:lines", "Professional Memberships");
	}
	
	public PlonePerson(String name, PloneFolder parent) {
		super(name, parent);
		
		// different
		try {
			editURI  = new URI("@@edit");
			imageURI = downloadURI.resolve("image");
		}
		catch(URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		unmapped = new HashMap<String, String>();
		
		// TODO: remove, don't use this anymore
		bodyStartIds.add("pd_person");
	}
	
	protected Map<String, String> getDataMap() {
		Map<String, String> dataMap = super.getDataMap();
		dataMap.put("form.widgets.IExcludeFromNavigation.exclude_from_nav-empty-marker", "1"); // required for some reason
		dataMap.remove("form.submitted"); // not used
		dataMap.remove("form.widgets.photo"); // this is a file
		if((createdNew || PloneWarsApp.forceUpload) && (imageFile != null && imageFile.exists() && imageFile.length() > 0))
			dataMap.put("form.widgets.photo.action", "replace");
		dataMap.put("form.buttons.save", "Save"); // Plone4/unique
		return dataMap;
	}
	
	protected Map<String, File> getFileMap() {
		System.out.println("Get file map!");
		Map<String, File> fileMap = super.getFileMap();

		if(createdNew || PloneWarsApp.forceUpload) {
			if(imageFile != null && imageFile.exists() && imageFile.length() > 0) {
				fileMap.put("form.widgets.photo", imageFile);
			}
		}

		return fileMap;
	}
	
	// override because Person is weird
	protected URL getNewEditURL() {
		URL newEditURL = null;
		//URL createURL;

		createdNew = true;

		try {
			newEditURL =  uploadURI.resolve(new URI(null, null, "../++add++faculty", null, null)).toURL();
		}
		catch(IOException | URISyntaxException e) {
			e.printStackTrace();
		}
		
		// make it here
		Map<String, String> dataMap = getDataMap();
		// only way to get this right
		dataMap.put("form.widgets.title", name); 
		HttpURLConnection connection = PloneWarsUtils.post(newEditURL, dataMap);
		connection.setInstanceFollowRedirects(false);
		
		// if it worked, now use the regular edit URL
		try {
			if(connection.getResponseCode() == HttpURLConnection.HTTP_MOVED_TEMP) {
				// regular editURL
				newEditURL = uploadURI.resolve(editURI).toURL();
			}
		}
		catch(IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// if it didn't work, just upload as is
		
		// now edit it
		return getEditURL();
	}
	
	/**
	 * overridden to map plone 3 names to 4
	 *
	public String attr(String name) {
		if(map3to4.containsKey(name)) {
			name = map3to4.get(name);
		}
		return super.attr(name);
	}
	
	/**
	 * overridden to map plone 3 names to 4
	 *
	public String attr(String name, String value) {
		if(map3to4.containsKey(name)) {
			name = map3to4.get(name);
		}
		else if(value != null && value.length() > 2) {
			unmapped.put(name, value);
		}
		return super.attr(name, value);
	}
	*/
	
	public boolean save() {
		
		Map<String,String> data = getDataMap();
		for(Entry<String,String> e : map3to4.entrySet()) {
			attr(e.getValue(), data.get(e.getKey()));
		}
		
		// make position from joined appointments
		String appointments = data.containsKey("appointments:lines") ? data.get("appointments:lines") : "";
		String position = appointments.replaceAll("\\s*\\n+\\s*", "; ");
		// this value is required
		// if all white space
		if(position.matches("^\\s*$")) {
			PloneWarsApp.out.println("\twarning: position has no value, using default");
			position = "unknown";
		}
		attr("form.widgets.position", position);
		
		// build new bio (aka about) from old bio
		String aboutValue = data.containsKey("bio") ? data.get("bio") : "";
		// plus everything else that doesn't have a place
		for(Entry<String,String> e : about.entrySet()) {
			String plone3value = data.get(e.getKey());
			if(plone3value != null && !plone3value.matches("^\\s*$")) {
				aboutValue += "<h2>" + e.getValue() + "</h2>\n";
				aboutValue += "<p>";
				aboutValue += plone3value.replaceAll("\\s*\n+\\s*", "<br />\n");
				aboutValue += "</p>\n";
			}
		}
		attr("form.widgets.about", aboutValue);
		
		String office  = data.containsKey("office")         ? data.get("office").trim()                                          : ""; // todo: could be null
		String address = data.containsKey("location:lines") ? data.get("location:lines").replaceAll("\\s*\\n+\\s*", ", ").trim() : ""; // todo: could be null
		String officeAddress = office;
		if(officeAddress.isEmpty()) {
			officeAddress = address;
		}
		else {
			officeAddress += ", " + address;
		}
		attr("form.widgets.office_address",  officeAddress);
		
		String education = data.containsKey("education:lines") ? data.get("education:lines").replaceAll("\\s*\\n+\\s*", "<br />").trim() : "";
		attr("form.widgets.education", education);
		
		// save image
		tag("plone.image", imageFile);
		/*
		System.out.println(name + " unmapped:");
		for(Entry<String, String> i : unmapped.entrySet()) {
			String key   = i.getKey();
			String value = i.getValue();
			System.out.println("\t\t" + key + ": \t" + value + ";");
		}
		*/
		return super.save();
	}
	
	// TODO: this code is plone3 only
	// TODO: download image from /image
	public boolean download() {
		// right now I'm not going to download anything

		URL editURL = null;
		try {
			editURL = downloadURI.resolve(new URI(null, null, "edit", null)).toURL();
		}
		catch(URISyntaxException | MalformedURLException e) {
			e.printStackTrace();
			return false;
		}
		
		// download image
		try {
			PloneWarsApp.out.println("\tdownload image: " + imageURI.getPath() + " ... ");
			HttpURLConnection connection = (HttpURLConnection)imageURI.toURL().openConnection();
			connection.setInstanceFollowRedirects(true);
			if(file.exists() && !PloneWarsApp.forceDownload) {
				connection.setRequestProperty("If-Modified-Since", MODIFIED_DATE_FORMAT.format(new Date(file.lastModified())));
			}

			if(connection.getResponseCode() == HttpURLConnection.HTTP_NOT_MODIFIED) {
				PloneWarsApp.out.println("\timage up to date, skipping");
			}
			else if(connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
				PloneWarsApp.out.println("\tdownloading ... ");
				imageFile = File.createTempFile("temp-" + name, '.' + connection.getHeaderField("Content-Type").split("/")[1]);
				PloneWarsUtils.streamCopy(connection.getInputStream(), new FileOutputStream(imageFile), true);
			}
			else if(connection.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND) {
				PloneWarsApp.out.println("\tno image found");
			}
			else {
				PloneWarsApp.out.println("\terror: " + connection.getResponseCode() + " " + connection.getResponseMessage());
			}
		}
		catch(IOException e) {
			e.printStackTrace();
		}
		
		/*
		URL imageURL = null;
		try {
			imageURL = downloadURI.resolve(new URI(null, null, "image", null)).toURL();
		}
		catch(URISyntaxException | MalformedURLException e) {
			e.printStackTrace();
			return false;
		}
		*/

		ParserDelegator delegator = new ParserDelegator();
		try {
			delegator.parse(new InputStreamReader(editURL.openConnection().getInputStream(), UTF8), getFormEditParserCallBack(), true);
		}
		catch(IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// shows if our parser found the form
		return inEditForm;
	}

	protected HTMLEditorKit.ParserCallback getFormEditParserCallBack() {
		return new FormEditParserCallBack();
	}

	protected class FormEditParserCallBack extends HTMLEditorKit.ParserCallback {

		String	selectName	= null, textareaName = null;
		
		protected Stack<MutableAttributeSet> attrs = new Stack<MutableAttributeSet>();
		protected Stack<HTML.Tag> tags = new Stack<HTML.Tag>(); 

		public void handleStartTag(HTML.Tag tag, MutableAttributeSet attr, int pos) {
			tags.push(tag);
			attrs.push(attr);
			String name = (String)attr.getAttribute(HTML.Attribute.NAME);
			if(!inEditForm) {
				if(tag.equals(HTML.Tag.FORM) && "edit_form".equals(name))
					inEditForm = true;
			}
			else {
				if(tag.equals(HTML.Tag.SELECT)) {
					selectName = (String)attr.getAttribute(HTML.Attribute.NAME);
				}
				else if(tag.equals(HTML.Tag.TEXTAREA)) {
					textareaName = (String)attr.getAttribute(HTML.Attribute.NAME);
				}
				else if(tag.equals(HTML.Tag.OPTION) && selectName != null) {
					if(attr.getAttribute(HTML.Attribute.SELECTED) == null) //if not selected, ignore
						return;

					String value = (String)attr.getAttribute(HTML.Attribute.VALUE);
					attr(selectName, value);
					//selectName = null; // todo: is this right? Doesn't handle multiple selects
				}
				// not sure when this can happen
				else if(textareaName != null) {

					System.err.println("starttag: textareaName wasn't null.");
					textareaName = null;
				}
			}
		}

		public void handleText(char[] data, int pos) {
			if(inEditForm) {
				if(textareaName != null) {
					if(!tags.peek().equals(HTML.Tag.TEXTAREA)) {
						System.err.println("Not actually in a textarea");
					}
					String testName = name;
					attr(textareaName, new String(data));
					textareaName = null;
				}
			}
		}
		
		// the real one
		public void handleEndTag(HTML.Tag tag, int pos) {
			HTML.Tag matchTag = tags.pop();
			if(!tag.equals(matchTag)) {
				System.err.println("tag doesn't match");
			}
			handleEndTag(tag, attrs.pop(), pos);
		}

		// mine
		public void handleEndTag(HTML.Tag tag, MutableAttributeSet attr, int pos) {
			
			if(inEditForm) {
				if(tag.equals(HTML.Tag.FORM) && "edit_form".equals(attr.getAttribute(HTML.Attribute.NAME))) {
					inEditForm = false;
				}
				if(tag.equals(HTML.Tag.SELECT)) {
					selectName = null;
				}
				else if(tag.equals(HTML.Tag.TEXTAREA)) {
					textareaName = null;
				}
			}
		}

		public void handleSimpleTag(HTML.Tag tag, MutableAttributeSet attr, int pos) {
			if(inEditForm) {
				
				// for some reason endtag is getting skipped on empty textareas (and selects?)
				if(textareaName != null) {
					System.err.println("simpletag: textareaName wasn't null.");
					textareaName = null;
				}
				if(selectName != null) {
					System.err.println("simpletag: selectname wasn't null.");
					selectName = null;
				}
				
				
				String type = (String)attr.getAttribute(HTML.Attribute.TYPE);
				String name = (String)attr.getAttribute(HTML.Attribute.NAME);
				String value = (String)attr.getAttribute(HTML.Attribute.VALUE);

				//we need INPUT, SELECT, what else?
				if(tag.equals(HTML.Tag.INPUT)) {
					//if it is radio/checkbox and not checked, ignore it (doesn't handle multiple checkboxes)
					// if not checked, ignore
					if((("radio".equals(type) || "checkbox".equals(type)) && attr.getAttribute(HTML.Attribute.CHECKED) == null)) 
						return;
					//if it is a cancel submit button, ignore it
					if("submit".equals(type) && "Cancel".equals(value))
						return;
					attr(name, value);
				}
			}
		}
	}

}
