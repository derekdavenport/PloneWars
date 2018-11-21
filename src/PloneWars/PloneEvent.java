package PloneWars;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.Map;
import java.util.regex.Pattern;

public class PloneEvent extends PlonePage {
	public static final String			TYPE					= "Event";

	protected static final Pattern	EMAIL_PATTERN	= Pattern.compile("^[\\w._%+-]+@[\\w.-]+\\.[A-Z]{2,4}$", Pattern.CASE_INSENSITIVE);
	protected static final Pattern	PHONE_PATTERN	= Pattern.compile("\\d{4}");

	protected boolean	ignore;

	protected URI		icsURI = null;
	protected Date		startDate, endDate;

	public PloneEvent(String name, PloneFolder parent) {
		super(name, parent);
		ignore = false;

		icsURI = downloadURI.resolve("ics_view");

		String startDateString = attr("startDate");
		startDate = startDate != null && startDateString.length() > 0 ? ploneDateStringToDate(startDateString, null) : null;

		String endDateString = attr("endDate");
		endDate = endDateString != null && endDateString.length() > 0 ? ploneDateStringToDate(endDateString, null) : null;
	}

	public String getId() {
		return name;
	}

	protected Map<String, String> getDataMap() {
		Map<String, String> dataMap = super.getDataMap();

		dataMap.put("attendees:lines", "");
		//for(String category : categories.split(","))
		//dataMap.put("eventType_existing_keywords:list", category);
		dataMap.put("subject_keywords:lines", dataMap.remove("categories")); // rename

		return dataMap;
	}

	public boolean upload() {
		if(endDate == null || endDate.before(PloneWarsApp.earliestEventDate)) {
			PloneWarsApp.out.println("upload: " + uploadURI.getPath() + " ... too old, " + endDate + " is before " + PloneWarsApp.earliestEventDate + "; ignoring ... ");
			return false;
		}
		else
			return super.upload();
	}

	public boolean download() {
		boolean success = false;
		try {
			PloneWarsApp.out.println("\tdownload: " + icsURI.getPath() + " ... ");
			HttpURLConnection connection = (HttpURLConnection)icsURI.toURL().openConnection();
			connection.setInstanceFollowRedirects(true);
			if(file.exists() && !PloneWarsApp.forceDownload) {
				connection.setRequestProperty("If-Modified-Since", MODIFIED_DATE_FORMAT.format(new Date(file.lastModified())));
			}

			if(connection.getResponseCode() == HttpURLConnection.HTTP_NOT_MODIFIED) {
				PloneWarsApp.out.println("\tevent up to date, skipping");
			}
			else {
				PloneWarsApp.out.println("\tdownloading ... ");

				BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream(), UTF8));
				String inputLine;
				while((inputLine = in.readLine()) != null) {
					//PloneWarsApp.out.println(inputLine);
					int colonPos = inputLine.indexOf(':');
					if(colonPos > 0) {
						switch(inputLine.substring(0, colonPos).toUpperCase()) {
							case "DTSTART":
								startDate = ICS_DATE_FORMAT.parse(inputLine.substring(colonPos + 1));
								attr("startDate", PLONE_DATE_FORMAT.format(startDate));
								attr("startDate_year", YEAR_FORMAT.format(startDate));
								attr("startDate_month", MONTH_FORMAT.format(startDate));
								attr("startDate_day", DAY_FORMAT.format(startDate));
								attr("startDate_hour", HOUR_FORMAT.format(startDate));
								attr("startDate_minute", MINUTE_FORMAT.format(startDate));
								attr("startDate_ampm", AMPM_FORMAT.format(startDate));
								break;
							case "DTEND":
								endDate = ICS_DATE_FORMAT.parse(inputLine.substring(colonPos + 1));
								attr("endDate", PLONE_DATE_FORMAT.format(endDate));
								attr("endDate_year", YEAR_FORMAT.format(endDate));
								attr("endDate_month", MONTH_FORMAT.format(endDate));
								attr("endDate_day", DAY_FORMAT.format(endDate));
								attr("endDate_hour", HOUR_FORMAT.format(endDate));
								attr("endDate_minute", MINUTE_FORMAT.format(endDate));
								attr("endDate_ampm", AMPM_FORMAT.format(endDate));
								break;
							case "CATEGORIES":
								attr("categories", inputLine.substring(colonPos + 1).replace(',', '\n')); //Plone 4 separates by newline, not comma
								break;
							case "LOCATION":
								attr("location", inputLine.substring(colonPos + 1).replace("\\,", ","));
								break;
							case "URL":
								attr("eventUrl", inputLine.substring(colonPos + 1));
								break;
							case "CONTACT":
								LinkedList<String> contactData = new LinkedList<String>(Arrays.asList(inputLine.substring(colonPos + 1).split("\\s*\\\\,\\s*"))); //should be name, number, email

								// search in reverse for email, number, name
								if(!contactData.isEmpty() && EMAIL_PATTERN.matcher(contactData.peekLast()).find()) {
									attr("contactEmail", contactData.removeLast());
								}

								if(!contactData.isEmpty() && PHONE_PATTERN.matcher(contactData.peekLast()).find()) {
									attr("contactPhone", contactData.removeLast());
									//contactDatum = contactData.isEmpty() ? null :  contactData.removeLast();
								}

								if(!contactData.isEmpty()) {
									StringBuilder nameBuilder = new StringBuilder();
									for(String contactDatum : contactData) {
										nameBuilder.append(contactDatum + ", ");
									}
									attr("contactName", nameBuilder.substring(0, nameBuilder.length() - 2));
								}

								//PloneWarsApp.out.print("name: " + contactName + "; phone: " + contactPhone + "; email: " + contactEmail);
								break;
						}
					}
				}
				in.close();
				
				if(endDate != null && endDate.before(PloneWarsApp.earliestEventDate)) {
					PloneWarsApp.out.println("\ttoo old, " + endDate + " is before " + PloneWarsApp.earliestEventDate + "; ignoring ... ");
					return false;
				}
				
				success = super.download();
			}
		}
		catch(IOException | ParseException e) {
			e.printStackTrace();
			success = false;
		}

		return success;
	}
}
