package servlet.util.uploads;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@WebServlet("/upload")
@MultipartConfig(
    fileSizeThreshold = 1024 * 1024 * 2,  // 2 Mo en mémoire
    maxFileSize = 1024 * 1024 * 50,       // 50 Mo max par fichier
    maxRequestSize = 1024 * 1024 * 100    // 100 Mo max pour la requête entière
)
public class UploadServlet extends HttpServlet {

    // Dossier où stocker les fichiers uploadés (à adapter)
    private static final String UPLOAD_DIR = "uploads";

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/html;charset=UTF-8");
        request.setCharacterEncoding("UTF-8");

        // Récupération du dossier réel sur le serveur
        String applicationPath = request.getServletContext().getRealPath("");
        String uploadFilePath = applicationPath + UPLOAD_DIR;

        // Créer le dossier s'il n'existe pas
        Path uploadPath = Paths.get(uploadFilePath);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String message = "";
        String erreur = "";

        try {
            // Récupérer la description (champ texte normal)
            String description = request.getParameter("description");
            if (description == null) description = "";

            // Récupérer le fichier uploadé
            Part filePart = request.getPart("fichier"); // "fichier" = name du input file
            String fileName = getFileName(filePart);

            if (fileName == null || fileName.isEmpty()) {
                erreur = "Aucun fichier sélectionné.";
            } else {
                // Sécurité : éviter les chemins dangereux (../ etc.)
                fileName = Paths.get(fileName).getFileName().toString();

                // Chemin complet du fichier sur le serveur
                Path filePath = uploadPath.resolve(fileName);

                // Sauvegarde du fichier
                filePart.write(filePath.toString());

                message = "Fichier uploadé avec succès !<br>" +
                          "Nom : " + fileName + "<br>" +
                          "Taille : " + filePart.getSize() + " octets<br>" +
                          "Description : " + description;
            }

        } catch (Exception e) {
            erreur = "Erreur lors de l'upload : " + e.getMessage();
            e.printStackTrace();
        }

        // Retour vers le formulaire avec messages
        request.setAttribute("message", message);
        request.setAttribute("erreur", erreur);
        request.getRequestDispatcher("/upload.html").forward(request, response);
    }

    // Méthode pour extraire le vrai nom du fichier depuis le header
    private String getFileName(Part part) {
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
