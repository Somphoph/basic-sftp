package org.basic

import com.jcraft.jsch.*
import org.slf4j.LoggerFactory
import java.io.File
import java.util.*


class BasicSftp {

    @Throws(Exception::class)
    fun putFile(conf: SFTPConfig, localFilePath: String, remoteFilePath: String) {
        var session: Session? = null
        var sftpChannel: ChannelSftp? = null
        try {
            val localFile = File(localFilePath)
            val connObj = connect(session, sftpChannel, conf)
            session = connObj.session
            sftpChannel = connObj.sftpChannel

            val remoteFile = File(remoteFilePath)
            val remoteDir = remoteFile.parentFile

            log.info("Working directory : " + sftpChannel!!.pwd())

            log.info("Change directory to : " + remoteDir.path)

            prepareDirectory(sftpChannel, remoteDir.path)


            val localDir = localFile.parentFile
            if (localDir.isDirectory) {
                sftpChannel.lcd(localDir.path)
            }
            log.info("Upload file : " + localFilePath + " to : " + remoteFile.name)
            sftpChannel.put(localFilePath, remoteFile.name)
        } catch (e: SftpException) {
            log.error("Error on put file error : ${e.id} - ${e.message}",e)
            throw e
        } finally {
            disconnect(session, sftpChannel)
        }
    }


    @Throws(Exception::class)
    fun putFile(conf: SFTPConfig, localPath: String, matchFile: String, remotePath: String) {
        var session: Session? = null
        var sftpChannel: ChannelSftp? = null
        try {
            val connObj = connect(session, sftpChannel, conf)
            session = connObj.session
            sftpChannel = connObj.sftpChannel

            log.info("Working directory : " + sftpChannel!!.pwd())

            log.info("Change directory to : $remotePath")
            prepareDirectory(sftpChannel, remotePath)

            sftpChannel.lcd(localPath)

            val files = getSourceFiles(localPath, matchFile)

            for (file in files!!) {
                log.info("Upload file : " + file.name + " to : " + file.name)
                sftpChannel.put(file.name, file.name)
            }
        } catch (e: Exception) {
            log.error("Error on put file.",e)
            throw e
        } finally {
            disconnect(session, sftpChannel)
        }
    }

    @Throws(Exception::class)
    private operator fun get(conf: SFTPConfig, remoteFile: String, localFile: String) {
        var session: Session? = null
        var sftpChannel: ChannelSftp? = null
        try {
            val connObj = connect(session, sftpChannel, conf)
            session = connObj.session
            sftpChannel = connObj.sftpChannel
            sftpChannel!!.get(remoteFile, localFile)
        } catch (e: Exception) {
            log.error("Error on get file.",e)
            throw e
        } finally {
            disconnect(session, sftpChannel)
        }
    }

    @Throws(Exception::class)
    fun getFile(conf: SFTPConfig, remoteFile: String, localPath: String) {
        val remote = File(remoteFile)
        val fileName = remote.name

        val localDir = File(localPath)
        if (!localDir.isDirectory) {
            throw RuntimeException("Your localPath is not directory.")
        }

        val localFile = localDir.path + File.separator + fileName
        get(conf, remoteFile, localFile)
    }

    @Throws(Exception::class)
    fun getFromDir(conf: SFTPConfig, remoteDir: String, localDir: String) {
        var session: Session? = null
        var sftpChannel: ChannelSftp? = null
        try {
            val connObj = connect(session, sftpChannel, conf)
            session = connObj.session
            sftpChannel = connObj.sftpChannel
            val exp = "$remoteDir/*"
            val list = sftpChannel!!.ls(exp) as Vector<ChannelSftp.LsEntry>
            for (entry in list) {
                val remoteFile = remoteDir + "/" + entry.filename
                val localFile = localDir + File.separator + entry.filename

                sftpChannel.get(remoteFile, localFile)
            }
        } catch (e: Exception) {
            log.error("Error on get file.",e)
            throw e
        } finally {
            disconnect(session, sftpChannel)
        }
    }

    @Throws(Exception::class)
    fun remove(conf: SFTPConfig, remotePath: String) {
        var session: Session? = null
        var sftpChannel: ChannelSftp? = null
        try {
            val connObj = connect(session, sftpChannel, conf)
            session = connObj.session
            sftpChannel = connObj.sftpChannel
            sftpChannel!!.rm(remotePath)
        } catch (e: Exception) {
            log.error("Error on remove file.",e)
            throw e
        } finally {
            disconnect(session, sftpChannel)
        }
    }

    @Throws(JSchException::class)
    private fun connect(session: Session?, sftpChannel: ChannelSftp?, conf: SFTPConfig): ConnectionObj {
        var session = session
        var sftpChannel = sftpChannel
        val jsch = JSch()
        session = jsch.getSession(conf.usr, conf.host!!, conf.port)

        session!!.setConfig("StrictHostKeyChecking", "no")
        if (conf.pwd != null && "" != conf.pwd) {
            session.setPassword(conf.pwd)
        }
        session.timeout = 300000
        log.info("Session connect...")
        session.connect()
        log.info("Session connected.")

        val channel = session.openChannel("sftp")

        log.info("Channel connect...")
        channel.connect()
        log.info("Channel connected.")

        sftpChannel = channel as ChannelSftp
        val obj = ConnectionObj()
        obj.session = session
        obj.sftpChannel = sftpChannel
        return obj
    }

    private fun disconnect(session: Session?, sftpChannel: ChannelSftp?) {
        if (sftpChannel != null) {
            try {
                if (!sftpChannel.isClosed) {
                    sftpChannel.exit()
                }
            } catch (e: Exception) {
                log.error("Error on disconnect.",e)
            }

        }

        if (session != null) {
            try {
                if (session.isConnected) {
                    session.disconnect()
                }
            } catch (e: Exception) {
                log.error("Error on disconnect.",e)
            }

        }
    }

    private fun getSourceFiles(sourceDir: String, sourceFileName: String?): Array<File>? {
        val dir = File(sourceDir)
        if (sourceFileName == null || sourceFileName == "*") {
            return dir.listFiles()
        }


        if (sourceFileName.indexOf("*") > -1) {
            dir.listFiles { dir, name ->
                var match = false

                val matchs = sourceFileName.split(".*".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

                var i = 0
                var idx = 0
                while (i < matchs.size) {
                    val fidx = name.indexOf(matchs[i], idx)
                    if (fidx > -1) {
                        match = true
                        idx = fidx + matchs[i].length
                    } else {
                        match = false
                        break
                    }
                    i++
                }
                match
            }
        }

        return emptyArray()
    }

    @Throws(Exception::class)
    private fun prepareDirectory(sftp: ChannelSftp, directory: String) {
        val dir = File(directory)
        val dirs = ArrayList<String>()

        var tdir = dir
        while (tdir.parentFile != null) {
            dirs.add(0, tdir.parent.replace("\\\\".toRegex(), "/"))
            tdir = tdir.parentFile
        }
        dirs.add(dir.path.replace("\\\\".toRegex(), "/"))

        for (d in dirs) {
            try {
                sftp.cd(d)
            } catch (e: Exception) {
                log.error("Error on prepare directory.",e)
                log.info("Can't access directory : $d.")
            }

            log.info("Working directory :" + sftp.pwd())
            if (d != sftp.pwd()) {
                log.info("Try to make destination directory...")
                val rdir = File(d)
                log.info("Create directory : " + rdir.getName())
                sftp.mkdir(rdir.getName())
                log.info("Change working directory to :$d")
                sftp.cd(d)
            }
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(BasicSftp::class.java)
    }
}

