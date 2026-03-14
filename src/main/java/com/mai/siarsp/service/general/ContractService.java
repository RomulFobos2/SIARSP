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
import java.util.Set;
import java.util.UUID;

/**
 * Сервис загрузки, скачивания и удаления файлов контрактов (договоров на отгрузку).
 *
 * Допустимые форматы: PDF, DOC, DOCX.
 * Путь хранения настраивается через свойство {@code contract.upload.path} в application.properties.
 */
@Service
@Slf4j
public class ContractService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "doc", "docx");

    @Value("${contract.upload.path:C:/siarsp-uploads/contracts}")
    private String uploadPathValue;

    private static String uploadPath;

    @PostConstruct
    public void init() {
        uploadPath = uploadPathValue;
        log.info("ContractService инициализирован: uploadPath = {}", uploadPath);
    }

    /**
     * Загружает файл контракта на сервер.
     *
     * @param file MultipartFile из формы
     * @return имя сохранённого файла (UUID_originalname)
     * @throws IOException              при ошибке сохранения
     * @throws IllegalArgumentException если файл пустой или недопустимый формат
     */
    public static String uploadContract(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Файл контракта пустой или null");
        }

        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException(
                    "Недопустимый формат файла: " + extension + ". Допустимые: PDF, DOC, DOCX");
        }

        String fileName = UUID.randomUUID() + "_" + originalFilename;
        Path filePath = Paths.get(uploadPath).resolve(fileName);

        Files.createDirectories(filePath.getParent());
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        log.info("Контракт загружен: {}", fileName);
        return fileName;
    }

    /**
     * Возвращает Resource для скачивания файла контракта.
     *
     * @param contractFileName имя файла контракта
     * @return Resource
     * @throws IOException если файл не найден или нечитаем
     */
    public static Resource getContractData(String contractFileName) throws IOException {
        Path filePath = Paths.get(uploadPath).resolve(contractFileName);
        Resource resource = new UrlResource(filePath.toUri());

        if (resource.exists() || resource.isReadable()) {
            return resource;
        }

        throw new IOException("Не удалось прочитать файл контракта: " + contractFileName);
    }

    /**
     * Удаляет файл контракта (используется при замене контракта).
     * Не выбрасывает исключений — только логирует ошибки.
     *
     * @param contractFileName имя файла контракта
     */
    public static void deleteContract(String contractFileName) {
        try {
            Path filePath = Paths.get(uploadPath).resolve(contractFileName);
            if (Files.deleteIfExists(filePath)) {
                log.info("Контракт удалён: {}", contractFileName);
            }
        } catch (IOException e) {
            log.error("Ошибка при удалении контракта '{}': {}", contractFileName, e.getMessage());
        }
    }

    /**
     * Извлекает расширение файла (в нижнем регистре).
     */
    private static String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }
}
