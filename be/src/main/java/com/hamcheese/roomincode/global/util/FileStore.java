package com.hamcheese.roomincode.global.util;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Component
public class FileStore {

    @Value("${file.upload-dir}")
    private String fileDir;

    // [보안] 허용할 확장자 목록
    private static final List<String> ALLOWED_EXTENSIONS = List.of("jpg");

    public String getFullPath(String filename) {
        return fileDir + filename;
    }

    // 파일을 서버에 저장
    public String storeFile(MultipartFile multipartFile) throws IOException {

        // 파일이 비어있는지 확인하고 비어있으면 null 반환
        if (multipartFile.isEmpty()) {
            return null;
        }

        // 사용자가 원래 올린 파일명
        String originName = multipartFile.getOriginalFilename();
        if (originName == null) originName = "";

        // 확장자명 검증
        validateExtension(originName);

        // 서버 저장용 이름 생성
        String storedName = createStoredName(originName);

        // 저장할 폴더가 없는 경우 폴더 생성
        File directory = new File(fileDir);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        // 파일을 실제 물리 경로에 저장
        multipartFile.transferTo(new File(getFullPath(storedName)));

        // DB 저장을 위해 원본 파일명과 서버 저장 파일명을 객체로 묶어 반환
        return storedName;
    }

    // 파일 삭제 메서드
    public void deleteFile(String filename) {
        if (filename == null || filename.isEmpty()) {
            return;
        }

        File file = new File(getFullPath(filename));
        if (file.exists()) {
            file.delete();
        }
    }

    // 확장자 검증 메서드
    private void validateExtension(String originName) {
        String ext = extractExt(originName).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new IllegalArgumentException("지원하지 않는 파일 형식입니다. jpg만 업로드 가능합니다.");
        }
    }

    // 저장용 이름 생성 메서드
    private String createStoredName(String originName) {
        String ext = extractExt(originName);
        String uuid = UUID.randomUUID().toString();

        // ext는 절대 null이 아니므로 empty만 체크
        if (ext.isEmpty()) {
            return uuid;
        }
        return uuid + "." + ext;
    }

    // 파일명에서 확장자 추출
    private String extractExt(String originName) {
        int pos = originName.lastIndexOf(".");
        if (pos == -1) return "";
        return originName.substring(pos + 1);
    }

    // 파일 저장 결과를 담는 DTO
    @Getter
    public static class UploadFile {
        private final String originName; // final 추가

        public UploadFile(String originalFilename) {
            this.originName = originalFilename;
        }

    }
}