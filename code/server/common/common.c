#include "common.h"

/*
 * Receives byte_count of bytes and stores them to the buffer.
 * 
 * If everything is ok, returns number of received bytes.
 * If the socket closes the connection, returns 0.
 * If other error occurs, returns -1. 
 */
int recv_bytes(int sock, char* buffer, int byte_count) {
  return recv(sock, (void *)buffer, byte_count, 0);
}

/****************************************************************************/

/*
 * Sends text to the socket.
 * 
 * If everything is ok, returns num of bytes sent.
 * If the socket closes the connection, returns 0.
 * If other error occurs, returns <0.
 */ 
int send_txt(int sock, char *txt)
{
  char log_buffer[255];
  int txtlen = strlen(txt);
  int send_status = -1;

  /* send the message */
  sprintf(log_buffer,"Sending: %s\n", txt);
  sdebug(COMMON_NAME, log_buffer);
  
  send_status = send(sock, (void *)txt, txtlen, 0);
  if (send_status < 0) {
      sprintf(log_buffer, "Error while sending message: %s.\n", strerror(errno));
      serror(COMMON_NAME,log_buffer);
  } else {
      sdebug(COMMON_NAME, "Sent.\n");
  }

  return send_status;
}

/****************************************************************************/

/** @return 1 if OK, 0 otherwise */
int recv_txt(int sock)
{
  char buf[MAX_TXT_LENGTH + 1];
  int txtlen;
  uint32_t txtlen_net;

  if (recv(sock, (void *)&txtlen_net, sizeof(txtlen_net), 0) < 0) return 0;
  txtlen = ntohl(txtlen_net);
  if (txtlen > MAX_TXT_LENGTH) {
	   serror(COMMON_NAME,"Message too long.\n");
	   return 0;
  }

  if (recv(sock, (void *)buf, txtlen, 0) < 0) return 0;
  buf[txtlen] = '\0'; /* C string terminator */

  printf("received: %s\n", buf);
  return 1;
}

/****************************************************************************/

/*
 * Receives a text from socket and stores it to buffer.
 * The stored text will be no longer than MAX_TXT_LENGTH.
 * 
 * If the text is received, returns 1.
 * If the socket closes the connection, returns 2;
 */ 
int recv_txt_buffer(int sock, char *buffer) {
  int txtlen;
  char log_buffer[255];
  uint32_t txtlen_net;
  int recv_status = 0;

  recv_status = recv(sock, (void *)&txtlen_net, sizeof(txtlen_net), 0);
  /*printf("Receive status: %d.\n",recv_status);*/
  
  /*socket closed*/
  if (recv_status == 0) {
	  return 2;
  }
  if (recv_status < 0) return 0;
  txtlen = ntohl(txtlen_net);
  /*txtlen = txtlen_net;*/
  sprintf(log_buffer,"Receiving %d chars.\n",txtlen);
  sdebug(COMMON_NAME, log_buffer);
  if (txtlen > MAX_TXT_LENGTH) {
	   serror(COMMON_NAME,"Message too long.\n");
	   return 0;
  }

  if (recv(sock, (void *)buffer, txtlen, 0) < 0) return 0;
  buffer[txtlen] = '\0'; /* C string terminator */

  /*printf("received: %s\n", buffer);*/
  return 1;
}

void send_greetings(int sock)
{
  recv_txt(sock);
  send_txt(sock, "welcome!");
}


