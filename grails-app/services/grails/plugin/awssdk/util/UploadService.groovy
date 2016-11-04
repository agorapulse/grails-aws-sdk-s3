package grails.plugin.awssdk.util

import grails.core.GrailsApplication
import org.springframework.web.multipart.MultipartFile

import java.util.zip.GZIPInputStream

class UploadService {

    static lazyInit = false
    static transactional = false

    static int CONNECT_TIMEOUT = 15000
    static int READ_TIMEOUT = 60000

    GrailsApplication grailsApplication

    File downloadFile(String urlString) {
        if (!urlString) {
            return null
        }

        try {
            URL url = new URL(urlString)
            String fileName = urlString.replaceAll('https://', '').replaceAll('http://', '').replaceAll('/', '-')
            BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream("$uploadPath/$fileName"))

            // Detect potential redirects (Code 3xx)
            HttpURLConnection connection = (HttpURLConnection) url.openConnection()
            connection.connectTimeout = CONNECT_TIMEOUT
            connection.readTimeout = READ_TIMEOUT
            int status = connection.getResponseCode();
            if (status == HttpURLConnection.HTTP_MOVED_TEMP
                    || status == HttpURLConnection.HTTP_MOVED_PERM
                    || status == HttpURLConnection.HTTP_SEE_OTHER) {
                url = new URL(connection.getHeaderField('Location'))
            }
            connection.disconnect()

            URLConnection redirectConnection = url.openConnection()
            redirectConnection.connectTimeout = CONNECT_TIMEOUT
            redirectConnection.readTimeout = READ_TIMEOUT

            // uncompressing if needed
            if (redirectConnection.contentEncoding == 'gzip') {
                GZIPInputStream gzipInputStream = new GZIPInputStream(redirectConnection.inputStream)
                out << gzipInputStream
            } else {
                out << redirectConnection.inputStream
            }

            out.close()
            File file = new File("$uploadPath/$fileName")
            return file
        } catch (SocketTimeoutException exception) {
            log.warn exception
            return null
        } catch (FileNotFoundException exception) {
            log.warn(exception)
            return null
        } catch (IOException exception) {
            log.warn(exception)
            return null
        }
    }

	void uploadFile(MultipartFile multipartFile, fileName) {
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
