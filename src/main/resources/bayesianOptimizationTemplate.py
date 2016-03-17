import socket


def main(job_id, params):
    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    sock.connect(("127.0.0.1", 9988))
    strToSend = u""
    for key, value in params:
        strToSend += u"%s VALUE %s\n" % (key, value[0])
    sock.sendall(strToSend)
    utility = sock.recv(4096)
    socket.close()
    return float(utility)
