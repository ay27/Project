This package is used to share some data between some classes.

1. Session.java is used to describe a session with remote server. It contains
	the RTP data ports, the video quality describe, surface holder, and so on.
2. VideoQuality.java is used to describe a video's quality, contains the
	bit-rate, the video-encoder, the frame-rate, the video-size, the SPS & PPS
	data. Some of this data will be saved in the external-storage.
