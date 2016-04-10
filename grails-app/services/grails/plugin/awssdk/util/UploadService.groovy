package grails.plugin.awssdk.util

import grails.core.GrailsApplication
import org.springframework.web.multipart.MultipartFile

class UploadService {

    GrailsApplication grailsApplication

    /**
     *
     * @param urlString
     * @return
     */
    File downloadFile(String urlString) {
        if (!urlString) {
            return null
        }

        try {
            URL url = new URL(urlString)
            String fileName = url.file.tokenize('/')[-1]
            BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream("$uploadPath/$fileName"))
            out << url.openStream()
            out.close()
            File file = new File("$uploadPath/$fileName")
            return file
        } catch (FileNotFoundException exception) {
            log.warn(exception)
            return null
        } catch (IOException exception) {
            log.warn(exception)
            return null
        }
    }

    /**
     *
     * @param multipartFile
     * @param fileName
     */
    void uploadFile(MultipartFile multipartFile,
                    fileName) {
        multipartFile.transferTo(new File("${uploadPath}/${fileName}"))
    }

    // PRIVATE

    def getConfig() {
        grailsApplication.config.grails?.plugins?.awssdk ?: grailsApplication.config.grails?.plugin?.awssdk
    }

    private String getUploadPath() {
        config['s3'] ? config['s3'].uploadPath ?: '/var/tmp' : '/var/tmp'
    }

}
