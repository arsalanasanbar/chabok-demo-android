package com.adp.chabok.data;

import com.adp.chabok.data.models.CaptainMessage;
import com.adp.chabok.data.models.MessageTO;

import java.util.List;


public interface ChabokDAO {

    int DATABASE_VERSION = 5;

    String DATABASE_NAME = "chabok.sqlite";

    String TABLE_NAME_MESSAGE = "chabok_message";
    String TABLE_NAME_CAPTAIN_MESSAGE = "captain_message";

    String TABLE_MESSAGE = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME_MESSAGE + "(id INTEGER PRIMARY KEY AUTOINCREMENT, serverId VARCHAR, message VARCHAR, sentDate INTEGER, " +
            "receivedDate INTEGER, read INTEGER , data VARCHAR , type INTEGER, senderId VARCHAR , counter INTEGER , send_status INTEGER)";

    String ALTER_TABLE_MESSAGE = "ALTER TABLE  " + TABLE_NAME_MESSAGE + " ADD COLUMN send_status INTEGER";

    String TABLE_CAPTAIN_MESSAGE = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME_CAPTAIN_MESSAGE + "(id INTEGER PRIMARY KEY AUTOINCREMENT, message VARCHAR, sentDate INTEGER, " +
            "receivedDate INTEGER, read INTEGER , data VARCHAR)";


    MessageTO saveMessage(MessageTO messageTO, int type);

    List<MessageTO> getMessages();

    List<MessageTO> getMessages(String orderByColumn);

    void deleteAllMessages();

    void deleteMessages(String id);

    void updateCounter(String serverId);

    void updateSendStatus(String messageId);

    int getUnreadMessagesCount();

    CaptainMessage saveCaptainMessage(CaptainMessage message);

    List<CaptainMessage> getCaptainMessages();
}
