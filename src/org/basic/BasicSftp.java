package org.basic;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

public class BasicSftp {
	
	final static Logger log = Logger.getLogger(BasicSftp.class);

	/**
	 * 
	 * @param host
	 * @param port
	 * @param usr
	 * @param pwd
	 * @param localPath
	 * @param localFileName
	 * @param remotePath
	 * @param remoteFileName
	 * @throws Exception
	 */
	public void putFile(String host,int port,String usr,String pwd,String localPath,String localFileName,String remotePath,String remoteFileName) throws Exception {
		JSch jsch = new JSch();
		Session session = null;
		ChannelSftp sftpChannel = null;
		try {
			session = jsch.getSession(usr, host, 22);
			session.setConfig("StrictHostKeyChecking", "no");
			session.setPassword(pwd);

			log.info("Session connect...");
			session.connect();
			log.info("Session connected.");

			Channel channel = session.openChannel("sftp");

			log.info("Channel connect...");
			channel.connect();
			log.info("Channel connected.");

			sftpChannel = (ChannelSftp) channel;
			log.info("Working directory : " + sftpChannel.pwd());

			log.info("Change directory to : " + remotePath);
			prepareDirectory(sftpChannel, remotePath);

			sftpChannel.lcd(localPath);

			File[] files = getSourceFiles(localPath, localFileName);
			if (files.length > 0) {
				for (File file : files) {

					log.info("Upload file : " + file.getName() + " to : " + file.getName());
					sftpChannel.put(file.getName(),file.getName());
				}

			} else {
				if (!remoteFileName.equals("*")) {
					log.info("Upload file : " + localFileName + " to : "
							+ remoteFileName);
					sftpChannel.put(localFileName, remoteFileName);
				} else {
					log.info("Upload file : " + localFileName + " to : " + localFileName);
					sftpChannel.put(localFileName, localFileName);
				}
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
				}
			}

			if (session != null) {
				try {
					if (session.isConnected()) {
						session.disconnect();
					}
				} catch (Exception e) {
				}
			}

		}
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
						String startMath = sourceFileName.substring(0, sourceFileName.indexOf("*"));
						String endMath = sourceFileName.substring(sourceFileName.indexOf("*") + 1,
								sourceFileName.length());

						match = !startMath.equals("") ? name.startsWith(startMath) : true;

						match = !endMath.equals("") ? name.endsWith(endMath) : true;

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
