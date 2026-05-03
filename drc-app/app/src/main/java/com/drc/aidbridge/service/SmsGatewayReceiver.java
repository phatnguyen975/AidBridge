package com.drc.aidbridge.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Log;

import com.drc.aidbridge.data.repository.gateway.SmsGatewayRepository;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SmsGatewayReceiver extends BroadcastReceiver {

    private static final String TAG = "AidBridgeSmsGateway";

    @Inject
    SmsGatewayRepository smsGatewayRepository;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(intent.getAction())) {
            return;
        }

        SmsMessage[] messages = Telephony.Sms.Intents.getMessagesFromIntent(intent);
        if (messages == null || messages.length == 0) {
            return;
        }

        PendingResult pendingResult = goAsync();
        String sender = messages[0].getOriginatingAddress();
        long receivedAt = System.currentTimeMillis();
        StringBuilder bodyBuilder = new StringBuilder();
        for (SmsMessage message : messages) {
            if (message == null) {
                continue;
            }
            String body = message.getMessageBody();
            if (body != null) {
                bodyBuilder.append(body);
            }
            if (message.getTimestampMillis() > 0L) {
                receivedAt = message.getTimestampMillis();
            }
        }

        String rawMessage = bodyBuilder.toString();
        Log.i(TAG, "SMS_GATEWAY_RECEIVED sender=" + safeSender(sender) + " length=" + rawMessage.length());
        smsGatewayRepository.handleIncomingSms(sender, rawMessage, receivedAt, pendingResult::finish);
    }

    private String safeSender(String sender) {
        if (sender == null || sender.trim().isEmpty()) {
            return "unknown";
        }
        String value = sender.trim();
        return value.length() <= 4 ? "****" : "****" + value.substring(value.length() - 4);
    }
}
