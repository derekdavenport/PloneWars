package PloneWars;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.EventQueue;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.text.*;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.awt.Toolkit;

import net.miginfocom.swing.MigLayout;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;
import java.awt.FlowLayout;
import java.awt.Component;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.awt.Dimension;

public class PFrame extends JFrame {
	

	private JPanel	contentPane;
	
	private String title = "Plone Wars";
	
	private JFileChooser fileChooser;
	private File defaultSaveDir = new File("saves/");
	private File currentSaveFile = null;

	private JComboBox downloadSourceComboBox;
	private JCheckBox downloadStagingCheckbox;

	private JComboBox  downloadHostComboBox;
	private JTextField downloadSiteRootField;
	private JTextField downloadSubfolderField;
	private JTextField downloadSiteDisplayField;

	private JLabel downloadUsernameLabel;
	private JTextField downloadUsernameField;
	private JLabel downloadPasswordLabel;
	private JPasswordField downloadPasswordField;

	private JComboBox downloadFilesComboBox;
	private JCheckBox downloadIgnoreCacheCheckbox;
	private JCheckBox downloadDefaultViewsCheckbox;
	private JCheckBox downloadExcludeFromNavCheckbox;
	private JTextField downloadEventsAfterField;

	private JLabel uploadSourceLabel;
	private JComboBox uploadSourceComboBox;
	private JCheckBox uploadStagingCheckbox;
	private JCheckBox uploadSameSiteCheckbox;
	private JCheckBox uploadSameLoginCheckbox;

	private JLabel uploadSiteRootLabel;
	private JTextField uploadSiteRootField;
	private JLabel uploadSubfolderLabel;
	private JTextField uploadSubfolderField;
	private JTextField uploadSiteDisplayField;

	private JLabel uploadUsernameLabel;
	private JTextField uploadUsernameField;
	private JLabel uploadPasswordLabel;
	private JPasswordField uploadPasswordField;

	private JLabel uploadFilesLabel;
	private JComboBox uploadFilesComboBox;
	private JCheckBox uploadRemoveExtensionsCheckbox;
	private JCheckBox uploadURItoUIDCheckbox;
	
	private JButton runButton;
	private JProgressBar runProgressBar;
	private JLabel downloadFilesLabel;
	private JMenuBar menuBar;
	private JMenu mnFile;
	private JMenuItem mntmSave;
	private JMenuItem mntmSaveAs;
	private JMenuItem mntmNew;
	private JMenuItem mntmOpen;
	private JSeparator fileSeparator1;
	private JSeparator fileSeparator2;
	private JPanel downloadPanel;

	/**
	 * Create the frame.
	 */
	public PFrame() {
		// setIconImage();

		setTitle("Untitled - " + title);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 555, 400);
		
		fileChooser = new JFileChooser();
		if(!defaultSaveDir.exists()) {
			defaultSaveDir.mkdirs();
		}
		fileChooser.setCurrentDirectory(defaultSaveDir);
		fileChooser.setFileFilter(new FileFilter() {
			public boolean accept(File f) {
		    if (f.isDirectory()) {
		        return true;
		    }
		    
		    if(f.getName().endsWith(".cfg.json")) {
		    	return true;
		    }

		    return false;
			}
			public String getDescription() {
				return "Plone Wars config files ";
			}
		});
		
		menuBar = new JMenuBar();
		setJMenuBar(menuBar);
		
		mnFile = new JMenu("File");
		menuBar.add(mnFile);
		
		mntmNew = new JMenuItem("New");
		mnFile.add(mntmNew);
		
		fileSeparator1 = new JSeparator();
		mnFile.add(fileSeparator1);
		
		mntmOpen = new JMenuItem("Open");
		mntmOpen.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				open();
			}
		});
		mnFile.add(mntmOpen);
		
		fileSeparator2 = new JSeparator();
		mnFile.add(fileSeparator2);
		
		mntmSave = new JMenuItem("Save");
		mntmSave.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				save();
			}
		});
		mnFile.add(mntmSave);
		
		mntmSaveAs = new JMenuItem("Save as");
		mntmSaveAs.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				saveAs();
			}
		});
		mnFile.add(mntmSaveAs);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
		
		JPanel settingsPanel = new JPanel();
		contentPane.add(settingsPanel);
		settingsPanel.setLayout(new BoxLayout(settingsPanel, BoxLayout.LINE_AXIS));
		
		downloadPanel = new JPanel();
		settingsPanel.add(downloadPanel);
		downloadPanel.setBorder(new TitledBorder(null, "Download Settings", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		downloadPanel.setLayout(new MigLayout("", "[][grow]", "[][][][][][][][][][][][]"));
		
		JLabel downloadSourceLabel = new JLabel("Source");
		downloadPanel.add(downloadSourceLabel, "flowx,cell 0 0 2 1,alignx left");
		
		downloadSourceComboBox = new JComboBox();
		downloadSourceComboBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				//JComboBox downloadSourceComboBox = (JComboBox)e.getSource();
				switch((String)downloadSourceComboBox.getSelectedItem()) {
					case "local cache":
						downloadStagingCheckbox.setEnabled(false);
						setDownloadSiteDisplayFieldValue();
						downloadUsernameLabel.setEnabled(false);
						downloadUsernameField.setEnabled(false);
						downloadPasswordField.setEnabled(false);
						downloadPasswordLabel.setEnabled(false);
						uploadSameLoginCheckbox.setEnabled(false);
						uploadSameLoginCheckbox.setSelected(false);
						setUploadSameLogin();
						downloadFilesLabel.setEnabled(false);
						downloadFilesComboBox.setEnabled(false);
						downloadIgnoreCacheCheckbox.setEnabled(false);
						downloadDefaultViewsCheckbox.setEnabled(false);
						downloadExcludeFromNavCheckbox.setEnabled(false);
						break;
					case "Plone 3":
					case "Plone 4":
						downloadStagingCheckbox.setEnabled(true);
						setDownloadSiteDisplayFieldValue();
						downloadUsernameLabel.setEnabled(true);
						downloadUsernameField.setEnabled(true);
						downloadPasswordField.setEnabled(true);
						downloadPasswordLabel.setEnabled(true);
						uploadSameLoginCheckbox.setEnabled(uploadSameSiteCheckbox.isEnabled());
						setUploadSameLogin();
						downloadFilesLabel.setEnabled(true);
						downloadFilesComboBox.setEnabled(true);
						downloadIgnoreCacheCheckbox.setEnabled(true);
						downloadDefaultViewsCheckbox.setEnabled(true);
						downloadExcludeFromNavCheckbox.setEnabled(true);
						break;
					default:
						System.out.println(downloadSourceComboBox.getSelectedItem());
				}
			}
		});
		downloadSourceLabel.setLabelFor(downloadSourceComboBox);
		downloadSourceComboBox.setModel(new DefaultComboBoxModel(new String[] {"Plone 3", "Plone 4", "local cache"}));
		downloadPanel.add(downloadSourceComboBox, "cell 0 0 2 1,growx");
		
		downloadStagingCheckbox = new JCheckBox("Staging");
		downloadStagingCheckbox.setName("downloadStagingCheckbox");
		downloadStagingCheckbox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setDownloadSiteDisplayFieldValue();
			}
		});
		downloadStagingCheckbox.setToolTipText("If the download site is in staging or not");
		downloadPanel.add(downloadStagingCheckbox, "flowx,cell 0 1 2 1,alignx left");
		

		downloadHostComboBox = new JComboBox();
		downloadHostComboBox.setName("downloadHostComboBox");
		downloadHostComboBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setDownloadSiteDisplayFieldValue();
			}
		});
		downloadHostComboBox.setModel(new DefaultComboBoxModel(new String[] {"louisville.edu", "stage.louisville.edu"}));
		downloadHostComboBox.setEditable(true);
		downloadPanel.add(downloadHostComboBox, "cell 1 1,growx");
		
		JLabel downloadSiteRootLabel = new JLabel("Site root");
		downloadPanel.add(downloadSiteRootLabel, "cell 0 2,alignx trailing");
		
		/*
		 * 
				String text = downloadSiteRootField.getText().replaceAll("^/+", "").replaceAll("/+$", "");
				downloadSiteRootField.setText(text);
				setDownloadSiteDisplayFieldValue();
		 */

		
		downloadSiteRootField = new JTextField();
		/*
		downloadSiteRootField.addPropertyChangeListener("value", new PropertyChangeListener() {
	    public void propertyChange(PropertyChangeEvent e){
	    	try {
					downloadSiteRootField.commitEdit();
				}
				catch(ParseException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
	    	System.out.println(e.getOldValue() + ", " + e.getNewValue() + ", " + downloadSiteRootField.getText());
	    	//setDownloadSiteDisplayFieldValue();
	    }
	});
	*/
		downloadSiteRootField.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				downloadSiteRootField.setText(downloadSiteRootField.getText().replaceAll("^/+", "").replaceAll("/+$", ""));
				setDownloadSiteDisplayFieldValue();
			}
		});
		downloadSiteRootField.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				downloadSiteRootField.setText(downloadSiteRootField.getText().replaceAll("^/+", "").replaceAll("/+$", ""));
				setDownloadSiteDisplayFieldValue();
			}
		});
		/* */
		downloadSiteRootLabel.setLabelFor(downloadSiteRootField);
		downloadPanel.add(downloadSiteRootField, "cell 1 2,growx");
		downloadSiteRootField.setColumns(10);
		
		JLabel downloadSubfolderLabel = new JLabel("Subfolder");
		downloadPanel.add(downloadSubfolderLabel, "cell 0 3,alignx trailing");
		
		downloadSubfolderField = new JTextField();
		downloadSubfolderField.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				downloadSubfolderField.setText(downloadSubfolderField.getText().replaceAll("^/+", "").replaceAll("/+$", ""));
				setDownloadSiteDisplayFieldValue();
			}
		});
		downloadSubfolderField.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				downloadSubfolderField.setText(downloadSubfolderField.getText().replaceAll("^/+", "").replaceAll("/+$", ""));
				setDownloadSiteDisplayFieldValue();
			}
		});
		downloadSubfolderLabel.setLabelFor(downloadSubfolderField);
		downloadPanel.add(downloadSubfolderField, "cell 1 3,growx");
		downloadSubfolderField.setColumns(10);
		
		downloadSiteDisplayField = new JTextField();
		downloadSiteDisplayField.setText("http://louisville.edu/");
		downloadSiteDisplayField.setEditable(false);
		downloadPanel.add(downloadSiteDisplayField, "cell 0 4 2 1,growx");
		downloadSiteDisplayField.setColumns(10);
		
		downloadUsernameLabel = new JLabel("Username");
		downloadPanel.add(downloadUsernameLabel, "cell 0 5,alignx trailing");
		
		downloadUsernameField = new JTextField();
		downloadUsernameLabel.setLabelFor(downloadUsernameField);
		downloadPanel.add(downloadUsernameField, "cell 1 5,growx");
		downloadUsernameField.setColumns(10);
		
		downloadPasswordLabel = new JLabel("Password");
		downloadPanel.add(downloadPasswordLabel, "cell 0 6,alignx trailing");
		
		downloadPasswordField = new JPasswordField();
		downloadPasswordLabel.setLabelFor(downloadPasswordField);
		downloadPanel.add(downloadPasswordField, "cell 1 6,growx");
		downloadPasswordField.setColumns(10);
		
		JSeparator downloadSeparator1 = new JSeparator();
		downloadPanel.add(downloadSeparator1, "cell 0 7 2 1,growx");
		
		downloadFilesLabel = new JLabel("Files");
		downloadPanel.add(downloadFilesLabel, "cell 0 8,alignx trailing");
		
		downloadFilesComboBox = new JComboBox();
		downloadFilesLabel.setLabelFor(downloadFilesComboBox);
		downloadFilesComboBox.setModel(new DefaultComboBoxModel(new String[] {"Used files", "All files"}));
		downloadFilesComboBox.setSelectedIndex(0);
		downloadPanel.add(downloadFilesComboBox, "cell 1 8,growx");
		
		downloadIgnoreCacheCheckbox = new JCheckBox("Ignore cache");
		downloadIgnoreCacheCheckbox.setToolTipText("If unchecked, pages that have not changed recently will not be redownloaded.");
		downloadPanel.add(downloadIgnoreCacheCheckbox, "cell 0 9 2 1");
		
		downloadDefaultViewsCheckbox = new JCheckBox("Default views");
		downloadDefaultViewsCheckbox.setSelected(true);
		downloadPanel.add(downloadDefaultViewsCheckbox, "flowx,cell 0 10 2 1");
		
		JLabel downloadEventsAfterLabel = new JLabel("Events after");
		downloadPanel.add(downloadEventsAfterLabel, "cell 0 11,alignx trailing");
		
		downloadEventsAfterField = new JTextField();
		downloadEventsAfterLabel.setLabelFor(downloadEventsAfterField);
		downloadEventsAfterField.setText("2000/01/01");
		downloadPanel.add(downloadEventsAfterField, "cell 1 11,growx");
		downloadEventsAfterField.setColumns(10);
		
		downloadExcludeFromNavCheckbox = new JCheckBox("Exclude from nav");
		downloadExcludeFromNavCheckbox.setSelected(true);
		downloadPanel.add(downloadExcludeFromNavCheckbox, "flowx,cell 0 10 2 1");
		
		JPanel uploadPanel = new JPanel();
		settingsPanel.add(uploadPanel);
		uploadPanel.setBorder(new TitledBorder(null, "Upload Settings", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		uploadPanel.setLayout(new MigLayout("", "[][grow]", "[][][][][][][][][][][]"));
		
		uploadSourceLabel = new JLabel("Destination");
		uploadPanel.add(uploadSourceLabel, "flowx,cell 0 0 2 1,alignx left");
		
		uploadSourceComboBox = new JComboBox();
		uploadSourceLabel.setLabelFor(uploadSourceComboBox);
		uploadSourceComboBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				boolean enabled = (uploadSourceComboBox.getSelectedItem() != "none");
				uploadStagingCheckbox.setEnabled(enabled);
				uploadSameSiteCheckbox.setEnabled(enabled);
				uploadSameLoginCheckbox.setEnabled(enabled);// && !downloadSourceComboBox.getSelectedItem().equals("local"));

				//uploadSiteRootLabel.setEnabled(enabled);
				//uploadSiteRootField.setEnabled(enabled);
				//uploadSubfolderLabel.setEnabled(enabled);
				//uploadSubfolderField.setEnabled(enabled);
				setUploadSameSite();
				setUploadSameLogin();
				
				uploadFilesLabel.setEnabled(enabled);
				uploadFilesComboBox.setEnabled(enabled);
				uploadRemoveExtensionsCheckbox.setEnabled(enabled);
				uploadURItoUIDCheckbox.setEnabled(enabled);
				
			}
		});
		uploadSourceComboBox.setModel(new DefaultComboBoxModel(new String[] {"Plone 4", "none"}));
		uploadPanel.add(uploadSourceComboBox, "cell 0 0 2 1,growx");
		
		uploadStagingCheckbox = new JCheckBox("Staging");
		uploadStagingCheckbox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setUploadSiteDisplayFieldValue();
			}
		});
		uploadStagingCheckbox.setSelected(true);
		uploadPanel.add(uploadStagingCheckbox, "cell 0 1 2 1,alignx left");
		
		uploadSameSiteCheckbox = new JCheckBox("Same site");
		uploadSameSiteCheckbox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setUploadSameSite();
			}
		});
		uploadSameSiteCheckbox.setSelected(true);
		uploadSameSiteCheckbox.setToolTipText("Use the same site information as the download.");
		uploadPanel.add(uploadSameSiteCheckbox, "flowx,cell 0 1 2 1,alignx left");
		
		uploadSameLoginCheckbox = new JCheckBox("Same login");
		uploadSameLoginCheckbox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setUploadSameLogin();
			}
		});
		uploadSameLoginCheckbox.setSelected(true);
		uploadSameLoginCheckbox.setToolTipText("Use the same login information as the download.");
		uploadPanel.add(uploadSameLoginCheckbox, "cell 0 1 2 1,alignx left");
		
		uploadSiteRootLabel = new JLabel("Site root");
		uploadSiteRootLabel.setEnabled(false);
		uploadPanel.add(uploadSiteRootLabel, "cell 0 2,alignx trailing");
		
		uploadSiteRootField = new JTextField();
		uploadSiteRootField.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				uploadSiteRootField.setText(uploadSiteRootField.getText().replaceAll("^/+", "").replaceAll("/+$", ""));
				setUploadSiteDisplayFieldValue();
			}
		});
		uploadSiteRootField.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				uploadSiteRootField.setText(uploadSiteRootField.getText().replaceAll("^/+", "").replaceAll("/+$", ""));
				setUploadSiteDisplayFieldValue();
			}
		});
		uploadSiteRootLabel.setLabelFor(uploadSiteRootField);
		uploadSiteRootField.setEnabled(false);
		uploadPanel.add(uploadSiteRootField, "cell 1 2,growx");
		uploadSiteRootField.setColumns(10);
		
		uploadSubfolderLabel = new JLabel("Subfolder");
		uploadSubfolderLabel.setEnabled(false);
		uploadPanel.add(uploadSubfolderLabel, "cell 0 3,alignx trailing");
		
		uploadSubfolderField = new JTextField();
		uploadSubfolderField.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				uploadSubfolderField.setText(uploadSubfolderField.getText().replaceAll("^/+", "").replaceAll("/+$", ""));
				setUploadSiteDisplayFieldValue();
			}
		});
		uploadSubfolderField.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				uploadSubfolderField.setText(uploadSubfolderField.getText().replaceAll("^/+", "").replaceAll("/+$", ""));
				setUploadSiteDisplayFieldValue();
			}
		});
		uploadSubfolderLabel.setLabelFor(uploadSubfolderField);
		uploadSubfolderField.setEnabled(false);
		uploadPanel.add(uploadSubfolderField, "cell 1 3,growx");
		uploadSubfolderField.setColumns(10);
		
		uploadSiteDisplayField = new JTextField();
		uploadSiteDisplayField.setText("http://stage.louisville.edu/");
		uploadSiteDisplayField.setEditable(false);
		uploadPanel.add(uploadSiteDisplayField, "cell 0 4 2 1,growx");
		uploadSiteDisplayField.setColumns(10);
		
		uploadUsernameLabel = new JLabel("Username");
		uploadUsernameLabel.setEnabled(false);
		uploadPanel.add(uploadUsernameLabel, "cell 0 5,alignx trailing");
		
		uploadUsernameField = new JTextField();
		uploadUsernameLabel.setLabelFor(uploadUsernameField);
		uploadUsernameField.setEnabled(false);
		uploadPanel.add(uploadUsernameField, "cell 1 5,growx");
		uploadUsernameField.setColumns(10);
		
		uploadPasswordLabel = new JLabel("Password");
		uploadPasswordLabel.setEnabled(false);
		uploadPanel.add(uploadPasswordLabel, "cell 0 6,alignx trailing");
		
		uploadPasswordField = new JPasswordField();
		uploadPasswordLabel.setLabelFor(uploadPasswordField);
		uploadPasswordField.setEnabled(false);
		uploadPanel.add(uploadPasswordField, "cell 1 6,growx");
		uploadPasswordField.setColumns(10);
		
		JSeparator uploadSeparator = new JSeparator();
		uploadPanel.add(uploadSeparator, "cell 0 7 2 1,growx");
		
		uploadFilesLabel = new JLabel("Files");
		uploadPanel.add(uploadFilesLabel, "cell 0 8,alignx trailing");
		
		uploadFilesComboBox = new JComboBox();
		uploadFilesLabel.setLabelFor(uploadFilesComboBox);
		uploadFilesComboBox.setModel(new DefaultComboBoxModel(new String[] {"New files", "All files"}));
		uploadFilesComboBox.setSelectedIndex(0);
		uploadPanel.add(uploadFilesComboBox, "cell 1 8,growx");
		
		uploadRemoveExtensionsCheckbox = new JCheckBox("Remove extensions");
		uploadRemoveExtensionsCheckbox.setToolTipText("Removes extensions from the end of file names. Right now this only works for pages ending in '.html'.");
		uploadPanel.add(uploadRemoveExtensionsCheckbox, "cell 0 9 2 1");
		
		uploadURItoUIDCheckbox = new JCheckBox("Convert URIs to UIDs");
		uploadURItoUIDCheckbox.setSelected(true);
		uploadURItoUIDCheckbox.setToolTipText("Convert all URIs in links and images to use Plone 4's Resource IDs.");
		uploadPanel.add(uploadURItoUIDCheckbox, "cell 0 10 2 1");
		
		JPanel runPanel = new JPanel();
		runPanel.setAlignmentY(Component.TOP_ALIGNMENT);
		contentPane.add(runPanel);
		runPanel.setLayout(new BoxLayout(runPanel, BoxLayout.LINE_AXIS));
		
		runButton = new JButton("Run");
		PFrame that = this;
		runButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				runButton.setEnabled(false);
				runProgressBar.setEnabled(true);
				PloneWarsApp task = new PloneWarsApp(
					getDownloadVersion(),
					getDownloadHost(),
					isDownloadStaging(),
					getDownloadSiteRoot(),
					getDownloadSubfolder(),
					getDownloadUsername(),
					getDownloadPassword(),
					
					isAllFiles(), //if false, only downloads images/files that are used/linked to on pages
					isForceDownload(), //ignore caching
					isDefaultViews(),
					isExcludeFromNav(),

					getEarliestEventDate(), //TODO: seems to be ignoring events older than 3 years, not 10

					getUploadVersion(),
					isUploadStaging(),
					getUploadSiteRoot(),
					getUploadSubfolder(),
					getUploadUsername(),
					getUploadPassword(),
					
					isForceUpload(),
					isRemoveExt(),
					isURItoUID()
				);
				task.addPropertyChangeListener(new PropertyChangeListener() {
					public void propertyChange(PropertyChangeEvent evt) {
						if("progress" == evt.getPropertyName()) {
							int progress = (Integer)evt.getNewValue();
							runProgressBar.setIndeterminate(false);
							runProgressBar.setValue(progress);
						}
						else if("done" == evt.getPropertyName()) {
							runProgressBar.setEnabled(false);
							runButton.setEnabled(true);
						}
					}
				});
				runProgressBar.setIndeterminate(true);
				task.execute();
			}
		});
		runPanel.add(runButton);
		
		runProgressBar = new JProgressBar(0, 100);
		runProgressBar.setEnabled(false);
		runProgressBar.setSize(new Dimension(146, 23));
		runProgressBar.setPreferredSize(new Dimension(146, 23));
		runPanel.add(runProgressBar);
	}
	
	public void enableRunButton() {
		runButton.setEnabled(true);
	}
	
	private class PathFormatter extends DefaultFormatter {
		public PathFormatter() {
			setOverwriteMode(false);
		}
		@Override
    public Object stringToValue(String string) throws ParseException {
      return string.replaceAll("^/+", "").replaceAll("/+$", "");
    }
	}
	
	private void setDownloadSiteDisplayFieldValue() {
			//String staging = downloadStagingCheckbox.isSelected() ? "stage." : "";
			String start   = downloadStagingCheckbox.isEnabled()  ? "http://" + downloadHostComboBox.getSelectedItem().toString() + "/" : "[local]";// + staging + "louisville.edu/" : "[local]/";
			String root    = downloadSiteRootField.getText().length()  > 0 ? downloadSiteRootField.getText()  + '/' : "";
			String sub     = downloadSubfolderField.getText().length() > 0 ? downloadSubfolderField.getText() + '/' : "";
			downloadSiteDisplayField.setText(start + root + sub);
			setUploadSameSite();
	}
	
	private void setUploadSiteDisplayFieldValue() {
		if(uploadSameSiteCheckbox.isEnabled()) {
			uploadSiteDisplayField.setEnabled(true);
			String staging = uploadStagingCheckbox.isSelected() ? "stage." : "";
			String start   = "http://" + staging + "louisville.edu/";
			String root    = uploadSiteRootField.getText().length()  > 0 ? uploadSiteRootField.getText()  + '/' : "";
			String sub     = uploadSubfolderField.getText().length() > 0 ? uploadSubfolderField.getText() + '/' : "";
			uploadSiteDisplayField.setText(start + root + sub);
		}
		else {
			uploadSiteDisplayField.setEnabled(false);
			uploadSiteDisplayField.setText("");
		}
		//setUploadSameSite();
}
	
	private void setUploadSameSite() {
		if(uploadSameSiteCheckbox.isEnabled()) {
			if(uploadSameSiteCheckbox.isSelected()) {
				uploadSiteRootLabel.setEnabled(false);
				uploadSiteRootField.setEnabled(false);
				uploadSubfolderLabel.setEnabled(false);
				uploadSubfolderField.setEnabled(false);
				uploadSiteRootField.setText(downloadSiteRootField.getText());
				uploadSubfolderField.setText(downloadSubfolderField.getText());
				//uploadSiteDisplayField.setText(downloadSiteDisplayField.getText());
				setUploadSiteDisplayFieldValue();
			}
			else {
				uploadSiteRootLabel.setEnabled(true);
				uploadSiteRootField.setEnabled(true);
				uploadSubfolderLabel.setEnabled(true);
				uploadSubfolderField.setEnabled(true);
				//setUploadSiteDisplayFieldValue();
			}
		}
		else {
			//uploadSiteRootLabel.setEnabled(false);
			//uploadSiteRootField.setEnabled(false);
			//uploadSubfolderLabel.setEnabled(false);
			//uploadSubfolderField.setEnabled(false);
			uploadSiteRootField.setText("");
			uploadSubfolderField.setText("");
			setUploadSiteDisplayFieldValue();
		}
	}
	
	private void setUploadSameLogin() {
		if(uploadSameSiteCheckbox.isEnabled()) {
			if(uploadSameLoginCheckbox.isEnabled() && uploadSameLoginCheckbox.isSelected()) {
				uploadUsernameLabel.setEnabled(false);
				uploadUsernameField.setEnabled(false);
				uploadPasswordLabel.setEnabled(false);
				uploadPasswordField.setEnabled(false);
				uploadUsernameField.setText(downloadUsernameField.getText());
				uploadPasswordField.setText(new String(downloadPasswordField.getPassword()));
			}
			else {
				uploadUsernameLabel.setEnabled(true);
				uploadUsernameField.setEnabled(true);
				uploadPasswordLabel.setEnabled(true);
				uploadPasswordField.setEnabled(true);
			}
		}
		else {
			uploadUsernameLabel.setEnabled(false);
			uploadUsernameField.setEnabled(false);
			uploadPasswordLabel.setEnabled(false);
			uploadPasswordField.setEnabled(false);
			uploadUsernameField.setText("");
			uploadPasswordField.setText("");
		}
	}
	
	public short getDownloadVersion() {
		short version = 0;
		switch((String)downloadSourceComboBox.getSelectedItem()) {
			case "Plone 3":
				version = 3;
				break;
			case "Plone 4":
				version = 4;
				break;
		}
		return version;
	}
	
	public String getDownloadHost() {
		return downloadHostComboBox.getSelectedItem().toString();
	}
	
	public boolean isDownloadStaging() {
		return downloadStagingCheckbox.isSelected();
	}
	
	public String getDownloadSiteRoot() {
		return downloadSiteRootField.getText();
	}
	
	public String getDownloadSubfolder() {
		return downloadSubfolderField.getText();
	}
	
	public String getDownloadUsername() {
		return downloadUsernameField.getText();
	}
	
	public String getDownloadPassword() {
		return new String(downloadPasswordField.getPassword());
	}
	
	public boolean isAllFiles() {
		return downloadFilesComboBox.getSelectedItem().equals("All files");
	}
	
	public boolean isForceDownload() {
		return downloadIgnoreCacheCheckbox.isSelected();
	}
	
	public boolean isDefaultViews() {
		return downloadDefaultViewsCheckbox.isSelected();
	}
	
	public boolean isExcludeFromNav() {
		return downloadExcludeFromNavCheckbox.isSelected();
	}
	
	public Date getEarliestEventDate() {
		Date d = null;
		try {
			d = PloneObject.PLONE_DAY_FORMAT.parse(downloadEventsAfterField.getText());
		}
		catch(ParseException e) {
			// this should really never happen because the field is validated
			e.printStackTrace();
		}
		return d;
	}
	
	
	// upload gets
	public short getUploadVersion() {
		short version = 0;
		switch((String)uploadSourceComboBox.getSelectedItem()) {
			case "Plone 3":
				version = 3;
				break;
			case "Plone 4":
				version = 4;
				break;
		}
		return version;
	}
	
	public boolean isUploadStaging() {
		return uploadStagingCheckbox.isSelected();
	}
	
	public String getUploadSiteRoot() {
		return uploadSiteRootField.getText();
	}
	
	public String getUploadSubfolder() {
		return uploadSubfolderField.getText();
	}
	
	public String getUploadUsername() {
		return uploadUsernameField.getText();
	}
	
	public String getUploadPassword() {
		return new String(uploadPasswordField.getPassword());
	}
	
	public boolean isForceUpload() {
		return uploadFilesComboBox.getSelectedItem().equals("All files");
	}
	
	public boolean isRemoveExt() {
		return uploadRemoveExtensionsCheckbox.isSelected();
	}
	
	public boolean isURItoUID() {
		return uploadURItoUIDCheckbox.isSelected();
	}
	
	private void open() {
		fileChooser.setDialogType(JFileChooser.OPEN_DIALOG);
		fileChooser.setDialogTitle("Open");

		fileChooser.setSelectedFile(null);
		int returnVal = fileChooser.showOpenDialog(this);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			currentSaveFile = fileChooser.getSelectedFile();

			JSONParser jsonParser = new JSONParser();
			try {
				@SuppressWarnings("unchecked")
				Map<String, Object> json = (Map<String, Object>) jsonParser.parse(new FileReader(currentSaveFile));
				LinkedHashMap<String, Object> orderedJson = new LinkedHashMap<String, Object>();

				String[] order = new String[] { "downloadSourceComboBox", "uploadStagingCheckbox", "uploadSameSiteCheckbox", "uploadSourceComboBox", "uploadSameLoginCheckbox" };
				
				for(String key : order) {
					orderedJson.put(key, json.remove(key));
				}
				orderedJson.putAll(json);
				
				for (Map.Entry<String, Object> entry : orderedJson.entrySet()) {
					try {
						Field field = PFrame.class.getDeclaredField(entry.getKey());
						if (JTextField.class.isAssignableFrom(field.getType())) {
							JTextField tf = (JTextField) field.get(this);
							tf.setText((String) entry.getValue());
						} else if (JComboBox.class.isAssignableFrom(field.getType())) {
							JComboBox cb = (JComboBox) field.get(this);
							cb.setSelectedItem((String) entry.getValue());
						} else if (JCheckBox.class.isAssignableFrom(field.getType())) {
							JCheckBox cb = (JCheckBox) field.get(this);
							cb.setSelected((boolean) entry.getValue());
						}
					} catch (NoSuchFieldException | SecurityException
							| IllegalArgumentException | IllegalAccessException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			} catch (IOException | org.json.simple.parser.ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			setTitle(currentSaveFile.getName() + " - Plone Wars");
		}
	}
	
	private void saveAs() {
		fileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
		fileChooser.setDialogTitle("Save As");

		File suggestedFile = null;
		/*
		 * if(currentSaveFile != null) { suggestedFile = currentSaveFile; } else
		 */
		{
			String suggestedFileName = null;
			if (downloadSiteRootField.getText().length() > 0) {
				suggestedFileName = downloadSiteRootField.getText().replace('/', '-');
				if (downloadSubfolderField.getText().length() > 0) {
					suggestedFileName += '-' + downloadSubfolderField.getText().replace('/', '-');
				}
			}
			else {
				suggestedFileName = "*";
			}
			suggestedFile = new File(defaultSaveDir, suggestedFileName + ".cfg.json");
		}

		fileChooser.setSelectedFile(suggestedFile);
		int returnVal = fileChooser.showSaveDialog(this);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			currentSaveFile = fileChooser.getSelectedFile();
			if (!currentSaveFile.getName().endsWith(".cfg.json")) {
				currentSaveFile = new File(currentSaveFile, currentSaveFile.getName() + ".cfg.json");
			}
			save();
		}
	}
	
	private void save() {
		if(currentSaveFile == null) {
			saveAs();
			return;
		}
		
		JSONObject json = new JSONObject();
		Field[] fields = PFrame.class.getDeclaredFields();
		for(Field field : fields) {
			try {
				if(JTextField.class.isAssignableFrom(field.getType())) {
					JTextField tf = (JTextField)field.get(this);
					json.put(field.getName(), tf.getText());
				}
				else if(JComboBox.class.isAssignableFrom(field.getType())) {
					JComboBox cb = (JComboBox)field.get(this);
					json.put(field.getName(), cb.getSelectedItem());
				}
				else if(JCheckBox.class.isAssignableFrom(field.getType())) {
					JCheckBox cb = (JCheckBox)field.get(this);
					json.put(field.getName(), cb.isSelected());
				}
			}
			catch(IllegalArgumentException | IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		try {
			FileWriter fw = new FileWriter(currentSaveFile);
			json.writeJSONString(fw);
			
			fw.close();
			setTitle(currentSaveFile.getName() + " - Plone Wars");
			
		}
		catch(IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
  public static void main(String[] args) {
    //Schedule a job for the event-dispatching thread:
    //creating and showing this application's GUI.
    javax.swing.SwingUtilities.invokeLater(new Runnable() {
        public void run() {
					try {
						UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
						PFrame frame = new PFrame();
						frame.setVisible(true);
					}
					catch(ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
						e.printStackTrace();
					}
        }
    });
}
}
