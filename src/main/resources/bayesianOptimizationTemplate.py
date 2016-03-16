import socket


def main(job_id, params):
    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    sock.connect(("127.0.0.1", 9988))
    sock.send(u"all key VALUE value\n")
    utility = sock.recv(1024)
    return float(utility)
