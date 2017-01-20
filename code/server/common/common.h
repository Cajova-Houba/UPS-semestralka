#ifndef __COMMON_H
#define __COMMON_H

#define SRV_PORT				65000
#define MAX_TXT_LENGTH			50
/*
 * Nick is expected to be delivered after connection in this time.
 * In ms.
 */
#define MAX_NICK_TIMEOUT      10000

/*
 * Waiting for turn - 2 minutes and 5 seconds (in client it's just 2 minutes), after that it is assumed, that player
 * has disconnected and the waiting process starts.
 */
#define MAX_TURN_WAITING_TIMEOUT     125000

#include <stdio.h>
#include <string.h>
#include <errno.h>

#include <sys/socket.h>
#include <arpa/inet.h>
#include <poll.h>

#include "common.h"
#include "slog.h"
#include "seneterror.h"

/*
 * Receives byte_count of bytes and stores them to the buffer.
 * Returns received number of bytes or -1 if error occurs.
 * 
 */
int recv_bytes(int sock, char* buffer, int byte_count);

int recv_bytes_timeout(int sock, char* buffer, int byte_count, int ms_timeout);

int send_txt(int sock, char *txt);
int recv_txt(int sock);
void send_greetings(int sock);
int recv_txt_buffer(int sock, char *buffer);

#endif
