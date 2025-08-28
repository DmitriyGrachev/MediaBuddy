package org.hrachov.com.filmproject.service.impl;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;

import com.google.api.client.util.store.FileDataStoreFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class YouTubeSubs {private static final Logger logger = LoggerFactory.getLogger(YouTubeSubs.class);

    public String getSubsWithYtdlp(String video) throws IOException, InterruptedException {
        // Создаем временный файл для сохранения субтитров
        String videoUrl = "https://www.youtube.com/watch?v=" + video;
        String tempFileName = UUID.randomUUID().toString();
        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        File tempFile = new File(tempDir, tempFileName);

        // Команда yt-dlp
        /*
        ProcessBuilder pb = new ProcessBuilder(
                "yt-dlp",
                "--skip-download",
                "--write-auto-sub",
                "--sub-lang", "en",
                "--sub-format", "vtt",
                "--convert-subs", "srt",
                "-o", tempFile.getAbsolutePath(),
                videoUrl
        );

         */
        ProcessBuilder pb = new ProcessBuilder(
                "yt-dlp",
                "--skip-download",
                "--write-auto-sub",
                "--sub-lang", "en",
                "--sub-format", "vtt", // оставить только vtt
                "-o", tempFile.getAbsolutePath(),
                videoUrl
        );
        // Перенаправить ошибки в основной поток
        pb.redirectErrorStream(true);

        // Запустить процесс
        Process process = pb.start();

        // Для отладки: читаем вывод команды
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
        }

        // Ждем окончания выполнения
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("yt-dlp failed with exit code " + exitCode);
        }
        // После выполнения yt-dlp
        File parentDir = tempFile.getParentFile();
        String baseName = tempFile.getName(); // например, "d031bffd-0d59-466f-8d90-c360e5ed79a2"
        File[] found = parentDir.listFiles((dir, name) -> name.startsWith(baseName) && name.endsWith(".en.vtt"));

        if (found == null || found.length == 0) {
            throw new IOException("Subtitle file was not created");
        }

        File subtitleFile = found[0];

        if (!subtitleFile.exists()) {
            throw new IOException("Subtitle file was not created");
        }

        // Читаем содержимое файла
        String subtitles = Files.readString(subtitleFile.toPath());

        // После чтения можно удалить временные файлы
        subtitleFile.delete();
        tempFile.delete();

        return subtitles;
    }}