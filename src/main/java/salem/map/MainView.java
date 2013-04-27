package salem.map;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

public class MainView {
	private JList<PlaySession> sessionList;
	private JPanel contentPane;
	private JList<PlaySession> matchesList;
	private MapPanel mergedMap;
	private MapPanel map;
	private TilesRepository tilesRepository;
	private MapPanel otherSessionMap;
	private JSplitPane mainSplit;
	private JSplitPane listSplit;
	private JSplitPane mapSplit;
	private JSplitPane topMapSplit;
	private JButton mergeButton;
	private JPanel matchesPanel;
	private JButton deleteButton;
	private transient MapPanel.MergeSpec mergeSpec;
	private File rootDir;
	private DefaultListModel<PlaySession> sessionListModel = new DefaultListModel<PlaySession>();

	public MainView() {
		sessionList.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				PlaySession value = sessionList.getSelectedValue();
				try {
					otherSessionMap.setSession(null, null);
					mergedMap.setSession(null, null);
					mergeButton.setEnabled(false);
					deleteButton.setEnabled(!sessionList.getSelectedValuesList().isEmpty());
					mergeSpec = null;
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				if (null == value) {
					matchesList.setListData(new PlaySession[0]);
					try {
						map.setSession(null, null);
					} catch (IOException e1) {
						e1.printStackTrace();
					}

				} else {
					try {
						map.setSession(value, null);
					} catch (IOException e1) {
						e1.printStackTrace();
					}
					try {
						Collection<PlaySession> matches = tilesRepository.findMatches(value);
						matches.remove(value);
						PlaySession[] listData = matches.toArray(new PlaySession[matches.size()]);
						Arrays.sort(listData, PlaySession.COMPARATOR);
						matchesList.setListData(listData);
						if (listData.length > 0) {
							matchesList.setSelectedValue(listData[0], true);

						}
					} catch (Exception e1) {
						e1.printStackTrace();
					}

				}

			}
		});
		matchesList.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				final PlaySession session1 = sessionList.getSelectedValue();
				PlaySession session2 = matchesList.getSelectedValue();
				mergeButton.setEnabled(session2 != null);
				mergeSpec = null;
				matchesPanel.removeAll();
				if (session2 != null) {
					try {
						mergedMap.setSession(session1, null);
						List<SessionMatch> sessionMatches = mergedMap.computeAllMatches(session2);
						Set<Point> solutions = new HashSet<Point>();
						for (final SessionMatch sessionMatch : sessionMatches) {
							Point translate = new Point(sessionMatch.xtranslate, sessionMatch.ytranslate);
							if (solutions.contains(translate)) continue;
//							System.out.println("adding solution " + translate);
							solutions.add(translate);
						}
/*
						if (solutions.size() > 1) {
							solutions.clear();

							for (final SessionMatch sessionMatch : sessionMatches) {
								Point translate = new Point(sessionMatch.xtranslate, sessionMatch.ytranslate);
								if (solutions.contains(translate)) continue;
								matchesPanel.add(new JButton(new AbstractAction("" + sessionMatch.xtranslate + "," + sessionMatch.ytranslate) {
									public void actionPerformed(ActionEvent e) {
										try {
											mergedMap.setSession(session1, null);
											mergedMap.mergeWithMatch(sessionMatch.session2, sessionMatch);
										} catch (IOException e1) {
											e1.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
										}
									}

									@Override
									public boolean isEnabled() {
										return super.isEnabled();    //To change body of overridden methods use File | Settings | File Templates.
									}
								}));
							}

						}
*/
						matchesPanel.revalidate();
						matchesPanel.repaint();
						mergeSpec = mergedMap.merge(session2);
						SessionMatch match = mergeSpec.match;
						otherSessionMap.setSession(session2, match.matchedTileInSession2);
						map.setSession(session1, match.matchedTileInSession1);

					} catch (Exception e1) {
						e1.printStackTrace();
					}
				} else {
					try {
						map.setSession(session1, null);
						otherSessionMap.setSession(null, null);
						mergedMap.setSession(null, null);
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				}
			}
		});
		deleteButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				List<PlaySession> sessions = sessionList.getSelectedValuesList();
				int[] indices = sessionList.getSelectedIndices();

				for (PlaySession session : sessions) {
					try {
						tilesRepository.remove(session);
					} catch (Exception e1) {
						e1.printStackTrace();
					}
					for (File file : session.getRootDir().listFiles()) {
						file.delete();
					}
					session.getRootDir().delete();
				}
				refreshSessionsList();
				// select the next value
				int index = indices[indices.length - 1] - indices.length + 1;
//				System.out.println("select "+index);
				sessionList.setSelectedIndex(index);
			}
		});
		mergeButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					if (mergeSpec == null) {
						return;
					}
					DateFormat df = new SimpleDateFormat("yyyy-MM-dd hh.mm.ss");
					df.format(new Date());

					long maxModifiedDate = 0;
					for (File tile : mergeSpec.match.session1.tiles) {
						long lastModified = tile.lastModified();
						maxModifiedDate = Math.max(lastModified, maxModifiedDate);
					}
					for (File tile : mergeSpec.match.session2.tiles) {
						long lastModified = tile.lastModified();
						maxModifiedDate = Math.max(lastModified, maxModifiedDate);
					}
					File destDir = new File(mergeSpec.match.session1.rootDir.getParent(), "merge_" + df.format(new Date(maxModifiedDate)));
					if (destDir.exists()) {
						destDir = new File(destDir.getParent(), destDir.getName() + "_1");
					}
					destDir.mkdirs();
//					System.out.println("merging into " + destDir.getAbsolutePath());
					TileOffsetHelper offsetHelper = new TileOffsetHelper();

					tilesRepository.remove(mergeSpec.match.session1);
					tilesRepository.remove(mergeSpec.match.session2);

//                System.out.println("translate1 : "+mergeSpec.session1Translate);
//                System.out.println("translate2 : "+mergeSpec.session2Translate);
					for (File session1Tile : mergeSpec.match.session1.tiles) {
						Point point = offsetHelper.parseOffset(session1Tile);
						point.x = point.x + mergeSpec.match.xtranslate;
						point.y = point.y + mergeSpec.match.ytranslate;

						long lastModified = session1Tile.lastModified();
						File destFile = new File(destDir, MessageFormat.format("tile_{0}_{1}.png", point.x, point.y));
//						System.out.println("moving " + session1Tile + " to " + destFile);
						Files.move(session1Tile.toPath(), destFile.toPath());
						destFile.setLastModified(lastModified);

					}
					for (File session2Tile : mergeSpec.match.session2.tiles) {
						Point point = offsetHelper.parseOffset(session2Tile);
						File destfile = new File(destDir, MessageFormat.format("tile_{0}_{1}.png", point.x, point.y));
						if (destfile.exists() && destfile.lastModified() > session2Tile.lastModified()) {
//							System.out.println("skipping newer " + destfile);
						} else {
//							System.out.println("overwriting " + destfile + " with " + session2Tile);
							long lastModified = session2Tile.lastModified();
							maxModifiedDate = Math.max(lastModified, maxModifiedDate);
							Files.copy(session2Tile.toPath(), destfile.toPath(), StandardCopyOption.REPLACE_EXISTING);
							destfile.setLastModified(lastModified);
						}
						session2Tile.delete();
					}

					mergeSpec.match.session1.rootDir.delete();
					mergeSpec.match.session2.rootDir.delete();

					PlaySession newSession = new PlaySession(destDir, tilesRepository);
					refreshSessionsList();
					sessionList.setSelectedValue(newSession, true);

				} catch (IOException e1) {
					e1.printStackTrace();
				} catch (Exception e1) {
					e1.printStackTrace();
				}


			}
		});

	}

	private void refreshSessionsList() {
		sessionListModel.removeAllElements();
		ArrayList<PlaySession> newSessions = new ArrayList<PlaySession>(tilesRepository.allSessions());
		Collections.sort(newSessions, PlaySession.COMPARATOR);
		for (PlaySession session : newSessions) {
			sessionListModel.addElement(session);
		}
	}

	public void initialize(File rootDir) throws Exception {
		refreshData(rootDir);
		sessionList.setCellRenderer(new PlaySessionListCellRenderer());
		matchesList.setCellRenderer(new PlaySessionListCellRenderer());
		sessionList.setModel(sessionListModel);

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				mainSplit.setDividerLocation(0.2);
				listSplit.setDividerLocation(0.5);
				mapSplit.setDividerLocation(0.5);
				topMapSplit.setDividerLocation(0.5);
			}
		});
	}

	private void refreshData(File rootDir) throws Exception {
		sessionListModel.clear();
		this.rootDir = rootDir;
		File[] files = this.rootDir.listFiles();
		if (null == files) {
			throw new IllegalArgumentException("root dir has no files");
		}

		tilesRepository = new TilesRepository();
		ArrayList<PlaySession> sessions = new ArrayList<PlaySession>();
		for (File file : files) {
			if (file.isDirectory()) {
				if (file.list().length == 0) {
					file.delete();
					continue;
				}
				PlaySession session = new PlaySession(file, tilesRepository);
				sessions.add(session);
			}
		}
		Collections.sort(sessions, PlaySession.COMPARATOR);
		for (PlaySession session : sessions) {
			sessionListModel.addElement(session);
		}
		matchesList.setListData(new PlaySession[0]);
	}

	private static class PlaySessionListCellRenderer extends DefaultListCellRenderer {
		@Override
		public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
			super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
			PlaySession session = (PlaySession) value;
			setText(session.getRootDir().getName());
			return this;
		}
	}

	public static void main(String[] args) throws Exception {
		final JFrame frame = new JFrame("MainView");
		final MainView mainView = new MainView();
		frame.setContentPane(mainView.contentPane);
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame.pack();
		frame.setSize(1600, 1024);
		String userHome = System.getProperty("user.home");
		File salemHome = new File(userHome, "Salem");
		File mapsHome = new File(salemHome, "map");
		File[] serverDirs = mapsHome.listFiles();
		final File rootDir;
		if (null != serverDirs && serverDirs.length == 1) {
			rootDir = serverDirs[0];
		} else if (serverDirs != null && serverDirs.length > 1) {
			rootDir = (File) JOptionPane.showInputDialog(null, "Choose a server", "Choose a server", JOptionPane.QUESTION_MESSAGE, null, serverDirs, serverDirs[0]);
		} else {
			JFileChooser chooser = new JFileChooser(mainView.rootDir);
			chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			int returnVal = chooser.showOpenDialog(frame);
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				rootDir = chooser.getSelectedFile();
			} else {
				return;
			}
		}
		JMenuBar menus = new JMenuBar();
		JMenu fileMenu = new JMenu("File");
		fileMenu.setMnemonic('F');

		menus.add(fileMenu);
		fileMenu.add(new JMenuItem(new AbstractAction("Open Maps in ...") {
			public void actionPerformed(ActionEvent e) {
				JFileChooser chooser = new JFileChooser(mainView.rootDir);
				chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				int returnVal = chooser.showOpenDialog(frame);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					try {
						mainView.refreshData(chooser.getSelectedFile());
					} catch (Exception e1) {
						e1.printStackTrace();
					}
				}
			}
		}));
		frame.setJMenuBar(menus);


		mainView.initialize(rootDir);
		frame.setVisible(true);
	}
}
