package com.hamcheese.roomincode.global.util;

import com.hamcheese.roomincode.global.config.EmailConfig;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EmailSender {

    private final JavaMailSender javaMailSender;
    private final EmailConfig emailConfig;
    @Value("${app.front.base-url}")
    private String frontBaseUrl;

    @Async
    public void sendSignUpVerifyEmail(String toEmail, String code) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            // true: 멀티파트(이미지, 파일 첨부 가능) 지원 여부, UTF-8 설정
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("[RoomInCode] 이메일 인증 코드가 도착했습니다.");

            // HTML 디자인 적용
            String htmlContent = getVerificationHtml(code, emailConfig.getExpirationMinutes());
            helper.setText(htmlContent, true); // true: HTML 모드 켜기

            javaMailSender.send(message);

        } catch (MessagingException e) {
            e.printStackTrace();
            throw new RuntimeException("메일 발송 실패");
        }
    }

    // HTML 템플릿 생성 메서드 (디자인 적용)
    private String getVerificationHtml(String code, long minutes) {
        String logoUrl = "https://i.imgur.com/oZm2K8H.png"; // (예시) 실제 사용하실 이미지 직접 링크로 교체하세요

        return """
            <!DOCTYPE html>
            <html lang="ko">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Email Verification</title>
                <style>
                    /* 폰트 및 기본 설정 */
                    body { margin: 0; padding: 0; font-family: 'Apple SD Gothic Neo', 'Malgun Gothic', sans-serif; background-color: #050505; color: #ffffff; }
                    .container { width: 100%%; max-width: 600px; margin: 0 auto; padding: 40px 20px; background-color: #050505; }
                    
                    /* 카드 디자인 (스크린샷 감성) */
                    .card {
                        background-color: #0f1014;
                        border: 1px dashed #00E5FF; /* 사이언 색상 점선 테두리 */
                        border-radius: 12px;
                        padding: 40px;
                        text-align: center;
                        box-shadow: 0 0 20px rgba(0, 229, 255, 0.1);
                    }
                    
                    /* 로고 */
                    .logo { margin-bottom: 30px; }
                    .logo img { max-width: 150px; height: auto; }
                    
                    /* 텍스트 스타일 */
                    .step { color: #00E5FF; font-size: 12px; font-weight: bold; letter-spacing: 2px; margin-bottom: 10px; display: inline-block; }
                    h1 { margin: 0 0 20px 0; font-size: 24px; font-weight: 700; color: #ffffff; }
                    p { color: #a1a1aa; font-size: 14px; line-height: 1.6; margin: 0 0 30px 0; }
                    
                    /* 인증 코드 박스 */
                    .code-box {
                        background-color: #18181b;
                        border: 1px solid #3f3f46;
                        border-radius: 8px;
                        padding: 20px;
                        margin: 30px 0;
                    }
                    .auth-code {
                        font-family: 'Courier New', monospace;
                        font-size: 36px;
                        font-weight: bold;
                        color: #00E5FF; /* 사이언 색상 */
                        letter-spacing: 10px;
                        margin: 0;
                    }
                    
                    /* 하단 안내 */
                    .footer { margin-top: 30px; font-size: 12px; color: #52525b; }
                    .footer a { color: #71717a; text-decoration: none; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="card">
                        <!-- 로고 영역 -->
                        <div class="logo">
                             <!-- 로고 이미지가 없다면 텍스트로 대체되거나 엑박이 뜹니다. 꼭 확인해주세요 -->
                             <img src="%s" alt="RoomInCode Logo">
                        </div>

                        <!-- 내용 영역 -->
                        <div class="step">STEP 02 / 03</div>
                        <h1>이메일 인증</h1>
                        <p>
                            안녕하세요, <strong>RoomInCode</strong>입니다.<br>
                            요청하신 회원가입 인증 코드를 전송해 드립니다.<br>
                            아래 코드를 %d분 이내에 입력해주세요.
                        </p>

                        <!-- 인증 코드 -->
                        <div class="code-box">
                            <p class="auth-code">%s</p>
                        </div>
                        
                        <p style="font-size: 13px; color: #ef4444;">
                            ⚠ 이 코드는 %d분 뒤 만료됩니다.
                        </p>

                        <div class="footer">
                            <p>본 메일은 발신 전용입니다.<br>
                            &copy; 2026 RoomInCode</p>
                        </div>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(logoUrl, minutes, code, minutes); // %s 자리에 값 주입
    }

    // 비밀번호 재설정 링크 발송
    @Async
    public void sendPasswordResetEmail(String toEmail, String token) {
        try {
            String resetLink = frontBaseUrl+"/reset/password?token="+token;
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("[RoomInCode] 비밀번호 재설정 링크입니다.");

            // HTML 내용 생성
            String htmlContent = getPasswordResetHtml(resetLink, emailConfig.getExpirationMinutes());
            helper.setText(htmlContent, true);

            javaMailSender.send(message);

        } catch (MessagingException e) {
            e.printStackTrace();
            throw new RuntimeException("메일 발송 실패");
        }
    }

    // HTML 디자인 (버튼 포함)
    private String getPasswordResetHtml(String link, long minutes) {
        return "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #ddd; border-radius: 10px;'>" +
                "<h2 style='color: #333; text-align: center;'>비밀번호 재설정</h2>" +
                "<p style='font-size: 16px; color: #555; line-height: 1.6;'>" +
                "안녕하세요, RoomInCode입니다.<br>" +
                "아래 버튼을 클릭하여 새로운 비밀번호를 설정해주세요." +
                "</p>" +
                "<div style='text-align: center; margin: 30px 0;'>" +
                "  <a href='" + link + "' style='" +
                "    background-color: #4CAF50; color: white; padding: 14px 24px; " +
                "    text-decoration: none; border-radius: 5px; font-size: 16px; font-weight: bold;" +
                "  '>비밀번호 변경하기</a>" +
                "</div>" +
                "<p style='font-size: 14px; color: #888; text-align: center;'>" +
                "이 링크는 <strong style='color: #d9534f;'>" + minutes + "분</strong> 동안만 유효합니다.<br>" +
                "본인이 요청하지 않았다면 이 메일을 무시하셔도 됩니다." +
                "</p>" +
                "</div>";
    }
}