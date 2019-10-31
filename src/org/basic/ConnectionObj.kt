package org.basic

import com.jcraft.jsch.ChannelSftp

class ConnectionObj {
    var session: com.jcraft.jsch.Session? = null
    var sftpChannel: ChannelSftp? = null
}
