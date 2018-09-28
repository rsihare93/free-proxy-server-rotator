# free-proxy-server-rotator
This is small server built which extracts and keep free live http/ https proxies across various free proxy sites  and keeps track of their health. Server serves number of clients over tcp and provides them rotated proxy address on demand. Server responds over tcp using json.
The server can be hosted on any java 8 compatible environment. It serves over configured tcp port and keeps the tcp connection persistent.
As it serves over tcp connection and using json message format, it can serve client built in any programming lanuage.
