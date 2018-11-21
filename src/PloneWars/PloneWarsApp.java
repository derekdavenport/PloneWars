/**
 * 
 */
package PloneWars;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Toolkit;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.DataOutputStream;
import java.io.PrintStream;

import java.net.CookieManager;
import java.net.CookieHandler;
import java.net.HttpCookie;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.net.HttpURLConnection;
import java.nio.file.Files;

import javax.swing.SwingWorker;
import javax.swing.UIManager;

/**
 * @author dadave02
 * 
 */
public class PloneWarsApp extends SwingWorker<Void, Void> {
	private static SimpleDateFormat sdf					= new SimpleDateFormat("yyyy-MM-dd HH-mm-ss");
	
	public static PrintStream out;// = System.out;

	public static short									downloadVersion, uploadVersion;
	public static boolean								downloadStaging, uploadStaging;
	public static String								downloadHost, downloadSite, uploadSite, downloadSubfolders, uploadSubfolders, downloadUsername, uploadUsername, downloadPassword, uploadPassword;

	public static Date									earliestEventDate;
	
	public static boolean								forceDownload, forceUpload, allFiles, removePageExt, addPageExt = true, uri2uid, defaultViews, excludeFromNavs, wpExport = false;
	
	public static byte									retries;

	
	public static URI										downloadRootURI, uploadRootURI, downloadURI, uploadURI;
	public static File									rootFolder, baseFolder;
	public static PloneFolder						rootPloneFolder, basePloneFolder;
	public static LinkedList<URI>				fileURIs;
	public static ArrayList<PlonePage>	pagesWithBrokenURIs, pagesWithWarningURIs;
	
	private HttpCookie						downloadCookie, uploadCookie;

	private static long starttime;
	private static int progress;
	private static int totalProgress;
	
	public PloneWarsApp(
			short downloadVersion, String downloadHost, boolean downloadStaging, String downloadSite, String downloadSubfolders, String downloadUsername, String downloadPassword,
			boolean allFiles, boolean forceDownload, boolean defaultViews, boolean excludeFromNavs, Date earliestEventDate,
			short uploadVersion,   boolean uploadStaging,   String uploadSite,   String uploadSubfolders,   String uploadUsername, String uploadPassword,
			boolean forceUpload, boolean removePageExt, boolean uri2uid) {
		
		PloneWarsApp.downloadVersion = downloadVersion;
		PloneWarsApp.downloadStaging = downloadStaging;
		PloneWarsApp.downloadHost = downloadHost;
		PloneWarsApp.downloadSite = downloadSite;
		PloneWarsApp.downloadSubfolders = downloadSubfolders;
		PloneWarsApp.downloadUsername = downloadUsername;
		PloneWarsApp.downloadPassword = downloadPassword;
		
		PloneWarsApp.allFiles = allFiles;
		PloneWarsApp.forceDownload = forceDownload;
		PloneWarsApp.defaultViews = defaultViews;
		PloneWarsApp.excludeFromNavs = excludeFromNavs;
		PloneWarsApp.earliestEventDate = earliestEventDate;
		
		PloneWarsApp.uploadVersion = uploadVersion;
		PloneWarsApp.uploadStaging = uploadStaging;
		PloneWarsApp.uploadSite = uploadSite;
		PloneWarsApp.uploadSubfolders = uploadSubfolders;
		PloneWarsApp.uploadUsername = uploadUsername;
		PloneWarsApp.uploadPassword = uploadPassword;
		
		PloneWarsApp.forceUpload = forceUpload;
		PloneWarsApp.removePageExt = removePageExt;
		PloneWarsApp.uri2uid = uri2uid;
		
		retries = 3;
	}
	
	public Void doInBackground() {
		starttime = System.currentTimeMillis();
		

		//out = System.out;
			// logging
		try {
			File logFolder = new File("log");
			logFolder.mkdir();
			out = new PrintStream(new FileOutputStream(new File(logFolder, downloadSite.replace('/',  '-') + ' ' + sdf.format(new Date()) + ".log"), true));
		}
		catch(FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		//String downloadHost = downloadStaging ? "stage.louisville.edu" : "louisville.edu";
		String uploadHost   = uploadStaging   ? "stage.louisville.edu" : "louisville.edu";

		String downloadRoot = (downloadSite.length() > 0) ? downloadSite + '/' : "";
		String downloadBase = downloadRoot;
		if (downloadSubfolders.length() > 0)
			downloadBase += downloadSubfolders + '/';
		
		String uploadRoot = (uploadSite.length() > 0) ? uploadSite + '/' : "";
		String uploadBase = uploadRoot;
		if (uploadSubfolders.length() > 0)
			uploadBase += uploadSubfolders + '/';
		
		String u = '/' + uploadBase;
		String uploadName = u.substring(u.lastIndexOf("/", u.length() - 2) + 1, u.length() - 1);
		
		try {
			downloadRootURI = new URI("http", downloadHost, '/' + downloadRoot, null, null);
			uploadRootURI   = new URI("http", uploadHost,   '/' + uploadRoot,   null, null);
			downloadURI     = new URI("http", downloadHost, '/' + downloadBase, null, null);
			uploadURI       = new URI("http", uploadHost,   '/' + uploadBase,   null, null);

			URI downloadLoginURI = new URI("https", downloadHost, '/' + downloadRoot, null, null);
			URI uploadLoginURI   = new URI("https", uploadHost,   '/' + uploadRoot,   null, null);

			// make sure we can log in before doing work
			if (uploadVersion > 0) {
				uploadCookie = PloneWarsUtils.ploneLogin(uploadLoginURI.resolve("login_form"), uploadUsername, uploadPassword);
				if (uploadCookie == null) {
					out.println("Could not log in. Exiting.");
					return null;
				}
			}
			
			
			rootFolder = new File("sites/" + downloadRoot); //based on download root so we can won't have to redownload sometimes
			if (!rootFolder.exists())
				rootFolder.mkdirs();
			baseFolder = new File(rootFolder, downloadSubfolders + '/');
			if (!baseFolder.exists())
				baseFolder.mkdirs();

			rootPloneFolder = new PloneFolder(downloadSite, null);
			PloneFolder tempFolder = rootPloneFolder;
			if (downloadSubfolders.length() > 0) {
				for (String folderName : downloadSubfolders.split("/")) {
					tempFolder = new PloneFolder(folderName, tempFolder);
				}
			}
			basePloneFolder = tempFolder;

			// hack to make folder switching possible
			basePloneFolder.uploadURI = uploadURI;
			basePloneFolder.name = uploadName;

			out.println("root uri: " + rootPloneFolder.downloadURI);
			out.println("starting at: " + basePloneFolder.downloadURI);


			fileURIs = new LinkedList<URI>();
			pagesWithBrokenURIs = new ArrayList<PlonePage>();
			pagesWithWarningURIs = new ArrayList<PlonePage>();

			// Download
			if (downloadVersion > 0) {
				downloadCookie = PloneWarsUtils.ploneLogin(downloadLoginURI.resolve("login_form"), downloadUsername, downloadPassword);
				if (downloadCookie != null) {
					download();
					PloneWarsUtils.logout(downloadLoginURI);
				}
				else {
					out.println("Could not download: could not log in to Plone " + downloadVersion);
				}
			}
			else {
				out.println("Building from local folder: " + baseFolder.getPath());
				basePloneFolder.build();
			}

			// Upload
			if (uploadVersion > 0) {
				uploadCookie = PloneWarsUtils.ploneLogin(uploadLoginURI.resolve("login_form"), uploadUsername, uploadPassword);
				if (uploadCookie != null) {
					upload();
				}
				else {
					out.println("Could not upload: could not log in to Plone " + uploadVersion);
				}
				
				// default views
				if (defaultViews) {
					LinkedList<PloneFolder> folders = new LinkedList<PloneFolder>();
					folders.add(basePloneFolder);
					folders.addAll(basePloneFolder.subFolders.values());

					while (!folders.isEmpty()) {
						PloneFolder ploneFolder = folders.remove();
						ploneFolder.setDefaultView();
						folders.addAll(ploneFolder.subFolders.values());
					}
				}
				
				// Link conversion
				if (uri2uid) {
					// if (uploadCookie == null)
						// uploadCookie = PloneWarsUtils.ploneLogin(uploadLoginURI.resolve("login_form"), uploadUsername, uploadPassword);

					if (uploadCookie != null) {
						out.println("Updating URIs to resource UIDs");

						LinkedList<PloneFolder> folders = new LinkedList<PloneFolder>();
						LinkedList<PlonePage> pages = new LinkedList<PlonePage>();
						LinkedList<PlonePage> pagesWithErrors = new LinkedList<PlonePage>();

						folders.addAll(basePloneFolder.subFolders.values());
						pages.addAll(basePloneFolder.subPages.values());

						while(!folders.isEmpty()) {
							PloneFolder ploneFolder = folders.remove();

							folders.addAll(ploneFolder.subFolders.values());
							pages.addAll(ploneFolder.subPages.values());
							//break;
						}
						out.println("updating " + pages.size() + " pages to UIDs ... ");
						//pagesToUpload.clear();
						while(!pages.isEmpty()) {
							PlonePage plonePage = pages.remove();
							if (plonePage.updateToResource()) {
								if ((plonePage.upload()) == false)
									pagesWithErrors.add(plonePage);
							}
							setProgress(Math.min(100, 10000 * ++progress / totalProgress));
						}

						out.println("Finished uri2uid with " + pagesWithErrors.size() + " pages with upload errors:");
						for (PlonePage page : pagesWithErrors) {
							out.println("\t" + page.uploadURI.getPath());
						}
						out.println("and " + pagesWithBrokenURIs.size() + " pages with broken URIs");
						for (PlonePage page : pagesWithBrokenURIs) {
							out.println("\t" + page.uploadURI.getPath());
						}
						out.println("and " + pagesWithWarningURIs.size() + " pages with warning URIs");
						for (PlonePage page : pagesWithWarningURIs) {
							out.println("\t" + page.uploadURI.getPath());
						}
					}
					else
						out.println("Could not upload: could not log in to Plone 4.");

				}
			}


		}
		catch(URISyntaxException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	protected void done() {
		Toolkit.getDefaultToolkit().beep();
		firePropertyChange("done", false, true);
		
		double elaspedms = (System.currentTimeMillis() - starttime);
		int seconds = (int)(elaspedms / 1000) % 60;
		int minutes = (int)((elaspedms / (1000 * 60)) % 60);
		int hours = (int)((elaspedms / (1000 * 60 * 60)) % 24);
		StringBuilder elaspedTime = new StringBuilder("Plone Wars ran in ");
		if (hours > 0)
			elaspedTime.append(hours + " hr, ");
		if (minutes > 0)
			elaspedTime.append(minutes + " min, ");
		elaspedTime.append(seconds + " sec.");

		out.println(elaspedTime);
	}

	public void upload() {
		out.println("Uploading " + uploadURI + " to Plone " + uploadVersion);

		LinkedList<PloneFolder> foldersToUpload = new LinkedList<PloneFolder>();
		LinkedList<PlonePage>   pagesToUpload = new LinkedList<PlonePage>();
		LinkedList<PloneField>  fieldsToUpload = new LinkedList<PloneField>();
		LinkedList<PloneTopic>  topicsToUpload = new LinkedList<PloneTopic>();
		LinkedList<PloneFile>   filesToUpload = new LinkedList<PloneFile>();
		LinkedList<PloneFolder> foldersWithErrors = new LinkedList<PloneFolder>();
		LinkedList<PlonePage>   pagesWithErrors = new LinkedList<PlonePage>();
		LinkedList<PloneField>  fieldsWithErrors = new LinkedList<PloneField>();
		LinkedList<PloneTopic>  topicsWithErrors = new LinkedList<PloneTopic>();
		LinkedList<PloneFile>   filesWithErrors = new LinkedList<PloneFile>();

		LinkedList<PloneFile> filesNotUploaded = new LinkedList<PloneFile>();

		foldersToUpload.add(basePloneFolder);
		/*
		 * foldersToUpload.addAll(basePloneFolder.subFolders.values());
		 * pagesToUpload.addAll(basePloneFolder.subPages.values());
		 * fieldsToUpload.addAll(basePloneFolder.subFields.values());
		 * filesToUpload.addAll(basePloneFolder.subFiles.values());
		 */

		out.println("uploading folders ... ");
		while (!foldersToUpload.isEmpty()) {
			PloneFolder ploneFolder = foldersToUpload.remove();
			if ((ploneFolder.upload() && ploneFolder.publish()) == false)
				foldersWithErrors.add(ploneFolder);

			foldersToUpload.addAll(ploneFolder.subFolders.values());
			pagesToUpload.addAll(ploneFolder.subPages.values());
			fieldsToUpload.addAll(ploneFolder.subFields.values());
			topicsToUpload.addAll(ploneFolder.subTopics.values());
			filesToUpload.addAll(ploneFolder.subFiles.values());
			setProgress(Math.min(100, 10000 * ++progress / totalProgress));
			//break;
		}
		out.println("uploading " + pagesToUpload.size() + " pages ... ");
		//pagesToUpload.clear();
		while (!pagesToUpload.isEmpty()) {
			PlonePage plonePage = pagesToUpload.remove();
			if ((plonePage.upload() && plonePage.publish()) == false)
				pagesWithErrors.add(plonePage);
			setProgress(Math.min(100, 10000 * ++progress / totalProgress));
		}

		out.println("uploading " + fieldsToUpload.size() + " form fields ... ");
		//pagesToUpload.clear();
		while(!fieldsToUpload.isEmpty()) {
			PloneField field = fieldsToUpload.remove();
			if (!field.upload())
				fieldsWithErrors.add(field);
			setProgress(Math.min(100, 10000 * ++progress / totalProgress));
		}

		out.println("uploading " + topicsToUpload.size() + " collections ... ");
		//pagesToUpload.clear();
		while(!topicsToUpload.isEmpty()) {
			PloneTopic topic = topicsToUpload.remove();
			if (!topic.upload())
				topicsWithErrors.add(topic);
			setProgress(Math.min(100, 10000 * ++progress / totalProgress));
		}
		out.println("uploading files ... ");
		//filesToUpload.clear();
		while(!filesToUpload.isEmpty()) {
			PloneFile ploneFile = filesToUpload.remove();
			if (allFiles || downloadVersion == 0 || fileURIs.contains(ploneFile.downloadURI)) {
				fileURIs.remove(ploneFile.downloadURI);
				if (ploneFile.upload() == false) //files don't publish in plone 4
					filesWithErrors.add(ploneFile);
			}
			else
				filesNotUploaded.add(ploneFile);
			setProgress(Math.min(100, 10000 * ++progress / totalProgress));
		}

		//errors
		out.println(foldersWithErrors.size() + " folders with upload errors:");
		for (PloneFolder folder : foldersWithErrors) {
			out.println("\t" + folder.uploadURI.getPath());
		}
		out.println(pagesWithErrors.size() + " pages with upload errors:");
		for (PlonePage page : pagesWithErrors) {
			out.println("\t" + page.uploadURI.getPath());
		}
		out.println(fieldsWithErrors.size() + " fields with upload errors:");
		for (PloneField field : fieldsWithErrors) {
			out.println("\t" + field.uploadURI.getPath());
		}
		out.println(topicsWithErrors.size() + " collections with upload errors:");
		for (PloneTopic topic : topicsWithErrors) {
			out.println("\t" + topic.uploadURI.getPath());
		}
		out.println(filesWithErrors.size() + " files with upload errors:");
		for (PloneFile file : filesWithErrors) {
			out.println("\t" + file.uploadURI.getPath());
		}

		out.println(fileURIs.size() + " files were referenced but not found: ");
		for (URI fileURI : fileURIs) {
			out.println("\t" + fileURI);
		}
		out.println(filesNotUploaded.size() + " files were found but not referenced: ");
		for (PloneFile file : filesNotUploaded) {
			out.println("\t" + file.downloadURI);
		}

		out.println("Finished uploading " + uploadURI + " to Plone " + uploadVersion);
	}

	public void download() {
		out.println("Downloading " + downloadURI.getPath());
		
		LinkedList<PlonePage>   pagesWithErrors = new LinkedList<PlonePage>();
		
			LinkedList<PloneFolder> foldersToLoad = new LinkedList<PloneFolder>();
			LinkedList<PlonePage> pagesToLoad = new LinkedList<PlonePage>();
			LinkedList<PloneField> fieldsToLoad = new LinkedList<PloneField>();
			LinkedList<PloneTopic> topicsToLoad = new LinkedList<PloneTopic>();
			LinkedList<PloneFile> filesToLoad = new LinkedList<PloneFile>();
			foldersToLoad.add(basePloneFolder);
			
			out.println("gathering workload data");
			ListIterator<PloneFolder> i = foldersToLoad.listIterator();
			while(i.hasNext()) {
				PloneFolder folder = i.next();
				
			}

			out.println("downloading folders ... ");
			while(!foldersToLoad.isEmpty()) {
				PloneFolder ploneFolder = foldersToLoad.remove();
				ploneFolder.download();
				ploneFolder.save();
				foldersToLoad.addAll(ploneFolder.subFolders.values());
				pagesToLoad.addAll(ploneFolder.subPages.values());
				fieldsToLoad.addAll(ploneFolder.subFields.values());
				topicsToLoad.addAll(ploneFolder.subTopics.values());
				filesToLoad.addAll(ploneFolder.subFiles.values());
				progress++;
			}
			int multiplier = 1;
			int pageMultiplier = 1;
			if (uploadVersion > 0)
				multiplier *= 2;
			if (uri2uid)
				pageMultiplier *= 2;
			totalProgress = 
					progress +
					pagesToLoad.size() * multiplier * pageMultiplier +
					fieldsToLoad.size() * multiplier +
					topicsToLoad.size() * multiplier +
					filesToLoad.size() * multiplier;
			totalProgress *= 100;
			setProgress(Math.min(100, 10000 * progress / totalProgress));
			out.println("downloading pages ... ");
			while(!pagesToLoad.isEmpty()) {
				PlonePage plonePage = pagesToLoad.remove();
				if (plonePage.download())
					plonePage.save();
				else
					pagesWithErrors.add(plonePage);
				setProgress(Math.min(100, 10000 * ++progress / totalProgress));
			}
			out.println("downloading form fields ...");
			while(!fieldsToLoad.isEmpty()) {
				PloneField field = fieldsToLoad.remove();
				field.download();
				field.save();
				setProgress(Math.min(100, 10000 * ++progress / totalProgress));
			}
			out.println("downloading collections ...");
			while(!topicsToLoad.isEmpty()) {
				PloneTopic topic = topicsToLoad.remove();
				topic.download();
				topic.save();
				setProgress(Math.min(100, 10000 * ++progress / totalProgress));
			}

			out.println("downloading files ... ");
			//out.println(fileURIs);
			//LinkedList<URI> temp = fileURIs;
			out.println(fileURIs);
			while(!filesToLoad.isEmpty()) {
				PloneFile ploneFile = filesToLoad.remove();
				if (allFiles || fileURIs.contains(ploneFile.downloadURI)) {
					ploneFile.download();
					ploneFile.save();
				}
				setProgress(Math.min(100, 1000 * ++progress / totalProgress));
			}
		
		out.println("Finished downloading " + downloadURI.getPath());

		out.println(pagesWithErrors.size() + " pages with download errors:");
		for (PlonePage page : pagesWithErrors) {
			out.println("\t" + page.downloadURI.getPath());
		}
	}
}
