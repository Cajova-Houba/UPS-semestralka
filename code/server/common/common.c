#include "common.h"

/*
 * Receives byte_count of bytes and stores them to the buffer.
 *
 * Returns:
 * Number of received bytes if everything is ok.
 * CLOSED_CONNECTION: if ENOTCONN raises
 * ERR_MSG: any other error.
 */
int recv_bytes(int sock, char* buffer, int byte_count) {
    int recv_status = 0;
    recv_status = recv(sock, (void *)buffer, byte_count, 0);
    if(recv_status > 0) {
        return recv_status;
    } else {
        if(errno == ENOTCONN) {
            return CLOSED_CONNECTION;
        } else {
            return ERR_MSG;
        }
    }
}

/*
 * Receives byte_count of bytes and stores them to the buffer.
 *
 * Returns:
 * Number of received bytes if everything is ok.
 * CLOSED_CONNECTION: if ENOTCONN raises
 * MSG_TIMEOUT: Socket timed out.
 * ERR_MSG: any other error.
 */
int recv_bytes_timeout(int sock, char* buffer, int byte_count, int ms_timeout) {
    struct pollfd fd;
    int ret_stat = 0;
    char log_msg[255];

    fd.fd = sock;
    fd.events = POLLIN;
    ret_stat = poll(&fd, 1, ms_timeout);
    switch (ret_stat) {
        case -1:
            sprintf(log_msg,"Error while receiving %d bytes: %s.\n",byte_count, strerror(errno));
            serror(COMMON_NAME, log_msg);
            return ERR_MSG;
        case 0:
//            serror(COMMON_NAME, "Timed out while receiving message.\n");
            return MSG_TIMEOUT;
        default:
            return recv(sock, (void *)buffer, byte_count, 0);
    }
}



/*
 * Sends text to the socket.
 *
 * Returns:
 * ERR_MSG: error while sending message.
 * CLOSED_CONNECTION: connection to socket is closed.
 * or number of bytes send
 */ 
int send_txt(int sock, char *txt)
{
    char log_buffer[255];
    int txtlen = (int)strlen(txt);
    int send_status = -1;

    // add new line at the end of the message

    /* send the message */
    send_status = (int)send(sock, (void *)txt, txtlen, 0);
    if (send_status < 0) {
      sprintf(log_buffer, "Error while sending message: %s.\n", strerror(errno));
      serror(COMMON_NAME,log_buffer);
      if(errno == ENOTCONN) {
          return CLOSED_CONNECTION;
      }
      return ERR_MSG;
    }

  return send_status;
}


