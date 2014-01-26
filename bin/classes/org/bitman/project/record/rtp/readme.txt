This package is used to transfer the data stream from CameraWorker to the RTP_Socket,
	then will be send to the remote server.

1. RTCP_SenderReport.java is used to send the SR-packet to remote server.
2. RTP_Socket.java is the only socket to output the data stream to remote server.
3. Sender.java manage the RTP_Socket. It get the data from CameraWorker, then
	use the RTP_Socket to send it.
