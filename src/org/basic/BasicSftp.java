package org.basic;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

public class BasicSftp {

	final static Logger log = Logger.getLogger(BasicSftp.class);

	/**
	 * 
	 * @param conf
	 * @param localPath
	 * @param localFileName
	 * @param remotePath
	 * @param remoteFileName
	 * @throws Exception
	 */
	public void putFile(SFTPConfig conf, String localPath, String localFileName, String remotePath, String remoteFileName)
			throws Exception {

		Session session = null;
		ChannelSftp sftpChannel = null;
		try {

			connect(session, sftpChannel, conf);

			log.info("Working directory : " + sftpChannel.pwd());

			log.info("Change directory to : " + remotePath);
			prepareDirectory(sftpChannel, remotePath);

			sftpChannel.lcd(localPath);

			log.info("Upload file : " + localFileName + " to : " + remoteFileName);
			sftpChannel.put(localFileName, remoteFileName);

		} catch (Exception e) {
			log.error(e);
			throw e;
		} finally {
			if (sftpChannel != null) {
				try {
					if (!sftpChannel.isClosed()) {
						sftpChannel.exit();
					}
				} catch (Exception e) {
					log.error(e);
				}
			}

			if (session != null) {
				try {
					if (session.isConnected()) {
						session.disconnect();
					}
				} catch (Exception e) {
					log.error(e);
				}
			}

		}
	}

	/**
	 * 
	 * @param conf
	 * @param localPath
	 *            Local directory of program running.
	 * @param matchFile
	 *            Match file expression string for find file in local directory
	 *            to put to remote server.
	 * @param remotePath
	 *            Remote directory to upload your file.
	 * @throws Exception 
	 */
	public void putFile(SFTPConfig conf, String localPath, String matchFile, String remotePath) throws Exception {
		Session session = null;
		ChannelSftp sftpChannel = null;
		try {

			connect(session, sftpChannel, conf);

			log.info("Working directory : " + sftpChannel.pwd());

			log.info("Change directory to : " + remotePath);
			prepareDirectory(sftpChannel, remotePath);

			sftpChannel.lcd(localPath);

			File[] files = getSourceFiles(localPath, matchFile);

			for (File file : files) {
				log.info("Upload file : " + file.getName() + " to : " + file.getName());
				sftpChannel.put(file.getName(), file.getName());
			}

		} catch (Exception e) {
			log.error(e);
			throw e;
		} finally {
			if (sftpChannel != null) {
				try {
					if (!sftpChannel.isClosed()) {
						sftpChannel.exit();
					}
				} catch (Exception e) {
					log.error(e);
				}
			}

			if (session != null) {
				try {
					if (session.isConnected()) {
						session.disconnect();
					}
				} catch (Exception e) {
					log.error(e);
				}
			}

		}
	}

	private void connect(Session session, ChannelSftp sftpChannel, SFTPConfig conf) throws JSchException {

		JSch jsch = new JSch();
		session = jsch.getSession(conf.getUsr(), conf.getHost(), conf.getPort());
		session.setConfig("StrictHostKeyChecking", "no");
		session.setPassword(conf.getPwd());

		log.info("Session connect...");
		session.connect();
		log.info("Session connected.");

		Channel channel = session.openChannel("sftp");

		log.info("Channel connect...");
		channel.connect();
		log.info("Channel connected.");

		sftpChannel = (ChannelSftp) channel;
	}

	private File[] getSourceFiles(String sourceDir, final String sourceFileName) {

		File dir = new File(sourceDir);
		if (sourceFileName == null || sourceFileName.equals("*")) {

			return dir.listFiles();

		} else {

			if (sourceFileName.indexOf("*") > -1) {
				return dir.listFiles(new FilenameFilter() {

					@Override
					public boolean accept(File dir, String name) {
						boolean match = false;

						String[] matchs = sourceFileName.split("*");

						for (int i = 0, idx = 0; i < matchs.length; i++) {
							int fidx = name.indexOf(matchs[i], idx);
							if (fidx > -1) {
								match = true;
								idx = fidx + matchs[i].length();
							} else {
								match = false;
								break;
							}
						}
						return match;
					}
				});
			}
		}
		return new File[] {};
	}

	private void prepareDirectory(ChannelSftp sftp, String directory) throws Exception {
		File dir = new File(directory);
		List<String> dirs = new ArrayList<String>();

		File tdir = dir;
		while (tdir.getParentFile() != null) {
			dirs.add(0, tdir.getParent().replaceAll("\\\\", "/"));
			tdir = tdir.getParentFile();
		}
		dirs.add(dir.getPath().replaceAll("\\\\", "/"));

		for (String d : dirs) {
			try {
				sftp.cd(d);
			} catch (Exception e) {
				log.error(e);
				log.info("Can't access directory : " + d + ".");
			}
			log.info("Working directory :" + sftp.pwd());
			if (!d.equals(sftp.pwd())) {
				log.info("Try to make destination directory...");
				File rdir = new File(d);
				log.info("Create directory : " + rdir.getName());
				sftp.mkdir(rdir.getName());
				log.info("Change working directory to :" + d);
				sftp.cd(d);
			}
		}
	}

}
