package PloneWars;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.channels.Channels;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

import javax.swing.text.BadLocationException;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.lang3.StringEscapeUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class PlonePage extends PloneObject {
	public static final String	TYPE = "Document";

	private File								tempFile;

	protected String						etag;

	protected int								bodyStart, bodyEnd;
	protected Set<String>       bodyStartIds = new HashSet<String>();
	protected List<PloneURI>		fileURIs;
	
	//private boolean hasTOC;

	//for parser
	// I'm getting a strange bug where if the @@images/image style ends with a / trying to save gives a 500 error
	// so I'm going to check for but not match the final /
	protected static Pattern wpAppendPattern    = Pattern.compile("/(?:image_[^/\"]+|@@images/image(/[^/\"]+)?)/?(?=\")");
	protected static Pattern wpAlignPattern     = Pattern.compile("image-(left|right)");
	protected static Pattern imageAppendPattern	= Pattern.compile("/(?:image(?:_[^/]+)?|@@images/image(?:/[^/]+)?)(?=/$)");
	protected static Pattern otherAppendPattern	= Pattern.compile("/(?:(?:at_download/file)|(?:view))/$");

	public PlonePage(String name, PloneFolder parent) {
		super(name, parent);

		//TODO: hasTOC = false; //<dl style="display: block;" class="portlet toc" id="document-toc">
		bodyStart = bodyEnd = -1;
		
		if(PloneWarsApp.downloadVersion > 3)
			bodyStartIds.addAll(Arrays.asList(new String[] {"text"}));
		else
			bodyStartIds.addAll(Arrays.asList(new String[] {"parent-fieldname-text", "homepage-column-main"}));
		
		

		fileURIs = new ArrayList<PloneURI>();

		tempFile = null;

		etag = attr("etag");

		parent.subPages.put(name, this);
	}

	// retuns true if anything was changed
	public boolean updateToResource() {
		boolean didSomething = false;

		//PloneWarsApp.out.println(html);
		boolean exists = file.exists();
		boolean isParsed = isParsed();

		// TODO: due to now parsing cache in download() this shouldn't happen
		// maybe it will happen when we don't download but do upload.
		// TODO: this is broken, bodyStart is wrong somehow
		if(exists && !isParsed) {
			PloneWarsApp.out.println("need to parse " + name);
			bodyStart = 0;
			try {
				//TODO: probably best to read from Plone4

				DataInputStream dis = new DataInputStream(new FileInputStream(file));
				byte[] htmlBytes = new byte[dis.available()];
				dis.readFully(htmlBytes);
				dis.close();
				contents = new String(htmlBytes, UTF8);

				InputStreamReader fileReader = new InputStreamReader(new ByteArrayInputStream(htmlBytes), UTF8);
				HTMLEditorKit.ParserCallback callback = getCallBack();
				ParserDelegator delegator = new ParserDelegator();
				delegator.parse(fileReader, callback, true);
			}
			catch(IOException e) {
				e.printStackTrace();
				bodyStart = -1; //TODO: is this a bad idea?
			}
		}

		if(fileURIs.size() > 0) {
			PloneWarsApp.out.println(uploadURI.getPath() + " has " + fileURIs.size() + " URIs to update.");
			ListIterator<PloneURI> i = fileURIs.listIterator(fileURIs.size()); //reverse order so pos will be correct
			StringBuilder htmlBuilder = new StringBuilder(contents);
			boolean addedToPagesWithBrokenURIs = false;
			boolean addedToPagesWithWarningURIs = false;

			while (i.hasPrevious()) {
				PloneURI ploneURI = i.previous();
				

				int nextLT = htmlBuilder.indexOf("<", ploneURI.tagStart); // should always be 0
				int nextGT = htmlBuilder.indexOf(">", ploneURI.tagStart);
				if(nextLT != ploneURI.tagStart) {
					PloneWarsApp.out.println("Error: URI location mismatch, off by " + (nextLT - ploneURI.tagStart) + " at " + ploneURI.tagStart);
				}
				//PloneWarsApp.out.println(ploneURI.attr.toString() + "=[\"']?");
				String regex = ploneURI.attr.toString() + "=[\"']?";
				String tagHTML = htmlBuilder.substring(nextLT, nextGT);
				Matcher attrMatcher = Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(tagHTML);
				try {
					if (attrMatcher.find()) {
						String newURIvalue = null;
						String uid = null;
						if (ploneURI.uploadURI.equals(uploadURI)) { // links to self
							newURIvalue = "" + ploneURI.append;
							//PloneWarsApp.out.println("equal: " + newURIvalue);
						}
						else if ((uid = ploneURI.getUID()) != null) {
							newURIvalue = "resolveuid/" + uid + ploneURI.append;
						}

						if(newURIvalue != null) {
							//PloneWarsApp.out.println("got uid " + uid);
							int start = nextLT + attrMatcher.end();
							int end = start + ploneURI.origUriLength;
							htmlBuilder.replace(start, end, newURIvalue);
							didSomething = true;
						}
						else { // no uid found
							//See if this points to something that exists that just doesn't have a uid (most likely another site that appears to be a part of our site)
							HttpURLConnection connection = (HttpURLConnection)ploneURI.downloadURI.toURL().openConnection();
							connection.setInstanceFollowRedirects(true);
							connection.setRequestMethod("HEAD");

							String cssClass = null;
							// find out if this is a broken link or just no UUID
							System.out.println(ploneURI.downloadURI);
							System.out.println(connection.getResponseCode());
							System.out.println("---------------------");
							
							if(connection.getResponseCode() >= 400 && connection.getResponseCode() < 500) { // really is broken
								PloneWarsApp.out.println("\t" + connection.getURL() + " " + connection.getResponseCode() + " " + connection.getResponseMessage());

								if(!addedToPagesWithBrokenURIs) {
									PloneWarsApp.pagesWithBrokenURIs.add(this);
									addedToPagesWithBrokenURIs = true;
								}
								cssClass = "brokenlink";
							}
							// moved
							else if(connection.getResponseCode() >= 300) {
								// TODO: add option to follow redirects
								if(true) {
									try {
										URI trueDownloadURI = new URI(connection.getHeaderField("location"));
										URI downloadRootRelative = PloneWarsApp.downloadRootURI.relativize(trueDownloadURI);
										if(!downloadRootRelative.equals(trueDownloadURI)) {
											//resolve to uploadation root
											ploneURI.uploadURI = PloneWarsApp.uploadRootURI.resolve(downloadRootRelative);
											PloneWarsApp.out.println("\twill try again at " + ploneURI.uploadURI);
											i.next();
											continue;
										}
									}
									catch(URISyntaxException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
								}
								// this seems unreachable
								if(!addedToPagesWithWarningURIs) {
									PloneWarsApp.pagesWithWarningURIs.add(this);
									addedToPagesWithWarningURIs = true;
								}
								cssClass = "warninglink";
							}
							// is on download server, but our URI didn't match (maybe encoded, '%20' != ' ')
							else {
								PloneWarsApp.out.println("\t" + connection.getResponseCode() + " URI found on download server but not upload");
								
								// if this is a resolveUID from original site, we need to unresolve it
								if (ploneURI.wasUID) {
									int start = nextLT + attrMatcher.end();
									int end = start + ploneURI.origUriLength;
									htmlBuilder.replace(start, end, ploneURI.downloadURI.toString() + ploneURI.append);
									didSomething = true;
								}
								
								
								if (!addedToPagesWithWarningURIs) {
									PloneWarsApp.pagesWithWarningURIs.add(this);
									addedToPagesWithWarningURIs = true;
								}
								cssClass = "warninglink";
							}
								

							// label with class
							Matcher classMatcher = Pattern.compile("\\s+class=([\"']?).*?(\\1)", Pattern.CASE_INSENSITIVE).matcher(tagHTML);
							int start, end;
							String replace;
							if (classMatcher.find()) {
								//PloneWarsApp.out.println("class found");
								start = nextLT + classMatcher.start(2);
								end = start;
								replace = " " + cssClass;
							}
							else {
								//PloneWarsApp.out.println("no class found");
								start = nextLT + tagHTML.indexOf(" ") + 1; // since the tag has to have a src or href attribute, there will be a space where we can inject the class
								end = start;
								replace = "class=\"" + cssClass + "\"";
							}

							//PloneWarsApp.out.println("start: " + start + "; end: " + end + "; replace: " + replace);

							htmlBuilder.replace(start, end, replace);
							didSomething = true;
						}
					}
					else {
						PloneWarsApp.out.println("\tError: did not find " + regex); // I don't see how this is possible
					}
				}
				catch(IndexOutOfBoundsException e) {
					PloneWarsApp.out.println("\tError trying to find " + ploneURI.uploadURI + ": index out of bounds: " + ploneURI.tagStart + " of " + htmlBuilder.length());
					e.printStackTrace();
				}
				catch(IOException e) {
					e.printStackTrace();
				}
			} // end while
			contents = htmlBuilder.toString();
		}
		return didSomething;
	}

	protected Map<String, String> getDataMap() {
		Map<String, String> dataMap = super.getDataMap();

		if(contents == null || contents == "") {
			try {
				DataInputStream dis = new DataInputStream(new FileInputStream(file));
				byte[] htmlBytes = new byte[dis.available()];
				dis.readFully(htmlBytes);
				dis.close();
				contents = new String(htmlBytes, UTF8);
			}
			catch(IOException e) {
				e.printStackTrace();
			}
		}

		dataMap.put("text", contents);
		return dataMap;
	}

	protected String getId() {
		int dotIndex = name.lastIndexOf('.');
		if(dotIndex > 0 && PloneWarsApp.removePageExt)
			return name.substring(0, dotIndex);
		else
			return name;
	}

	public boolean upload() {
		/* */
		PloneWarsApp.out.println("upload: " + uploadURI.getPath() + " ... ");

		//because getDataMap needs the file
		if(!file.exists()) {
			PloneWarsApp.out.println("file doesn't exist, returning false");
			return false;
		}

		URL editURL = getEditURL();
		if(editURL == null) {
			PloneWarsApp.out.println("\tERROR: could not get the edit URL.");
			return false;
		}

		Map<String, String> dataMap = getDataMap();
		Map<String, File> fileMap = getFileMap();
		
		// key blacklist (may be more. consider a white list):
		dataMap.remove("creation_date");
		dataMap.remove("modification_date");

		byte tryCount = 0;
		while(true) {
			tryCount++;
			HttpURLConnection connection = PloneWarsUtils.post(editURL, dataMap, fileMap);
			if(connection == null) {
				PloneWarsApp.out.println("\tERROR: could not open connection to " + editURL);
				if(tryCount < PloneWarsApp.retries) {
					PloneWarsApp.out.println("\tretrying ... ");
					continue;
				}
				else {
					PloneWarsApp.out.println("\tfailed permanently");
					return false;
				}
			}

			connection.setInstanceFollowRedirects(false);

			try {
				if(connection.getResponseCode() == HttpURLConnection.HTTP_MOVED_TEMP) {
					// plone saved changes to the parent folder: this can happen if the page has the same name as the parent
					//if(connection.getHeaderField("Location").lastIndexOf('/') == connection.getHeaderField("Location").length() - 1)
					//PloneWarsApp.out.println(connection.getHeaderField("Location"));
					//PloneWarsApp.out.println(getPath());
					if(connection.getHeaderField("Location").endsWith("/")){  // TODO: this needs to not just check for a slash but for the whole path, too much lying

						//I think with my changes to getEditURL() this should never happen
						PloneWarsApp.out.println("I THOUGHT I FIXED THIS. THIS SHOULD NEVER HAPPEN!");
						PloneWarsApp.out.println("\tplone lied, fixing parent ...");
						parent.upload(); //fix parent
						PloneWarsApp.out.println("\tforcing new ... ");
						editURL = getNewEditURL(); // force to create new
						continue;
					}
					PloneWarsApp.out.println("\tdone ");
					break;
				}
				else if(connection.getResponseCode() == HttpURLConnection.HTTP_INTERNAL_ERROR) {
					PloneWarsApp.out.println("\tUnexpected response while trying to upload: " + connection.getResponseCode() + " " + connection.getResponseMessage());
					PloneWarsApp.out.println("\tA non-page object of the same name may already exist at this location ");
					return false;
				}
				else {
					PloneWarsApp.out.println("\tfailed! ");
					if(tryCount < PloneWarsApp.retries) {
						PloneWarsApp.out.println("\tretrying ... ");
						continue;
					}
					else {
						PloneWarsApp.out.println("\tfailed permanently: " + connection.getResponseCode() + " " + connection.getResponseMessage());
						PloneWarsUtils.streamCopy(connection.getInputStream(), new FileOutputStream(new File(parent.file, name + "." + connection.getResponseCode() + ".err")), true);

						PloneWarsApp.out.println();
						return false;
					}
				} //end responseCode tests
			}
			catch(IOException e) {
				e.printStackTrace();
			}
		} //end while
		return true;
	}

	public boolean download2() {
		String base = "http://louisville.edu";// + parent.getPath();
		try {
			PloneWarsApp.out.print("download: " + base + '/' + name + " ... ");

			if(file == null)
				file = new File(parent.file, name);

			URL downloadURL = new URL(base + '/' + name + "//replaceField?kukitTimeStamp=" + System.currentTimeMillis());

			//"http://louisville.edu/liberalstudies/prospective-majors/prospective-majors.html//replaceField?kukitTimeStamp=1345749783956

			PloneWarsApp.out.print("downloading ... ");
			// build data string
			Map<String, String> dataMap = new HashMap<String, String>();
			dataMap.put("fieldname", "text");
			dataMap.put("macro", "rich-field-view");
			dataMap.put("templateId", "widgets/rich");
			dataMap.put("edit", "true");
			HttpURLConnection connection = PloneWarsUtils.post(downloadURL, dataMap);

			InputSource is = new InputSource(new InputStreamReader(new DataInputStream(connection.getInputStream()), "UTF-8"));
			// is.setEncoding("UTF-8");
			Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(is);

			//Element root = doc.getDocumentElement();
			//root.normalize();

			String text = (String)XPathFactory.newInstance().newXPath().compile("/kukit/commands/command[@selector='parent-fieldname-text']/param[@name='html']/text()").evaluate(doc, XPathConstants.STRING);
			if(text.length() == 0) {
				System.err.println("returned xml did not have the necessary data, page possibly locked");
				return false;
			}
			else {
				PloneWarsApp.out.print("extracting ... ");
				int textareaStart = text.indexOf('>', text.indexOf("<textarea"));
				int textareaEnd = text.indexOf("</textarea>", textareaStart);
				String encodedHTML = text.substring(textareaStart + 1, textareaEnd);
				contents = PloneWarsUtils.decodeHTML(encodedHTML);
			}
		}
		catch(IOException | SAXException | ParserConfigurationException | XPathExpressionException e) {
			e.printStackTrace();
		}
		return true;
	}
	
	private boolean plone4download() {
		boolean success = false;

		PloneWarsApp.out.println("download page: " + uploadURI.getPath() + " ... ");
		try {
			HttpURLConnection connection = (HttpURLConnection)uploadURI.resolve("replaceField?kukitTimeStamp=" + System.currentTimeMillis()).toURL().openConnection();
			connection.setInstanceFollowRedirects(false);

			/* if(!PloneWarsApp.forceDownload && etag != null && etag != "")
				connection.setRequestProperty("If-None-Match", etag); */ // no etags, TODO: look for other caching? I don't see anything in headers

			if (connection.getResponseCode() == HttpURLConnection.HTTP_NOT_MODIFIED) {
				PloneWarsApp.out.println("\tpage up to date, skipping");
			}
			else {
				PloneWarsApp.out.println("\tdownloading ... ");
				//PloneWarsApp.out.println(connection.getHeaderFields());
				//etag = connection.getHeaderField("ETag");
				
				InputSource is = new InputSource(new InputStreamReader(new DataInputStream(connection.getInputStream()), UTF8));
				// is.setEncoding("UTF-8");
				try {
					Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(is);
					Element root = doc.getDocumentElement();
					root.normalize();
				}
				catch (SAXException | ParserConfigurationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		
		
		return success;
	}
	
	public File plone4download(HttpURLConnection connection) {
		File tempFile = null;
		// if(PloneWarsApp.uploadRootURI.equals(uploadURI)) { //this will happen when trying to open the site root. It's cool, the root is really there.
		try {
			byte tries = 4;
			while(tempFile == null && tries --> 0) {
				if(connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
					InputStream inputStream = connection.getInputStream();
					InputStreamReader reader = new InputStreamReader(inputStream, UTF8);
					BufferedReader bufferedReader = new BufferedReader(reader);
					String inputLine;
					String key = "";
					StringBuffer valBuf = new StringBuffer(); //, contentsBuffer = new StringBuffer();
	
					tempFile = File.createTempFile("temp-" + name, null);
					//FileWriter fileWriter = new FileWriter(tempFile);
					BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tempFile), UTF8));
					
					//ignore first batch, next batch is attributes, next batch is content
					boolean ignoreMode = true, attrMode = false, contentMode = false;
					while((inputLine = bufferedReader.readLine()) != null) {
						// signals a change
						if(inputLine.equals("")) {
							if(ignoreMode) {
								ignoreMode = false;
								attrMode = true;
								continue;
							}
							// double return?
							else if(attrMode) {
								attrMode = false;
								contentMode = true;
								continue;
							}
						}
						
						// read in the attributes
						if(attrMode) {
							// a continuation line
							if(inputLine.startsWith("  ")) {
								// plone inserts blank lines between each line, ignore them.
								if(!inputLine.equals("  ")) {
									System.out.println(URLEncoder.encode(inputLine.substring(2), UTF8.toString()));
									valBuf.append("\n" + inputLine.substring(2));
								}
							}
							// a new line
							else {
								int colonIndex = inputLine.indexOf(": ");
								// a new entry
								if(colonIndex >= 1) {
									// add finished attr
									if(key.length() > 0)
										attr(key, valBuf.toString());
									
									// make new attr
									key = inputLine.substring(0, colonIndex);
									valBuf = new StringBuffer(inputLine.substring(colonIndex + 2, inputLine.length()));
								}
								else {
									// should never get here
									System.out.println("error, not a continue or a new. what is this: " + inputLine);
								}
							}
						}
						// page contents
						else if(contentMode) {
							bufferedWriter.write(inputLine);
							bufferedWriter.newLine();
						}
					}
					//System.out.println(data);
					//System.out.println("contents: " + contentsBuffer.toString());
					//contents = contentsBuffer.toString();
					
					bufferedReader.close();
					bufferedWriter.close();
				}
				else {
					PloneWarsApp.out.println("Unexpected response from server while trying to open " + connection.getURL() + " : " + connection.getResponseCode() + ' ' + connection.getResponseMessage());
					connection = (HttpURLConnection)connection.getURL().openConnection();
					System.out.println("retry plone4download");
				}
			}
		}
		catch(IOException e) {
			e.printStackTrace();
		}
		return tempFile;
	}
	
	protected URL getDownloadURL() throws MalformedURLException {
		if(PloneWarsApp.downloadVersion > 3) {
			// old plone 4 method: downloadURI.resolve("atct_edit?tinymce.suppress=text").toURL()
			return downloadURI.resolve("external_edit").toURL();
		}
		else {
			return downloadURI.toURL();
		}
	}

	public boolean download() {
		PloneWarsApp.out.println("download page: " + downloadURI.getPath() + " ... ");
		super.download();
		
		try {
			HttpURLConnection connection = (HttpURLConnection)getDownloadURL().openConnection();
			//connection.setInstanceFollowRedirects(false);

			// Plone 3 only. Exists in Plone 4, but not turned on on our servers
			if(!PloneWarsApp.forceDownload && etag != null && etag != "") {
				connection.setRequestProperty("If-None-Match", etag);
			}
			
			InputStream inputStream;
			// from cache, but Plone 4 doesn't appear to have cache control turned on, so this will never happen (maybe move this to plone3download()
			if(connection.getResponseCode() == HttpURLConnection.HTTP_NOT_MODIFIED) {
				PloneWarsApp.out.println("\tpage up to date, using cache");

				DataInputStream dis = new DataInputStream(new FileInputStream(file));
				byte[] htmlBytes = new byte[dis.available()];
				dis.readFully(htmlBytes);
				dis.close();
				contents = new String(htmlBytes, UTF8);
				
				inputStream = new ByteArrayInputStream(htmlBytes);
				// always 0 for cache
				bodyStart = 0;
				bodyEnd   = contents.length();
			}
			// from download
			else {
				PloneWarsApp.out.println("\tdownloading ... ");
				
				if(PloneWarsApp.downloadVersion > 3) {
					//plone4downloadDetails(); // not needed because it only gets title and description which are set when loaded from folder
					tempFile = plone4download(connection);
					// got contents directly in file already
					bodyStart = 0;
				}
				else {
					etag = connection.getHeaderField("ETag");
					tempFile = plone3download(connection);
				}
				inputStream = new FileInputStream(tempFile);
			}
			
			PloneWarsApp.out.println("\tparsing ... ");

			InputStreamReader fileReader = new InputStreamReader(inputStream, UTF8);
			HTMLEditorKit.ParserCallback callback = getCallBack();
			ParserDelegator delegator = new ParserDelegator();
			delegator.parse(fileReader, callback, true);
			
			// some parse error
			if(bodyStart == bodyEnd) {
				return false;
			}

		}
		catch(IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	protected File plone3download(HttpURLConnection connection) throws IOException {
		// temp file prefix must be at least 3 chars
		File tempFile = File.createTempFile("temp-" + name, null);
		PloneWarsUtils.streamCopy(connection.getInputStream(), new FileOutputStream(tempFile), true);
		return tempFile;
	}

	protected HTMLEditorKit.ParserCallback getCallBack() {
		return new CallBack();
	}

	protected boolean isParsed() {
		// not sure how to calculate bodyEnd in Plone 4
		return(bodyStart >= 0); // && bodyEnd > bodyStart);
	}
	
	public String preContent() {
		return "";
	}

	/**
	 * yes it's a giant try/catch. yes it always returns true. sorry. Maybe I'll clean it up one day.
	 */
	public boolean save() {
		PloneWarsApp.out.println("\tsaving to " + file.getPath() + " ... ");

		try {
		
				if (tempFile != null) {
					if (isParsed()) {
						FileInputStream fis = new FileInputStream(tempFile);
						// Cannot use Channel.transferTo because bodyStart and bodyEnd are for UTF8
						DataInputStream dis = new DataInputStream(fis);
						byte[] htmlBytes = new byte[dis.available()];
						dis.readFully(htmlBytes);
						dis.close();
						
						contents = new String(htmlBytes, UTF8);
						if(bodyStart > 0 || bodyEnd > 0) {
							contents = contents.substring(bodyStart, bodyEnd);
						}
						
					}
					//didn't find a body
					else { 
						contents = "";
					}
					tempFile.delete();
				}

			// write contents
			if (contents.length() > 0) {
				OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file), UTF8);
				
				if (PloneWarsApp.wpExport) {
					writer.write(
						"<html>\n" +
						"	<head>\n" + 
						"		<title>" + attr("title") + "</title>\n" + "" +
						"		<meta name=\"description\" content=\"" + attr("description").replace("\"", "&quot;") + "\"/>\n" +
						"	</head>\n" +
						"	<body>\n"
					);
					for (Entry<String, String> i : data.entrySet()) {
						writer.write("		<div id=\"" + i.getKey().replace("\"", "&quot;") + "\">" + i.getValue().replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;") + "</div>\n");
					}
					// cheating
					writer.write("		<div id=\"image\">" + name + "-image.jpg</div>\n");
					writer.write("		<div id=\"content\">\n");
					//writer.write(preContent());
					
					// fix for WordPress
					/*
					if(fileURIs.size() > 0) {
						ListIterator<PloneURI> i = fileURIs.listIterator(fileURIs.size()); //reverse order so pos will be correct
						StringBuilder htmlBuilder = new StringBuilder(contents);

						while(i.hasPrevious()) {
							PloneURI ploneURI = i.previous();
						}
					}
					*/
					
					// easy way first
					contents = wpAppendPattern.matcher(contents).replaceAll("");
					contents = wpAlignPattern.matcher(contents).replaceAll("align$1");
					contents = contents.replaceAll("\"", "");
					
					//autop function that changes new lines into HTML and HTML into new lines.
					contents = contents.replaceAll("\\s+", " ");
				}
				
				writer.write(contents);
				
				if(PloneWarsApp.wpExport) {
					writer.write(
						"		</div>\n" +
						"	</body>\n" +
						"</html>"
					);
				}
				
				writer.close();
			}
			// make empty file
			else {
				if (file.exists()) {
					// this appears to delete all tagged data
					new RandomAccessFile(file, "rw").setLength(0);
				}
				else {
					file.createNewFile();
				}
			}
		}
		// if we don't have write access
		catch (IOException e) {
			e.printStackTrace();
		}

		attr("etag", etag);
		super.save();
		return true;
	}

	protected class CallBack extends HTMLEditorKit.ParserCallback {
		protected Stack<String>	ids						= new Stack<String>();
		protected boolean				bodyStartNext	= false;

		// public void flush() throws BadLocationException{}
		// public void handleComment(char[] data, int pos){}

		public void handleStartTag(HTML.Tag tag, MutableAttributeSet attr, int pos) {
			String id = (String)attr.getAttribute(HTML.Attribute.ID);
			ids.push(id);

			// TODO: look for toc

			if(bodyStartNext) {
				bodyStart = pos;
				bodyStartNext = false;
			}
			else if(bodyStart < 0 && bodyStartIds.contains(id)) { // id is one of the ones we're looking for 
				bodyStartNext = true;
			}

			//look for links to documents
			if(bodyStart > -1 && bodyEnd < 0 && tag.equals(HTML.Tag.A)) {
				String href = ((String)attr.getAttribute(HTML.Attribute.HREF));

				if(href != null && href.length() > 0) {
					//if(href.matches(".+\\.[^./]+/$") && !href.endsWith(".html/") && !href.endsWith(".ics/")) //has an extension, but not extensions to things we already know about
					handleURI(HTML.Attribute.HREF, href, pos);
				}
			}
		}

		public void handleSimpleTag(HTML.Tag tag, MutableAttributeSet attr, int pos) {

			if(bodyStartNext) {
				bodyStart = pos;
				bodyStartNext = false;
			}

			//look for images
			if(bodyStart > -1 && bodyEnd < 0 && tag.equals(HTML.Tag.IMG)) {
				String src = ((String)attr.getAttribute(HTML.Attribute.SRC));
				handleURI(HTML.Attribute.SRC, src, pos);
			}
		}

		private void handleURI(HTML.Attribute attr, String uriValue, int pos) {
			String origURIvalue = uriValue;
			if(uriValue == null) {
				// TODO: handle this
				PloneWarsApp.out.println("\tmissing href/src at " + pos + ". Skipping.");
				return;
			}
			uriValue = uriValue.trim(); // it can happen

			// only do something if we have something
			if(uriValue.length() > 0 && !uriValue.startsWith("mailto:")) {
				int poundIndex = uriValue.indexOf('#');
				String fragment = null;
				if(poundIndex == 0) {
					//links to this page, do nothing
					return;
				}
				else if(poundIndex > 0) {
					//link to some other page
					fragment = uriValue.substring(poundIndex + 1);
					uriValue = uriValue.substring(0, poundIndex);
				}

				if(!uriValue.endsWith("/"))
					uriValue += '/';

				URI uri = null;
				try {
					if(uriValue.equals("./")) {
						uri = downloadURI;
					}
					else {
						// to match inernal naming style (in PloneFolder.getNameFromURI)
						uriValue = URLDecoder.decode(uriValue, UTF8.name());
						 // most common illegal character, won't catch everything tho
						uri = new URI(uriValue.replace(" ", "%20"));
					}
				}
				catch(URISyntaxException | UnsupportedEncodingException e) {
					e.printStackTrace();
					return;
				}

				try {
					if(!uri.isAbsolute()) {
						uri = downloadURI.resolve("..").resolve(uri); //need the .. because we append a slash to the end earlier
					}
					// if() //TODO: change https to http if necessary

					URI relative = PloneWarsApp.downloadRootURI.relativize(uri);
					
					// see if the uri is a part of this website
					if(!relative.isAbsolute()) { 
						String append = "";
						Matcher imageAppendMatcher = imageAppendPattern.matcher(uri.toString());
						Matcher otherAppendMatcher = otherAppendPattern.matcher(uri.toString());
						if(fragment != null && fragment.length() > 0) {
							append = fragment;
							//URI thisPageTestURI = plone3URI.relativize(uri);
						}
						else if(imageAppendMatcher.find()) {
							uri = new URI(uri.toString().substring(0, imageAppendMatcher.start() + 1)); //remove image resize suffix
							append = imageAppendMatcher.group();
						}
						else if(otherAppendMatcher.find()) {
							uri = new URI(uri.toString().substring(0, otherAppendMatcher.start() + 1));
							append = otherAppendMatcher.group();
						}

						// if this is a uid link, resolve it
						Matcher uidMatcher = Pattern.compile("resolveuid/(\\w+)/$").matcher(uri.toString());
						boolean isUID = uidMatcher.find();
						if(isUID) {
							// String uid = uidMatcher.group(1);
							try {
								HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
								connection.setInstanceFollowRedirects(false);
								
								if (connection.getResponseCode() >= 300 && connection.getResponseCode() < 400) {
									uri = new URI(connection.getHeaderField("location") + '/');
								}
								else {
									System.err.println("I thought this was a resolveuid link, but it's not: " + uri.toString());
								}
							}
							catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
						

						// add to main file uri list
						if(!PloneWarsApp.fileURIs.contains(uri)) {
							//PloneWarsApp.out.println("\treference to: " + uri);
							
							//tests for ending. what if it doesn't have one? TODO: Plone 4 doens't give extension to files by default. :( maybe have to ask the server for mime type
							 //has an extension, but not html or ics
							if(uri.toString().matches(".+\\.[^./]+/$") && !uri.toString().endsWith(".html/") && !uri.toString().endsWith(".ics/")) {
								PloneWarsApp.out.println("\tadding " + uri + " to list");
								PloneWarsApp.fileURIs.add(uri);
							}
						}

						//PloneWarsApp.out.println(origURIvalue);
						/*
						if(append.length() > 0)
							relative = PloneWarsApp.downloadRootURI.relativize(uri); //redo if append removed
						*/
						
						 // not a link to the root (which doesn't have a uid)
						if (relative.getPath().length() > 0) {

							// figure out where to point to in Plone 4
							// first, is this link relative to our download folder?
							URI downloadRelative = PloneWarsApp.downloadURI.relativize(uri);
							URI forUpload = null;
							if(!downloadRelative.equals(uri)) // part of subfolder struction
								// resolve to uploadation subfolder
								forUpload = PloneWarsApp.uploadURI.resolve(downloadRelative); 
							// TODO: if only moving part of the site, this should be skipped. add an option?
							else {
								// points to something outside subfolder, fall back to site root
								URI downloadRootRelative = PloneWarsApp.downloadRootURI.relativize(uri);
								if(!downloadRootRelative.equals(uri)) {
									//resolve to uploadation root
									forUpload = PloneWarsApp.uploadRootURI.resolve(downloadRootRelative);
								}
							}
							
							// link to home page?
							assert forUpload != null; 
							
							PloneURI ploneURI = new PloneURI(uri, forUpload, attr, pos - bodyStart, origURIvalue.length(), append, isUID);
							fileURIs.add(ploneURI);
						}
					}
				}
				catch(URISyntaxException e) {
					e.printStackTrace();
				}
			}
		}

		public void handleEndTag(HTML.Tag tag, int pos) {

			if(bodyStartNext) {
				bodyStart = pos;
				bodyStartNext = false;
			}

			String id = ids.pop();

			if(bodyStart >= 0 && bodyStartIds.contains(id)) {
				bodyEnd = pos;
				try {
					this.flush(); // exit parser, I think
				}
				catch(BadLocationException e) {
				}
			}
			// PloneWarsApp.out.println("End Tag: " + tag + "; match: " + match + "; id: " +
			// id + ";  pos: " + pos);
		}

		public void handleText(char[] data, int pos) {
		
			if(bodyStartNext) {
				bodyStart = pos;
				bodyStartNext = false;
			}
			// HTML.Tag tag = tags.peek();
			// String dataString = new String(data);
			// PloneWarsApp.out.println("Text: " +
			// dataString.substring(0,Math.min(dataString.length(),10)) + "; Tag: " +
			// tag + "; pos: " + pos);
		}
		// public void handleError(String errorMsg, int pos){}
		// public void handleEndOfLineString(String eol) {}
	}

}
