package com.mai.siarsp.service.general;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Работа с изображениями и медиа-файлами, которые прикладываются к сущностям сервиса.
 */

@Service
@Slf4j
public class ImageService {

    @Value("${upload.path:C:/siarsp-uploads/images}")
    private String uploadPathValue;

    private static String uploadPath;

    @PostConstruct
    public void init() {
        uploadPath = uploadPathValue;
        log.info("ImageService инициализирован: uploadPath = {}", uploadPath);
    }

    public static String uploadImage(MultipartFile file) throws IOException {
        log.info("Начинаем загрузку изображения...");
        if (file != null && !file.isEmpty()) {
            String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Path filePath = Paths.get(uploadPath).resolve(fileName);

            Files.createDirectories(filePath.getParent());
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            log.info("Изображение загружено: {}", fileName);
            return fileName;
        }

        throw new IllegalArgumentException("Файл пустой или null");
    }

    public static Resource getImageData(String imageName) throws IOException {
        Path filePath = Paths.get(uploadPath).resolve(imageName);
        Resource resource = new UrlResource(filePath.toUri());

        if (resource.exists() || resource.isReadable()) {
            return resource;
        }

        throw new IOException("Не удалось прочитать файл: " + imageName);
    }
}
