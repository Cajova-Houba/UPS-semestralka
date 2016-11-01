package org.valesz.ups.common.message;

/**
 * Message which will be sent (and received) to server.
 *
 * @author Zdenek Vales
 */
public class Message {

    private final MessageType messageType;
    private final String content;

    public Message(MessageType messageType, String content) {
        this.messageType = messageType;
        this.content = content;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public String getContent() {
        return content;
    }

    /**
     * Returns the length of the message as array of four bytes.
     * Example:
     *  length = 14
     *  return = 014 000 000 000
     * @return
     */
    public byte[] getMessageLengthBytes() {
        int l = getMessageLength();

        byte res[] = new byte[4];

        res[0] = (byte)(l >> 24);
        res[1] = (byte)((l << 8) >> 24);
        res[2] = (byte)((l << 16) >> 24);
        res[3] = (byte)((l << 24) >> 24);

        return res;
    }

    public int getMessageLength() {
        return content.length();
    }

    @Override
    public String toString() {
        return "Message{" +
                "messageType=" + messageType +
                ", content='" + content + '\'' +
                '}';
    }

    /**
     * Returns this message as an array of bytes.
     * First four bytes are length of the whole message.
     * @return
     */
    public byte[] toBytes() {
        byte res[] = new byte[4+getMessageLength()];
        byte len[] = getMessageLengthBytes();

        int i = 0;
        while(i < 4) {
            res[i] = len[i];
            i++;
        }
        while (i < res.length) {
            res[i] = (byte)content.charAt(i-4);
            i++;
        }
        return res;
    }
}
