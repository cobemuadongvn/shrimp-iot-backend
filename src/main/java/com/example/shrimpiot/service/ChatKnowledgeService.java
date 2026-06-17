package com.example.shrimpiot.service;

import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Locale;

@Service
public class ChatKnowledgeService {

    public String answerBasicQuestion(String rawMessage) {
        String message = normalize(rawMessage);

        if (containsAny(message, "ph", "do axit", "do kiem")) {
            return "pH là chỉ số thể hiện độ axit/kiềm của nước. Trong ao nuôi tôm, pH quá thấp hoặc quá cao đều có thể làm tôm bị stress, giảm ăn và dễ phát sinh bệnh. Ngưỡng tham khảo trong hệ thống hiện là khoảng 6.5–8.5. Nếu pH thấp, nên kiểm tra nguồn nước, hạn chế thay đổi đột ngột và xử lý theo quy trình kỹ thuật của ao.";
        }

        if (containsAny(message, "oxy", "do", "hoa tan", "suc oxy", "oxygen")) {
            return "Oxy hòa tan (DO) là lượng oxy có trong nước để tôm/cá hô hấp. DO thấp là tình huống nguy hiểm vì có thể làm tôm nổi đầu, yếu và chết hàng loạt. Hệ thống hiện cảnh báo khi DO dưới 4 mg/L. Khi DO thấp, nên bật máy sục oxy/quạt nước, kiểm tra mật độ nuôi, chất hữu cơ và theo dõi lại thông số sau xử lý.";
        }

        if (containsAny(message, "nhiet do", "temperature", "nong", "lanh")) {
            return "Nhiệt độ nước ảnh hưởng trực tiếp đến trao đổi chất, khả năng ăn và sức khỏe của tôm. Nhiệt độ quá cao làm tôm stress và giảm oxy hòa tan; nhiệt độ quá thấp làm tôm chậm lớn. Hệ thống hiện đặt ngưỡng tham khảo khoảng 20–35°C. Khi nhiệt độ cao, nên tăng cường quạt nước/sục oxy và hạn chế gây sốc môi trường.";
        }

        if (containsAny(message, "do man", "salinity", "man")) {
            return "Độ mặn thể hiện lượng muối hòa tan trong nước. Mỗi đối tượng nuôi có khoảng độ mặn phù hợp riêng. Độ mặn thay đổi đột ngột có thể làm tôm bị sốc. Khi độ mặn bất thường, nên kiểm tra nguồn nước cấp, mưa lớn, bốc hơi và điều chỉnh từ từ, tránh thay đổi quá nhanh.";
        }

        if (containsAny(message, "canh bao", "bat thuong", "xu ly", "warning", "danger")) {
            return "Khi có cảnh báo, trước tiên cần xác định thông số nào vượt ngưỡng, mức độ nghiêm trọng và thời điểm phát sinh. Với DO thấp nên ưu tiên bật sục oxy/quạt nước. Với pH bất thường nên kiểm tra nguồn nước và xử lý theo quy trình kỹ thuật. Với nhiệt độ cao nên tăng cường oxy và theo dõi sát. Sau khi xử lý cần xác nhận cảnh báo và ghi chú lại trong nhật ký vận hành.";
        }

        return "Tôi có thể hỗ trợ hỏi đáp kiến thức nuôi tôm như pH, nhiệt độ, oxy hòa tan, độ mặn và hướng dẫn xử lý cảnh báo. Bạn cũng có thể hỏi: 'Ao hiện tại thế nào?', 'Cảm biến mới nhất?', 'Có cảnh báo chưa xử lý không?', 'Relay đang bật hay tắt?', hoặc 'Thiết bị online không?'.";
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
