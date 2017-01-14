#ifndef __COMMON_H
#define __COMMON_H

#define SRV_PORT				65000
#define MAX_TXT_LENGTH			50

#include <stdio.h>
#include <string.h>

#include <sys/socket.h>
#include <arpa/inet.h>

#include "common.h"
#include "slog.h"

/*
 * Receives byte_count of bytes and stores them to the buffer.
 * Returns received number of bytes or -1 if error occurs.
 * 
 */
int recv_bytes(int sock, char* buffer, int byte_count);
int send_txt(int sock, char *txt);
int recv_txt(int sock);
void send_greetings(int sock);
int recv_txt_buffer(int sock, char *buffer);

#endif
