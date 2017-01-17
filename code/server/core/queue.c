
#include "queue.h"

/*
 * Returns the number of elements in queue.
 */
int get_queue_size(struct Queue *queue) {
    struct Node* n = queue->first;
    int count = 0;

    while(n != NULL) {
        count++;
        n = n->next;
    }

    return count;
}

/*
 * Inserts the player to the end of the queue.
 * If the queue is full, returns -1. Otherwise 0.
 */
int insert_to_queue(struct Queue* queue, struct Player player) {
    int size = get_queue_size(queue);
    if(size == MAX_QUEUE_SIZE) {
        return -1;
    }

    // new node
    struct Node* n = malloc(sizeof(struct Node));
    n->player = player;
    n->next = NULL;

    // empty queue
    if(queue->first == NULL) {
        queue->first = n;
        queue->last = n;
    } else {
        queue->last->next = n;
    }

    return 0;
}

/*
 * Removes the queue->first from queue and returns it.
 * If the queue is empty, returns null.
 */
void remove_from_queue(struct Queue *queue, struct Player *player) {
    if(queue->first == NULL) {
        player = NULL;
        return;
    }

    struct Node tmp = *(queue->first);
    free(queue->first);
    queue->first = tmp.next;
    if(tmp.next == NULL) {
        queue->last = NULL;
    }

    *player = tmp.player;
}

/*
 * Frees all the nodes in queue and sets queue->first and last to NULL.
 */
void clear_queue(struct Queue* queue) {
    struct Node* n = queue->first;
    struct Node* tmp;

    while ( n != NULL) {
        tmp = n->next;
        free(n);
        n = tmp;
    }
}

