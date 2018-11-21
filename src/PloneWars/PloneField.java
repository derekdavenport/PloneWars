/**
 * 
 */
package PloneWars;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;

/**
 * @author dadave02
 * 
 */
public abstract class PloneField extends PloneObject {

	/**
	 * @param name
	 * @param parent
	 */
	public PloneField(String name, PloneFolder parent) {
		super(name, parent);
		parent.subFields.put(name, this);
		
		// TODO: serealize hashmap and save to attr
		// TODO: unserealize hashmap and retrieve from attr
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see plonewars.PloneObject#download()
	 */
	@Override
	public boolean download() {
		// right now I'm not going to download anything

		URL editURL = null;
		try {
			editURL = downloadURI.resolve(new URI(null, null, "edit", null)).toURL();
		}
		catch(URISyntaxException | MalformedURLException e) {
			e.printStackTrace();
		}

		// TODO: make sure edit connection works
		ParserDelegator delegator = new ParserDelegator();
		try {
			delegator.parse(new InputStreamReader(editURL.openConnection().getInputStream(), UTF8), getFormEditParserCallBack(), true);
		}
		catch(IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return true;
	}

	protected HTMLEditorKit.ParserCallback getFormEditParserCallBack() {
		return new FormEditParserCallBack();
	}

	protected class FormEditParserCallBack extends HTMLEditorKit.ParserCallback {
		boolean	inEditForm	= false;

		String	selectName	= null, textareaName = null;
		
		protected Stack<MutableAttributeSet> attrs = new Stack<MutableAttributeSet>();

		public void handleStartTag(HTML.Tag tag, MutableAttributeSet attr, int pos) {
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
				}
			}
		}

		public void handleText(char[] data, int pos) {
			if(inEditForm) {
				if(textareaName != null) {
					attr(textareaName, new String(data));
					textareaName = null;
				}
			}
		}
		
		public void handleEndTag(HTML.Tag tag, int pos) {
			handleEndTag(tag, attrs.pop(), pos);
		}

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

	/*
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
	*/

	/*
	protected Map<String, String> getDataMap() {
		Map<String, String> dataMap = super.getDataMap(); //new HashMap<String, String>();

		//dataMap.put("fgTDefault", formElements.get("fgTDefault"));
		//dataMap.putAll(); // overwrite with these values
		dataMap.putAll(formElements); // overwrite with the form elements
		
		// TODO: code to load in saved formElements from attr. serialize?

		return dataMap;
	}
	*/

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

		HttpURLConnection connection = PloneWarsUtils.post(editURL, getDataMap(), null);
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
