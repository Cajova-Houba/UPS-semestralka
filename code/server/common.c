#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <errno.h>

#include <sys/socket.h>
#include <arpa/inet.h>

#include "common.h"
#include "slog.h"

/****************************************************************************/

void err(char *msg)
{
  printf("error (%i, %s): %s\n", errno, strerror(errno), msg);
  exit(1);
}

/****************************************************************************/

/** @return 1 if OK, 0 otherwise */
int send_txt(int sock, char *txt)
{
  int txtlen;
  uint32_t txtlen_net;

  txtlen = strlen(txt);

  /* send message length */
  txtlen_net = htonl(txtlen);
  if (send(sock, (void *)&txtlen_net, sizeof(txtlen_net), 0) < 0) return 0;

  /* send the message itself */
  printf("sending: %s\n", txt);
  if (send(sock, (void *)txt, txtlen, 0) < 0) return 0;

  return 1;
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
	   serror("common","Message too long.\n");
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
  sprintf(&log_buffer,"Receiving %d chars.\n",txtlen);
  sdebug(COMMON_NAME, log_buffer);
  if (txtlen > MAX_TXT_LENGTH) {
	   serror("common","Message too long.\n");
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


