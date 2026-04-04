package com.drc.aidbridge.modules.aid.internal.service;

import org.springframework.web.multipart.MultipartFile;

public interface SpeechToTextService {
    String transcribe(MultipartFile audioFile);
}
