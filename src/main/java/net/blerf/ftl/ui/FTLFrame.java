package net.blerf.ftl.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.font.LineMetrics;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.filechooser.FileFilter;
import javax.xml.bind.JAXBException;

import net.vhati.modmanager.core.FTLUtilities;

import net.blerf.ftl.model.Profile;
import net.blerf.ftl.parser.DataManager;
import net.blerf.ftl.parser.MysteryBytes;
import net.blerf.ftl.parser.ProfileParser;
import net.blerf.ftl.parser.SavedGameParser;
import net.blerf.ftl.ui.ExtensionFileFilter;
import net.blerf.ftl.ui.GeneralAchievementsPanel;
import net.blerf.ftl.ui.IconCycleButton;
import net.blerf.ftl.ui.ProfileStatsPanel;
import net.blerf.ftl.ui.SavedGameDumpPanel;
import net.blerf.ftl.ui.SavedGameFloorplanPanel;
import net.blerf.ftl.ui.SavedGameGeneralPanel;
import net.blerf.ftl.ui.SavedGameHangarPanel;
import net.blerf.ftl.ui.SavedGameSectorMapPanel;
import net.blerf.ftl.ui.SavedGameStateVarsPanel;
import net.blerf.ftl.ui.ShipUnlockPanel;
import net.blerf.ftl.ui.StatusbarMouseListener;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class FTLFrame extends JFrame {

	private static final Logger log = LogManager.getLogger(FTLFrame.class);

	private Profile profile;
	SavedGameParser.SavedGameState gameState = null;

	private ImageIcon openIcon = new ImageIcon( ClassLoader.getSystemResource("open.gif") );
	private ImageIcon saveIcon = new ImageIcon( ClassLoader.getSystemResource("save.gif") );
	private ImageIcon unlockIcon = new ImageIcon( ClassLoader.getSystemResource("unlock.png") );
	private ImageIcon aboutIcon = new ImageIcon( ClassLoader.getSystemResource("about.gif") );
	private ImageIcon updateIcon = new ImageIcon( ClassLoader.getSystemResource("update.gif") );
	private ImageIcon releaseNotesIcon = new ImageIcon( ClassLoader.getSystemResource("release-notes.png") );

	private URL aboutPage = ClassLoader.getSystemResource("about.html");
	private URL latestVersionTemplate = ClassLoader.getSystemResource("update.html");
	private URL releaseNotesTemplate = ClassLoader.getSystemResource("release-notes.html");

	private String latestVersionUrl = "https://raw.github.com/Vhati/ftl-profile-editor/master/latest-version.txt";
	private String versionHistoryUrl = "https://raw.github.com/Vhati/ftl-profile-editor/master/release-notes.txt";
	private String bugReportUrl = "https://github.com/Vhati/ftl-profile-editor/issues/new";
	private String forumThreadUrl = "http://www.ftlgame.com/forum/viewtopic.php?f=7&t=10959";

	// For checkbox icons
	private static final int maxIconWidth = 64;
	private static final int maxIconHeight = 64;
	private BufferedImage iconShadeImage;

	private ArrayList<JButton> updatesButtonList = new ArrayList<JButton>();
	private Runnable updatesCallback;

	private ShipUnlockPanel shipUnlockPanel;
	private GeneralAchievementsPanel generalAchievementsPanel;
	private ProfileStatsPanel statsPanel;
	private SavedGameDumpPanel savedGameDumpPanel;
	private SavedGameGeneralPanel savedGameGeneralPanel;
	private SavedGameFloorplanPanel savedGamePlayerFloorplanPanel;
	private SavedGameFloorplanPanel savedGameNearbyFloorplanPanel;
	private SavedGameHangarPanel savedGameHangarPanel;
	private SavedGameSectorMapPanel savedGameSectorMapPanel;
	private SavedGameStateVarsPanel savedGameStateVarsPanel;
	private JLabel statusLbl;
	private final HyperlinkListener linkListener;

	private String appName;
	private int appVersion;


	public FTLFrame( String appName, int appVersion ) {
		this.appName = appName;
		this.appVersion = appVersion;

		// GUI setup
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setSize(800, 700);
		setLocationRelativeTo(null);
		setTitle( String.format( "%s v%d", appName, appVersion ) );

		try {
			setIconImage( ImageIO.read( ClassLoader.getSystemResource("unlock.png") ) );
		}
		catch ( IOException e ) {
			log.error( "Error reading \"unlock.png\".", e );
		}

		linkListener = new HyperlinkListener() {
			@Override
			public void hyperlinkUpdate( HyperlinkEvent e ) {
				if ( e.getEventType() == HyperlinkEvent.EventType.ACTIVATED ) {
					log.trace( "Dialog link clicked: "+ e.getURL() );
					if ( Desktop.isDesktopSupported() ) {
						try {
							Desktop.getDesktop().browse( e.getURL().toURI() );
							log.trace( "Link opened in external browser." );
						}
						catch ( Exception f ) {
							log.error( "Unable to open link.", f );
						}
					}
				}
			}
		};

		initCheckboxIcons();

		JPanel contentPane = new JPanel( new BorderLayout() );
		setContentPane(contentPane);

		JTabbedPane tasksPane = new JTabbedPane();
		contentPane.add( tasksPane, BorderLayout.CENTER );

		JPanel profilePane = new JPanel( new BorderLayout() );
		tasksPane.add( "Profile", profilePane );

		JToolBar profileToolbar = new JToolBar();
		setupProfileToolbar(profileToolbar);
		profilePane.add(profileToolbar, BorderLayout.NORTH);

		JTabbedPane profileTabsPane = new JTabbedPane();
		profilePane.add( profileTabsPane, BorderLayout.CENTER );

		shipUnlockPanel = new ShipUnlockPanel(this);
		generalAchievementsPanel = new GeneralAchievementsPanel(this);
		statsPanel = new ProfileStatsPanel(this);

		profileTabsPane.add( "Ship Unlocks & Achievements" , new JScrollPane( shipUnlockPanel ) );
		profileTabsPane.add( "General Achievements" , new JScrollPane( generalAchievementsPanel ) );
		profileTabsPane.add( "Stats" , new JScrollPane( statsPanel ) );


		JPanel savedGamePane = new JPanel( new BorderLayout() );
		tasksPane.add( "Saved Game", savedGamePane );

		JToolBar savedGameToolbar = new JToolBar();
		setupSavedGameToolbar(savedGameToolbar);
		savedGamePane.add(savedGameToolbar, BorderLayout.NORTH);

		JTabbedPane savedGameTabsPane = new JTabbedPane();
		savedGamePane.add( savedGameTabsPane, BorderLayout.CENTER );

		savedGameDumpPanel = new SavedGameDumpPanel(this);
		savedGameGeneralPanel = new SavedGameGeneralPanel(this);
		savedGamePlayerFloorplanPanel = new SavedGameFloorplanPanel(this);
		savedGameNearbyFloorplanPanel = new SavedGameFloorplanPanel(this);
		savedGameHangarPanel = new SavedGameHangarPanel(this);
		savedGameSectorMapPanel = new SavedGameSectorMapPanel(this);
		savedGameStateVarsPanel = new SavedGameStateVarsPanel(this);

		savedGameTabsPane.add( "Dump", savedGameDumpPanel);
		savedGameTabsPane.add( "General", new JScrollPane( savedGameGeneralPanel ) );
		savedGameTabsPane.add( "Player Ship", savedGamePlayerFloorplanPanel );
		savedGameTabsPane.add( "Nearby Ship", savedGameNearbyFloorplanPanel );
		savedGameTabsPane.add( "Change Ship", savedGameHangarPanel );
		savedGameTabsPane.add( "Sector Map", savedGameSectorMapPanel );
		savedGameTabsPane.add( "State Vars", savedGameStateVarsPanel );

		JPanel statusPanel = new JPanel();
		statusPanel.setLayout( new BoxLayout(statusPanel, BoxLayout.Y_AXIS) );
		statusPanel.setBorder( BorderFactory.createLoweredBevelBorder() );
		statusLbl = new JLabel(" ");
		//statusLbl.setFont( statusLbl.getFont().deriveFont(Font.PLAIN) );
		statusLbl.setBorder( BorderFactory.createEmptyBorder(2, 4, 2, 4) );
		statusLbl.setAlignmentX( Component.LEFT_ALIGNMENT );
		statusPanel.add( statusLbl );
		contentPane.add( statusPanel, BorderLayout.SOUTH );

		// Load blank profile (sets Kestrel unlock).
		loadProfile( Profile.createEmptyProfile() );

		// Check for updates in a seperate thread.
		setStatusText( "Checking for updates..." );
		Thread t = new Thread( "CheckVersion" ) {
			@Override
			public void run() {
				checkForUpdate();
			}
		};
		t.setDaemon(true);
		t.start();
	}

	private void showErrorDialog( String message ) {
		JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
	}

	private void initCheckboxIcons() {
		log.trace( "Initialising checkbox locked icon." );
		iconShadeImage = new BufferedImage(maxIconWidth, maxIconHeight, BufferedImage.TYPE_INT_ARGB);
		Graphics g = iconShadeImage.createGraphics();
		g.setColor( new Color(0, 0, 0, 150) );
		g.fillRect(0, 0, maxIconWidth, maxIconHeight);
		InputStream stream = null;
		try {
			stream = DataManager.get().getResourceInputStream("img/customizeUI/box_lock_on.png");
			BufferedImage lock = ImageIO.read( stream );
			int x = (maxIconWidth-lock.getWidth()) / 2;
			int y = (maxIconHeight-lock.getHeight()) / 2;
			g.drawImage(lock, x, y, null);
		}
		catch ( IOException e ) {
			log.error( "Error reading lock image." , e );
		}
		finally {
			try {if ( stream != null ) stream.close();}
			catch ( IOException f ) {}
		}
		g.dispose();
	}

	public BufferedImage getScaledImage( InputStream in ) throws IOException {
		BufferedImage origImage = ImageIO.read( in );
		int width = origImage.getWidth();
		int height = origImage.getHeight();

		if ( width <= maxIconWidth && height < maxIconHeight )
			return origImage;

		if ( width > height ) {
			height /= width / maxIconWidth;
			width = maxIconWidth;
		} else {
			width /= height / maxIconHeight;
			height = maxIconHeight;
		}
		BufferedImage scaledImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = scaledImage.createGraphics();
		g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g2d.drawImage(origImage, 0, 0, width, height, null);
		g2d.dispose();

		return scaledImage;
	}

	public IconCycleButton createCycleButton( String baseImagePath, boolean cycleDifficulty ) {
		InputStream stream = null;
		try {
			stream = DataManager.get().getResourceInputStream(baseImagePath);
			BufferedImage origImage = getScaledImage(stream);
			int centeringOffsetX = (maxIconWidth-origImage.getWidth())/2;
			int centeringOffsetY = (maxIconHeight-origImage.getHeight())/2;

			BufferedImage lockedImage = new BufferedImage( maxIconWidth, maxIconHeight, BufferedImage.TYPE_INT_ARGB );
			Graphics2D lockedG = lockedImage.createGraphics();
			lockedG.drawImage( origImage, centeringOffsetX, centeringOffsetY, null );
			lockedG.drawImage( iconShadeImage, 0, 0, null );
			lockedG.dispose();

			String[] labels = null;
			if ( cycleDifficulty == true )
				labels = new String[] { "EASY", "NORMAL" };  // Locked / Easy / Normal.
			else
				labels = new String[] { null };              // Locked / Unlocked.

			ImageIcon[] icons = new ImageIcon[ 1+labels.length ];
			icons[0] = new ImageIcon( lockedImage );

			for (int i=0; i < labels.length; i++) {  // Create icons, drawing any non-null labels.
				String label = labels[i];
				BufferedImage tempImage = new BufferedImage( maxIconWidth, maxIconHeight, BufferedImage.TYPE_INT_ARGB );
				Graphics2D tempG = tempImage.createGraphics();
				tempG.drawImage( origImage, centeringOffsetX, centeringOffsetY, null );
				if ( label != null ) {
					LineMetrics labelMetrics = tempG.getFontMetrics().getLineMetrics(label, tempG);
					int labelWidth = tempG.getFontMetrics().stringWidth(label);
					int labelHeight = (int)labelMetrics.getAscent() + (int)labelMetrics.getDescent();
					int labelX = tempImage.getWidth()/2 - labelWidth/2;
					int labelY = tempImage.getHeight() - (int)labelMetrics.getDescent();
					tempG.setColor( Color.BLACK );
					tempG.fillRect( labelX-4, tempImage.getHeight() - labelHeight, labelWidth+8, labelHeight );
					tempG.setColor( Color.WHITE );
					tempG.drawString( label, labelX, labelY );
				}
				tempG.dispose();
				icons[1+i] = new ImageIcon( tempImage );
			}

			return new IconCycleButton( icons );
		}
		catch ( IOException e ) {
			log.error( "Error reading cycle button image ("+ baseImagePath +")." , e );
		}
		finally {
			try {if ( stream != null ) stream.close();}
			catch ( IOException f ) {}
		}
		return null;
	}


	private void setupProfileToolbar( JToolBar toolbar ) {
		log.trace( "Initialising Profile toolbar." );

		toolbar.setMargin( new Insets(5, 5, 5, 5) );
		toolbar.setFloatable(false);

		final JFileChooser fc = new JFileChooser();
		fc.addChoosableFileFilter( new FileFilter() {
			@Override
			public String getDescription() {
				return "FTL Profile (prof.sav)";
			}
			@Override
			public boolean accept(File f) {
				return f.isDirectory() || f.getName().equalsIgnoreCase("prof.sav");
			}
		});

		File candidateProfileFile = new File( FTLUtilities.findUserDataDir(), "prof.sav" );
		if ( candidateProfileFile.exists() ) {
			fc.setSelectedFile( candidateProfileFile );
		} else {
			fc.setCurrentDirectory( FTLUtilities.findUserDataDir() );
		}

		fc.setMultiSelectionEnabled(false);

		JButton openButton = new JButton("Open", openIcon);
		openButton.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent e ) {
				log.trace( "Open profile button clicked." );
				fc.setDialogTitle( "Open Profile" );
				if ( fc.showOpenDialog(FTLFrame.this) == JFileChooser.APPROVE_OPTION ) {
					RandomAccessFile raf = null;
					InputStream in = null;
					try {
						log.trace( "File selected: "+ fc.getSelectedFile().getAbsolutePath() );

						// Read whole file so we can hash it.
						raf = new RandomAccessFile( fc.getSelectedFile(), "r" );
						byte[] data = new byte[(int)raf.length()];
						raf.readFully(data);
						raf.close();

						MessageDigest md = MessageDigest.getInstance("MD5");
						byte[] readHash = md.digest(data);

						in = new ByteArrayInputStream(data);
						// Parse file data
						ProfileParser ftl = new ProfileParser();
						Profile p = ftl.readProfile(in);
						in.close();

						FTLFrame.this.loadProfile(p);

						// Perform mock write.
						ByteArrayOutputStream out = new ByteArrayOutputStream();
						FTLFrame.this.updateProfile(profile);
						ftl.writeProfile(out, profile);
						out.close();

						// Hash result.
						byte[] outData = out.toByteArray();
						md.reset();
						byte[] writeHash = md.digest(outData);

						// Compare.
						for (int i = 0; i < readHash.length; i++) {
							if ( readHash[i] != writeHash[i] ) {
								log.error("Hash fail on mock write - Unable to assure valid parsing.");

								String hex = "";
								for (int j = 0; j < data.length; j++) {
									hex += String.format("%02x", data[j]);
									if ( (j+1) % 32 == 0 )
										hex +="\n";
								}

								String errText = "<b>FTL Profile Editor has detected that it cannot interpret your profile correctly.<br/>" +
										"Using this app may result in loss of stats/achievements.</b>" +
										"<br/><br/>" +
										"Please copy (Ctrl-A, Ctrl-C) the following text and paste it into a new bug report <a href='"+bugReportUrl+"'>here</a> " +
										"(GitHub signup is free) or post to the FLT forums <a href='"+forumThreadUrl+"'>here</a> (Signup also free)." +
										"<br/>If using GitHub, set the issue title as \"Profile Parser Error\"<br/><br/>I will fix the problem and release a new version as soon as I can :)" +
										"<br/><br/><pre>"+ hex +"</pre>";

								JDialog failDialog = createHtmlDialog( "Profile Parser Error", errText );
								failDialog.setVisible(true);

								break;
							}
						}

						log.trace( "Profile read successfully." );
					}
					catch( Exception f ) {
						log.error( "Error reading profile.", f );
						showErrorDialog( "Error reading profile:\n"+ f.getMessage() );
					}
					finally {
						try {if ( raf != null ) raf.close();}
						catch ( IOException g ) {}
						try {if ( in != null ) in.close();}
						catch ( IOException g ) {}
					}
				} else {
					log.trace( "Open dialog cancelled." );
				}
			}
		});
		openButton.addMouseListener( new StatusbarMouseListener(this, "Open an existing profile.") );
		toolbar.add( openButton );

		JButton saveButton = new JButton("Save", saveIcon);
		saveButton.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent e ) {
				log.trace( "Save profile button clicked." );
				fc.setDialogTitle( "Save Profile" );
				if ( fc.showSaveDialog(FTLFrame.this) == JFileChooser.APPROVE_OPTION ) {
					FileOutputStream out = null;
					try {
						File file = fc.getSelectedFile();
						log.trace("File selected: "+ file.getAbsolutePath());
						ProfileParser ftl = new ProfileParser();
						out = new FileOutputStream( file );
						FTLFrame.this.updateProfile(profile);
						ftl.writeProfile(out, profile);
					}
					catch( IOException f ) {
						log.error( "Error writing profile.", f );
						showErrorDialog( "Error saving profile:\n"+ f.getMessage() );
					}
					finally {
						try {if ( out != null ) out.close();}
						catch ( IOException g ) {}
					}
				} else {
					log.trace( "Save dialog cancelled." );
				}
			}
		});
		saveButton.addMouseListener( new StatusbarMouseListener(this, "Save the current profile.") );
		toolbar.add( saveButton );


		JButton unlockShipsButton = new JButton("Unlock All Ships", unlockIcon);
		unlockShipsButton.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent e ) {
				log.trace("Unlock all ships button clicked.");
				shipUnlockPanel.unlockAllShips();
			}
		});
		unlockShipsButton.addMouseListener( new StatusbarMouseListener(this, "Unlock All Ships.") );
		toolbar.add( unlockShipsButton );


		JButton unlockShipAchsButton = new JButton("Unlock All Ship Achievements", unlockIcon);
		unlockShipAchsButton.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent e ) {
				log.trace("Unlock all ship achievements button clicked.");
				shipUnlockPanel.unlockAllShipAchievements();
			}
		});
		unlockShipAchsButton.addMouseListener( new StatusbarMouseListener(this, "Unlock All Ship Achievements.") );
		toolbar.add( unlockShipAchsButton );

		toolbar.add( Box.createHorizontalGlue() );

		JButton extractButton = new JButton("Extract Dats", saveIcon);
		extractButton.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent e ) {
				log.trace("Extract button clicked.");

				JFileChooser extractChooser = new JFileChooser();
				extractChooser.setDialogTitle("Choose a dir to extract into");
				extractChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				extractChooser.setMultiSelectionEnabled(false);

				if ( extractChooser.showSaveDialog(FTLFrame.this) == JFileChooser.APPROVE_OPTION ) {
					try {
						File extractDir = extractChooser.getSelectedFile();
						log.trace("Dir selected: "+ extractDir.getAbsolutePath());

						JOptionPane.showMessageDialog(FTLFrame.this, "This may take a few seconds.\nClick OK to proceed.", "About to Extract", JOptionPane.PLAIN_MESSAGE);

						DataManager.get().extractDataDat( extractDir );
						DataManager.get().extractResourceDat( extractDir );

						JOptionPane.showMessageDialog(FTLFrame.this, "All dat content extracted successfully.", "Extraction Complete", JOptionPane.PLAIN_MESSAGE);
					}
					catch( IOException ex ) {
						log.error( "Error extracting dats.", ex );
						showErrorDialog( "Error extracting dat:\n"+ ex.getMessage() );
					}
				} else
					log.trace( "Extract dialog cancelled." );
			}
		});
		extractButton.addMouseListener( new StatusbarMouseListener(this, "Extract dat content to a directory.") );
		toolbar.add( extractButton );

		toolbar.add( Box.createHorizontalGlue() );

		JButton aboutButton = createAboutButton();
		toolbar.add( aboutButton );

		JButton updatesButton = createUpdatesButton();
		updatesButtonList.add( updatesButton );
		toolbar.add( updatesButton );
	}

	private void setupSavedGameToolbar( JToolBar toolbar ) {
		log.trace( "Initialising SavedGame toolbar." );

		toolbar.setMargin( new Insets(5, 5, 5, 5) );
		toolbar.setFloatable(false);

		final JFileChooser fc = new JFileChooser();
		fc.addChoosableFileFilter( new FileFilter() {
			@Override
			public String getDescription() {
				return "FTL Saved Game (continue.sav)";
			}
			@Override
			public boolean accept(File f) {
				return f.isDirectory() || f.getName().equalsIgnoreCase("continue.sav");
			}
		});

		File candidateSaveFile = new File( FTLUtilities.findUserDataDir(), "continue.sav" );
		if ( candidateSaveFile.exists() ) {
			fc.setSelectedFile( candidateSaveFile );
		} else {
			fc.setCurrentDirectory( FTLUtilities.findUserDataDir() );
		}

		fc.setMultiSelectionEnabled(false);

		JButton openButton = new JButton("Open", openIcon);
		openButton.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent e ) {
				log.trace( "Open saved game button clicked." );
				fc.setDialogTitle( "Open Saved Game" );
				if ( fc.showOpenDialog(FTLFrame.this) == JFileChooser.APPROVE_OPTION ) {
					try {
						log.trace( "File selected: "+ fc.getSelectedFile().getAbsolutePath() );

						SavedGameParser parser = new SavedGameParser();
						SavedGameParser.SavedGameState gs = parser.readSavedGame( fc.getSelectedFile() );
						loadGameState( gs );

						log.trace( "Game state read successfully." );

						if ( gameState.getMysteryList().size() > 0 ) {
							StringBuilder musteryBuf = new StringBuilder();
							musteryBuf.append("This saved game file contains mystery bytes the developers hadn't anticipated!\n");
							boolean first = true;
							for (MysteryBytes m : gameState.getMysteryList()) {
								if (first) { first = false; }
								else { musteryBuf.append(",\n"); }
								musteryBuf.append(m.toString().replaceAll("(^|\n)(.+)", "$1  $2"));
							}
							log.warn( musteryBuf.toString() );
						}
					}
					catch( Exception f ) {
						log.error( "Error reading saved game.", f );
						showErrorDialog( "Error reading saved game:\n"+ f.getMessage() );
					}
				} else {
					log.trace( "Open dialog cancelled." );
				}
			}
		});
		openButton.addMouseListener( new StatusbarMouseListener(this, "Open an existing saved game.") );
		toolbar.add( openButton );

		JButton saveButton = new JButton("Save", saveIcon);
		saveButton.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent e ) {
				log.trace( "Save game state button clicked." );

				if ( gameState == null ) return;

				if ( gameState.getMysteryList().size() > 0 )
					log.warn( "The original saved game file contained mystery bytes, which will be omitted in the new file." );

				fc.setDialogTitle( "Save Game State" );
				if ( fc.showSaveDialog(FTLFrame.this) == JFileChooser.APPROVE_OPTION ) {
					FileOutputStream out = null;
					try {
						File file = fc.getSelectedFile();
						log.trace("File selected: "+ file.getAbsolutePath());
						SavedGameParser parser = new SavedGameParser();
						out = new FileOutputStream( file );
						FTLFrame.this.updateGameState(gameState);
						parser.writeSavedGame(out, gameState);
					}
					catch( IOException f ) {
						log.error( "Error writing game state.", f );
						showErrorDialog( "Error saving game state:\n"+ f.getMessage() );
					}
					finally {
						try {if ( out != null ) out.close();}
						catch ( IOException g ) {}
					}
				} else {
					log.trace( "Save dialog cancelled." );
				}
			}
		});
		saveButton.addMouseListener( new StatusbarMouseListener(this, "Save the current game state.") );
		toolbar.add( saveButton );

		JButton dumpButton = new JButton("Dump", saveIcon);
		dumpButton.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent e ) {
				log.trace( "Dump game state button clicked." );

				if ( gameState == null ) return;

				JFileChooser dumpChooser = new JFileChooser();
				dumpChooser.setCurrentDirectory( fc.getCurrentDirectory() );

				ExtensionFileFilter txtFilter = new ExtensionFileFilter("Text Files (*.txt)", new String[] {".txt"});
				dumpChooser.addChoosableFileFilter( txtFilter );

				if ( dumpChooser.showSaveDialog(FTLFrame.this) == JFileChooser.APPROVE_OPTION ) {
					BufferedWriter out = null;
					try {
						log.trace( "File selected: "+ dumpChooser.getSelectedFile().getAbsolutePath() );

						File file = dumpChooser.getSelectedFile();
						if ( !file.exists() && dumpChooser.getFileFilter() == txtFilter && !txtFilter.accept(file) ) {
							file = new File( file.getAbsolutePath() + txtFilter.getPrimarySuffix() );
						}

						out = new BufferedWriter( new OutputStreamWriter( new FileOutputStream( file ) ) );
						out.write(gameState.toString());
						out.close();
					}
					catch( IOException f ) {
						log.error( "Error dumping game state.", f );
						showErrorDialog( "Error dumping game state:\n"+ f.getMessage() );
					}
					finally {
						try {if ( out != null ) out.close();}
						catch ( IOException g ) {}
					}
				} else
					log.trace( "Dump dialog cancelled." );
			}
		});
		dumpButton.addMouseListener( new StatusbarMouseListener(this, "Dump unmodified game state info to a text file.") );
		toolbar.add( dumpButton );

		toolbar.add( Box.createHorizontalGlue() );

		JButton aboutButton = createAboutButton();
		toolbar.add( aboutButton );

		JButton updatesButton = createUpdatesButton();
		updatesButtonList.add( updatesButton );
		toolbar.add( updatesButton );
	}

	public JButton createAboutButton() {
		final JDialog aboutDialog = new JDialog(this,"About",true);
		JPanel aboutPanel = new JPanel();
		aboutPanel.setLayout( new BoxLayout(aboutPanel, BoxLayout.Y_AXIS) );
		aboutDialog.setContentPane(aboutPanel);
		aboutDialog.setSize(300, 250);
		aboutDialog.setLocationRelativeTo( this );

		try {
			JEditorPane editor = new JEditorPane( aboutPage );
			editor.setEditable(false);
			editor.addHyperlinkListener(linkListener);
			aboutPanel.add(editor);
		}
		catch ( IOException e ) {
			log.error(e);
		}

		JButton aboutButton = new JButton("About", aboutIcon);
		aboutButton.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent e ) {
				log.trace("About button clicked.");
				aboutDialog.setVisible(true);
			}
		});
		aboutButton.addMouseListener( new StatusbarMouseListener(this, "View information about this tool and links for information/bug reports") );
		return aboutButton;
	}

	public JButton createUpdatesButton() {
		JButton updatesButton = new JButton("Updates");
		updatesButton.setEnabled(false);
		updatesButton.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent e ) {
				if ( updatesCallback != null )
					updatesCallback.run();
			}
		});
		updatesButton.addMouseListener( new StatusbarMouseListener(this, "Update this tool or review past changes.") );
		return updatesButton;
	}

	private void checkForUpdate() {
		URL url = null;
		BufferedReader in = null;
		String line = null;
		try {
			log.trace( "Checking for latest version." );

			url = new URL( latestVersionUrl );
			in = new BufferedReader( new InputStreamReader( (InputStream)url.getContent(), Charset.forName("UTF-8").newDecoder() ) );
			int latestVersion = Integer.parseInt( in.readLine() );
			in.close();

			if ( latestVersion > appVersion ) {
				log.trace( "New version available." );

				final String historyHtml = getVersionHistoryHtml( latestVersionTemplate, appVersion );

				final Runnable newCallback = new Runnable() {
					@Override
					public void run() {
						log.trace( "Updates button clicked (new version)." );
						JDialog updatesDialog = createHtmlDialog( "Update Available", historyHtml );
						updatesDialog.setVisible( true );
					}
				};
				// Make changes from the GUI thread.
				Runnable r = new Runnable() {
					@Override
					public void run() {
						updatesCallback = newCallback;
						for ( JButton updatesButton : updatesButtonList ) {
							updatesButton.setBackground( new Color( 0xff, 0xaa, 0xaa ) );
							updatesButton.setIcon(updateIcon);
							updatesButton.setEnabled(true);
						}
						setStatusText( "A new version has been released." );
					}
				};
				SwingUtilities.invokeLater(r);

			} else {

				log.trace( "Already up-to-date." );

				final String historyHtml = getVersionHistoryHtml( releaseNotesTemplate, 0 );

				// Replacement behavior for the updates button.
				final Runnable newCallback = new Runnable() {
					@Override
					public void run() {
						log.trace("Updates button clicked (release notes).");
						JDialog updatesDialog = createHtmlDialog( "Release Notes", historyHtml );
						updatesDialog.setVisible(true);
					}
				};
				// Make changes from the GUI thread.
				Runnable r = new Runnable() {
					@Override
					public void run() {
						updatesCallback = newCallback;
						Color defaultColor = UIManager.getColor("Button.background");
						for ( JButton updatesButton : updatesButtonList ) {
							if ( defaultColor != null )
								updatesButton.setBackground(defaultColor);
							updatesButton.setIcon(releaseNotesIcon);
							updatesButton.setEnabled(true);
						}
						setStatusText( "No new updates." );
					}
				};
				SwingUtilities.invokeLater(r);
			}
		}
		catch ( Exception e ) {
			log.error( "Error checking for latest version.", e );
			showErrorDialog( "Error checking for latest version.\n(Use the About window to check the download page manually)\n"+ e );
		}
		finally {
			try {if ( in != null ) in.close();}
			catch ( IOException e ) {}
		}
	}

	private String getVersionHistoryHtml( URL templateUrl, int sinceVersion ) throws IOException {

		// Buffer for presentation-ready html.
		StringBuilder historyBuf = new StringBuilder();

		URL url = null;
		BufferedReader in = null;
		String line = null;
		try {
			// Fetch the template.
			StringBuilder templateBuf = new StringBuilder();
			url = templateUrl;
			in = new BufferedReader( new InputStreamReader( (InputStream)url.getContent() ) );
			while ( (line = in.readLine()) != null ) {
				templateBuf.append(line).append("\n");
			}
			in.close();
			String historyTemplate = templateBuf.toString();

			// Fetch the changelog, templating each revision.
			url = new URL( versionHistoryUrl );
			in = new BufferedReader( new InputStreamReader( (InputStream)url.getContent(), Charset.forName("UTF-8").newDecoder() ) );

			int releaseVersion = 0;
			StringBuilder releaseBuf = new StringBuilder();
			String releaseDesc = null;
			while ( (line = in.readLine()) != null ) {
				releaseVersion = Integer.parseInt( line );
				if ( releaseVersion <= sinceVersion ) break;

				releaseBuf.setLength(0);
				while ( (line = in.readLine()) != null && !line.equals("") ) {
					releaseBuf.append("<li>").append(line).append("</li>\n");
				}
				// Must've either hit a blank or done.

				if (releaseBuf.length() > 0) {
					String[] placeholders = new String[] { "{version}", "{items}" };
					String[] values = new String[] { "v"+releaseVersion, releaseBuf.toString() };
					releaseDesc = historyTemplate;
					for (int i=0; i < placeholders.length; i++)
						releaseDesc = releaseDesc.replaceAll(Pattern.quote(placeholders[i]), Matcher.quoteReplacement(values[i]) );
					historyBuf.append(releaseDesc);
				}
			}
			in.close();

		} finally {
			try {if ( in != null ) in.close();}
			catch ( IOException e ) {}
		}

		return historyBuf.toString();
	}

	private JDialog createHtmlDialog( String title, String content ) {

		final JDialog dlg = new JDialog(this, title, true);
		JPanel panel = new JPanel();
		panel.setLayout( new BoxLayout(panel, BoxLayout.Y_AXIS) );
		dlg.setContentPane(panel);
		dlg.setSize(600, 400);
		dlg.setLocationRelativeTo( this );

		JEditorPane editor = new JEditorPane("text/html", content);
		editor.setEditable(false);
		editor.setCaretPosition(0);
		editor.addHyperlinkListener(linkListener);
		panel.add( new JScrollPane(editor) );

		return dlg;
	}

	public void loadProfile( Profile p ) {
		try {
			log.trace( "Loading profile data into UI." );

			shipUnlockPanel.setProfile(p);
			generalAchievementsPanel.setProfile(p);
			statsPanel.setProfile(p);

			profile = p;
		}
		catch ( IOException e ) {
			log.error( "Error while loading profile.", e );

			if ( profile != null && profile != p ) {
				log.info( "Attempting to revert GUI to the previous profile..." );
				showErrorDialog("Error loading profile.\nAttempting to return to the previous profile...");
				loadProfile(profile);
			} else {
				showErrorDialog("Error loading profile.\nThis has left the GUI in an ambiguous state.\nSaving is not recommended until another profile has successfully loaded.");
			}
		}

		this.repaint();
	}

	public void updateProfile( Profile p ) {
		log.trace( "Updating profile from UI selections." );

		shipUnlockPanel.updateProfile(p);
		generalAchievementsPanel.updateProfile(p);
		statsPanel.updateProfile(p);

		loadProfile(p);
	}

	/**
	 * Returns the currently loaded game state.
	 *
	 * This method should only be called when a panel
	 * needs to pull the state, make a major change,
	 * and reload it.
	 */
	public SavedGameParser.SavedGameState getGameState() {
		return gameState;
	}

	public void loadGameState( SavedGameParser.SavedGameState gs ) {
		savedGameDumpPanel.setGameState( gs );
		savedGameGeneralPanel.setGameState( gs );
		savedGamePlayerFloorplanPanel.setShipState( gs.getPlayerShipState() );
		savedGameNearbyFloorplanPanel.setShipState( gs.getNearbyShipState() );
		savedGameSectorMapPanel.setGameState( gs );
		savedGameStateVarsPanel.setGameState( gs );

		gameState = gs;
	}

	public void updateGameState( SavedGameParser.SavedGameState gs ) {
		// savedGameDumpPanel doesn't modify anything.
		savedGameGeneralPanel.updateGameState( gs );
		savedGamePlayerFloorplanPanel.updateShipState( gs.getPlayerShipState() );
		savedGameNearbyFloorplanPanel.updateShipState( gs.getNearbyShipState() );
		savedGameSectorMapPanel.updateGameState( gs );
		savedGameStateVarsPanel.updateGameState( gs );

		// Sync session's redundant ship info with player ship.
		gs.setPlayerShipName( gs.getPlayerShipState().getShipName() );
		gs.setPlayerShipBlueprintId( gs.getPlayerShipState().getShipBlueprintId() );

		loadGameState(gs);
	}

	public void setStatusText( String text ) {
		if (text.length() > 0)
			statusLbl.setText(text);
		else
			statusLbl.setText(" ");
	}
}
