package org.hrachov.com.filmproject.service.impl;

import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.*;

@Service
public class TextSummarizerImpl {
    public String getSummary(String youtubeText) throws Exception {

        String pythonPath = "D:\\Python\\python.exe";
        String scriptPath = "D:\\Python\\PythonProjects\\summarize.py";

        // Save text to a temp file
        File tempFile = File.createTempFile("input", ".txt");
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tempFile), "UTF-8"))) {
            writer.write(youtubeText);
        }

        // Build process
        ProcessBuilder pb = new ProcessBuilder(pythonPath, scriptPath, tempFile.getAbsolutePath());
        pb.redirectErrorStream(true); // Merge stderr and stdout
        System.out.println("Running command: " + String.join(" ", pb.command()));

        Process process = pb.start();

        // Read output
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"));
        StringBuilder output = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            System.out.println("Process output: " + line); // print every line immediately
            output.append(line).append("\n");
        }

        int exitCode = process.waitFor();
        System.out.println("Python exited with code: " + exitCode);

        if (exitCode == 0) {
            System.out.println("Суммаризация:\n" + output.toString());
        } else {
            System.out.println("Ошибка:\n" + output.toString());
        }

        String summaryResultWithLogs = output.toString();
        String summaryResult = summaryResultWithLogs.substring(summaryResultWithLogs.lastIndexOf("## Key Points",summaryResultWithLogs.length()-1));
        tempFile.delete(); // delete temp file
        System.out.println("Here is summary for this video\n" + summaryResult);
        return summaryResult;
    }
}
