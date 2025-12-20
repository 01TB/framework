package servlet.util.uploads;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;

public class FileManager {
    
    public static final String UPLOAD_DIR = "uploads";

    // Sauvegarde des fichiers uploadés sur le disque
    public static void saveToDisk(HttpServletRequest req, Part filePart, byte[] fileBytes) throws IOException {
        String applicationPath = req.getServletContext().getRealPath("");
        String uploadFilePath = applicationPath + UPLOAD_DIR;

        Path uploadPath = Paths.get(uploadFilePath);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String fileName = getFileName(filePart);
        if (fileName == null || fileName.isEmpty()) {
            return;  // Skip silencieusement si pas de fichier
            // Ou throw new IllegalArgumentException("Pas de fichier valide");
        }

        // Sécurité : éviter les chemins dangereux
        fileName = Paths.get(fileName).getFileName().toString();

        uploadPath = uploadPath.resolve(fileName);
        
        Files.write(uploadPath, fileBytes);
    }

    // Méthode pour extraire le vrai nom du fichier depuis le header
    public static String getFileName(Part part) {
        String contentDisposition = part.getHeader("content-disposition");
        if (contentDisposition != null) {
            for (String cd : contentDisposition.split(";")) {
                if (cd.trim().startsWith("filename")) {
                    String fileName = cd.substring(cd.indexOf('=') + 1).trim().replace("\"", "");
                    return fileName.substring(fileName.lastIndexOf('/') + 1)
                                   .substring(fileName.lastIndexOf('\\') + 1); // pour Windows
                }
            }
        }
        return null;
    }

}
