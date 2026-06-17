package com.example.shrimpiot.service;

import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Locale;

@Service
public class ChatIntentService {

    public String detectIntent(String rawMessage) {
        String message = normalize(rawMessage);

        if (containsAny(message, "online", "offline", "mat ket noi", "con ket noi", "thiet bi co ket noi", "thiet bi song", "thiet bi hoat dong")) {
            return "DEVICE_STATUS";
        }

        if (containsAny(message, "relay", "may bom", "bom", "quat", "suc oxy", "den", "bat hay tat", "dang bat", "dang tat", "trang thai thiet bi chap hanh")) {
            return "RELAY_STATUS";
        }

        if ((message.contains("canh bao") || message.contains("bat thuong")) && containsAny(message, "chua xu ly", "dang mo", "hien co", "nao khong", "open")) {
            return "OPEN_ALERTS";
        }

        if (containsAny(message, "ao hien tai", "trang thai ao", "cam bien moi nhat", "thong so moi nhat", "du lieu moi nhat", "thong so hien tai", "ao bay gio", "dashboard")) {
            return "LATEST_READING";
        }

        return "BASIC_KNOWLEDGE";
    }

    private String normalize(String text) {
        if (text == null) return "";
        String n = Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return n.toLowerCase(Locale.ROOT);
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) return true;
        }
        return false;
    }
}
