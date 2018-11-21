/**
 * 
 */
package PloneWars;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Map;

import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;

/**
 * @author dadave02
 * 
 */
public class PloneNews extends PlonePage {
	public static final String TYPE = "News Item";
	public static final String EXT  = ".html";

	private URI imageURI, expiresURI, effectiveURI, creatorURI, createdURI, modifiedURI;
	private File tempFile;

	private static Date defaultExpirationDate	= new GregorianCalendar(2499, Calendar.DECEMBER, 31).getTime();
	private static Date defaultEffectiveDate = new Date();

	private Date expirationDateDate, effectiveDateDate;
	private String creator;

	/**
	 * @param name
	 * @param parent
	 */
	public PloneNews(String name, PloneFolder parent) {
		super(name, parent);

		imageURI = downloadURI.resolve("image");
		expiresURI = downloadURI.resolve("expires");
		effectiveURI = downloadURI.resolve("effective");
		createdURI = downloadURI.resolve("created");
		modifiedURI = downloadURI.resolve("modified");
		creatorURI = downloadURI.resolve("Creator");

		/*
		caption = attr("caption");
		expirationDate = attr("expirationDate");
		expirationDate_year = attr("expirationDate_year");
		expirationDate_month = attr("expirationDate_month");
		expirationDate_day = attr("expirationDate_day");
		expirationDate_hour = attr("expirationDate_hour");
		expirationDate_minute = attr("expirationDate_minute");
		expirationDate_ampm = attr("expirationDate_ampm");
		*/

		expirationDateDate = defaultExpirationDate;

		try {
			String ploneImage = URLEncoder.encode("plone.image", UTF8.name());
			if(view != null && view.list().contains(ploneImage)) {
				tempFile = File.createTempFile("temp-" + name, null);
				ByteBuffer imageBuffer = ByteBuffer.allocate(view.size(ploneImage));
				view.read(ploneImage, imageBuffer);
				imageBuffer.flip(); //probably not needed
				FileOutputStream fos = new FileOutputStream(tempFile);
				fos.write(imageBuffer.array());
				fos.close();
			}
		}
		catch(IOException e) {
			e.printStackTrace();
			if(tempFile.canWrite())
				tempFile.delete();
		}
	}
	
	public static Date ploneUriToDate(URI ploneURI, Date defaultDate) {
		Date date = null;
		try {
			PloneWarsApp.out.println("\tdownload date: " + ploneURI.getPath() + " ... ");
			HttpURLConnection connection = (HttpURLConnection)ploneURI.toURL().openConnection();
			connection.setInstanceFollowRedirects(true);
			// some sites force HTTPS, but the 302 response contains the date anyway
			if (connection.getResponseCode() == HttpURLConnection.HTTP_OK || connection.getResponseCode() == HttpURLConnection.HTTP_MOVED_TEMP) {
				PloneWarsApp.out.println("\tdownloading ... ");
				BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream(), UTF8));
				String inputLine;
				while ((inputLine = in.readLine()) != null) { // should only be one line, but we'll loop anyway
					if (inputLine.indexOf("/") >= 0) { // has a date in it
						date = ploneDateStringToDate(inputLine, defaultEffectiveDate);
						break; //that's the only line we need
					}
				}
				in.close();
			}
			else {
				date = defaultDate;
				PloneWarsApp.out.println("\tunexpected response from server: " + connection.getResponseCode() + " " + connection.getResponseMessage());
			}
		}
		catch(Exception e) {
			date = defaultDate;
			e.printStackTrace();
		}
		return date;
	}
	
	public void splitAndSaveDate(Date date, String prefix) {
		attr(prefix,         PLONE_DATE_FORMAT.format(date));
		attr(prefix + "_year",     YEAR_FORMAT.format(date));
		attr(prefix + "_month",   MONTH_FORMAT.format(date));
		attr(prefix + "_day",       DAY_FORMAT.format(date));
		attr(prefix + "_hour",     HOUR_FORMAT.format(date));
		attr(prefix + "_minute", MINUTE_FORMAT.format(date));
		attr(prefix + "_ampm",     AMPM_FORMAT.format(date));
	}

	public boolean download() {
		boolean downloaded = super.download();
		if(downloaded) {
			
			effectiveDateDate = ploneUriToDate(effectiveURI, defaultEffectiveDate);
			splitAndSaveDate(effectiveDateDate, "effectiveDate");

			expirationDateDate = ploneUriToDate(expiresURI, defaultExpirationDate);
			splitAndSaveDate(expirationDateDate, "expirationDate");

			attr("createdDate", PloneWarsUtils.ploneUriToString(createdURI));
			
			// replace creators with creators:lines
			String creators = attr("creators");
			attr("creators", (String)null);
			attr("creators:lines", creators != null ? creators : PloneWarsUtils.ploneUriToString(creatorURI));
			

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
					tempFile = File.createTempFile("temp-" + name, '.' + connection.getHeaderField("Content-Type").split("/")[1]);
					PloneWarsUtils.streamCopy(connection.getInputStream(), new FileOutputStream(tempFile), true);
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
		}
		return downloaded;
	}
	
	public String preContent() {
		String html = "";
		if (tempFile != null && tempFile.exists()) {
			String imageURLString = name + "-image.jpg";
			html = "[caption id=\"\" align=\"alignright\" width=\"200\"]<a href=\"" + imageURLString + "\"><img class=\"size-thumbnail\" src=\"" + imageURLString + "\" alt=\"" + attr("caption") + "\" width=\"200\"/></a> " + attr("caption") + "[/caption]";
		}
		return html;
	}
	
	public boolean save() {
		//attr("caption", caption);
		//attr("image", )
		if (PloneWarsApp.wpExport && tempFile != null && tempFile.exists()) {
			try {
				File newsImage = new File(parent.file, name + "-image.jpg"); 
				Files.copy(tempFile.toPath(), new FileOutputStream(newsImage));
			}
			catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return super.save() && view != null && tempFile != null && tempFile.exists() && tag("plone.image", tempFile);
	}

	/* should be handled automatically now
	protected Map<String, String> getDataMap() {
		Map<String, String> dataMap = super.getDataMap();

		dataMap.put("expirationDate", expirationDate);
		dataMap.put("expirationDate_year", expirationDate_year);
		dataMap.put("expirationDate_month", expirationDate_month);
		dataMap.put("expirationDate_day", expirationDate_day);
		dataMap.put("expirationDate_hour", expirationDate_hour);
		dataMap.put("expirationDate_minute", expirationDate_minute);
		dataMap.put("expirationDate_ampm", expirationDate_ampm);

		dataMap.put("imageCaption", caption);
		return dataMap;
	}
	*/

	protected Map<String, File> getFileMap() {
		Map<String, File> fileMap = super.getFileMap();

		if(createdNew || PloneWarsApp.forceUpload) {
			if(tempFile != null && tempFile.exists() && tempFile.length() > 0)
				fileMap.put("image_file", tempFile);
		}

		return fileMap;
	}

	protected HTMLEditorKit.ParserCallback getCallBack() {
		return new CallBack();
	}

	protected class CallBack extends PlonePage.CallBack {
		protected boolean	captionStartNext	= false, captionFound = false;

		public void handleStartTag(HTML.Tag tag, MutableAttributeSet attr, int pos) {
			super.handleStartTag(tag, attr, pos);

			 // caption is before the body
			if(bodyStart < 0 && !captionFound) {
				String id = ids.peek();
				if(id != null && id.equals("parent-fieldname-imageCaption")) {
					captionStartNext = true;
				}
			}
		}

		public void handleText(char[] data, int pos) {
			super.handleText(data, pos);
			if(captionStartNext) {
				attr("caption", new String(data).trim());
				captionStartNext = false;
				captionFound = true;
			}
		}
	}

}
