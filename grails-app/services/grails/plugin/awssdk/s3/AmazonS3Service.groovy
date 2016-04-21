package grails.plugin.awssdk.s3

import com.amazonaws.AmazonClientException
import com.amazonaws.ClientConfiguration
import com.amazonaws.regions.Region
import com.amazonaws.regions.ServiceAbbreviations
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.*
import com.amazonaws.services.s3.transfer.TransferManager
import com.amazonaws.services.s3.transfer.Upload
import grails.core.GrailsApplication
import grails.plugin.awssdk.AwsClientUtil
import groovy.util.logging.Log4j
import org.springframework.beans.factory.InitializingBean

@Log4j
class AmazonS3Service implements InitializingBean {

    static SERVICE_NAME = ServiceAbbreviations.S3
    static final Map HTTP_CONTENTS = [
            audio: [contentType: 'audio/mpeg'],
            csv:   [contentType: 'text/csv', contentDisposition: 'attachment'],
            excel: [contentType: 'application/vnd.ms-excel', contentDisposition: 'attachment'],
            flash: [contentType: 'application/x-shockwave-flash'],
            pdf:   [contentType: 'application/pdf'],
            file:  [contentType: 'application/octet-stream', contentDisposition: 'attachment'],
            video: [contentType: 'video/x-flv']
    ]

    GrailsApplication grailsApplication
    AmazonS3Client client
    TransferManager transferManager
    private String defaultBucketName = ''

    void afterPropertiesSet() throws Exception {
        // Set region
        Region region = AwsClientUtil.buildRegion(config, serviceConfig)
        assert region?.isServiceSupported(SERVICE_NAME)

        // Create client
        def credentials = AwsClientUtil.buildCredentials(config, serviceConfig)
        ClientConfiguration configuration = AwsClientUtil.buildClientConfiguration(config, serviceConfig)
        client = new AmazonS3Client(credentials, configuration)
                .withRegion(region)

        defaultBucketName = serviceConfig?.bucket ?: ''
    }

    protected void init(String bucketName) {
        defaultBucketName = bucketName
    }

    /**
     *
     * @param bucketName
     * @param region
     */
    void createBucket(String bucketName,
                      String region = '') {
        if (!region) {
            region = serviceConfig.region ?: config.region ?: AwsClientUtil.DEFAULT_REGION
        }
        client.createBucket(bucketName, region)
    }

    /**
     *
     * @param bucketName
     */
    void deleteBucket(String bucketName) {
        client.deleteBucket(bucketName)
    }

    /**
     *
     * @param bucketName
     * @param key
     * @return
     */
    boolean deleteFile(String bucketName,
                       String key) {
        boolean deleted = false
        try {
            client.deleteObject(bucketName, key)
            deleted = true
        } catch (AmazonS3Exception exception) {
            log.warn(exception)
        } catch (AmazonClientException exception) {
            log.warn(exception)
        }
        deleted
    }

    /**
     *
     * @param key
     * @return
     */
    boolean deleteFile(String key) {
        assertDefaultBucketName()
        deleteFile(defaultBucketName, key)
    }

    /**
     *
     * @param String
     * @param bucketName
     * @param prefix
     * @return
     */
    boolean deleteFiles(String bucketName,
                        String prefix) {
        assert prefix.tokenize('/').size() >= 2, "Multiple delete are only allowed in sub/sub directories"
        boolean deleted = false
        try {
            ObjectListing objectListing = listObjects(bucketName, prefix)
            objectListing.objectSummaries?.each { S3ObjectSummary summary ->
                client.deleteObject(bucketName, summary.key)
            }
            deleted = true
        } catch (AmazonS3Exception exception) {
            log.warn(exception)
        } catch (AmazonClientException exception) {
            log.warn(exception)
        }
        deleted
    }

    /**
     *
     * @param prefix
     * @return
     */
    boolean deleteFiles(String prefix) {
        assertDefaultBucketName()
        deleteFiles(defaultBucketName, prefix)
    }

    /**
     *
     * @param String
     * @param bucketName
     * @param prefix
     * @return
     */
    boolean exists(String bucketName,
                   String prefix) {
        boolean exists = false
        if (!prefix) {
            return false
        }
        try {
            ObjectListing objectListing = client.listObjects(bucketName, prefix)
            if (objectListing.objectSummaries) {
                exists = true
            }
        } catch (AmazonS3Exception exception) {
            log.warn(exception)
        } catch (AmazonClientException exception) {
            log.warn(exception)
        }
        exists
    }

    /**
     *
     * @param prefix
     * @return
     */
    boolean exists(String prefix) {
        assertDefaultBucketName()
        exists(defaultBucketName, prefix)
    }

    /**
     *
     * @return
     */
    List listBucketNames() {
        client.listBuckets().collect { it.name }
    }

    /**
     *
     * @param bucketName
     * @param prefix
     * @return
     */
    ObjectListing listObjects(String bucketName,
                              String prefix) {
        client.listObjects(bucketName, prefix)
    }

    /**
     *
     * @param prefix
     * @return
     */
    ObjectListing listObjects(String prefix = '') {
        assertDefaultBucketName()
        listObjects(defaultBucketName, prefix)
    }

    /**
     *
     * @param String
     * @param bucketName
     * @param key
     * @param expirationDate
     * @return
     */
    String generatePresignedUrl(String bucketName,
                                String key,
                                Date expirationDate) {
        client.generatePresignedUrl(bucketName, key, expirationDate).toString()
    }

    /**
     *
     * @param key
     * @param expirationDate
     * @return
     */
    String generatePresignedUrl(String key,
                                Date expirationDate) {
        assertDefaultBucketName()
        generatePresignedUrl(defaultBucketName, key, expirationDate)
    }

    /**
     *
     * @param bucketName
     * @param path
     * @param input
     * @param cannedAcl
     * @param contentType
     * @return
     */
    String storeFile(String bucketName,
                     String path,
                     Object input,
                     CannedAccessControlList cannedAcl = CannedAccessControlList.PublicRead,
                     String contentType = '') {
        try {
            if (input instanceof File) {
                PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, path, input)
                        .withCannedAcl(cannedAcl)
                client.putObject(putObjectRequest)
            } else {
                String fileExtension = path.tokenize('.').last()
                Map contentInfo = buildContentInfo(contentType, fileExtension)
                ObjectMetadata objectMetadata = new ObjectMetadata()
                if (contentInfo.contentDisposition) {
                    objectMetadata.setContentDisposition(contentInfo.contentDisposition)
                }
                objectMetadata.setContentType(contentInfo.contentType)
                if (cannedAcl == CannedAccessControlList.PublicRead) {
                    objectMetadata.setHeader('x-amz-acl', 'public-read')
                }
                client.putObject(bucketName, path, input, objectMetadata)
            }
        } catch (AmazonS3Exception exception) {
            log.error(exception)
            return ''
        } catch (AmazonClientException exception) {
            log.error(exception)
            return ''
        }

        if (s3CnameEnabled) {
            Region region = AwsClientUtil.buildRegion(config, serviceConfig)
            return "https://${region.name == AwsClientUtil.DEFAULT_REGION ? 's3' : "s3-${region.name}"}.amazonaws.com/${bucketName}/${path}"
        } else {
            return "${client.endpoint}/${bucketName}/${path}".replace('http://', 'https://')
        }
    }

    /**
     *
     * @param path
     * @param input
     * @param cannedAcl
     * @param contentType
     * @return
     */
    String storeFile(String path,
                     Object input,
                     CannedAccessControlList cannedAcl = CannedAccessControlList.PublicRead,
                     String contentType = '') {
        assertDefaultBucketName()
        storeFile(defaultBucketName, path, input, cannedAcl, contentType)
    }

    /**
     *
     * @param bucketName
     * @param path
     * @param file
     * @param cannedAcl
     * @param contentType
     * @return
     */
    Upload transferFile(String bucketName,
                        String path,
                        File file,
                        CannedAccessControlList cannedAcl = CannedAccessControlList.PublicRead) {
        if (!transferManager) {
            // Create transfer manager (only create if required, since it may pool connections and threads)
            transferManager = new TransferManager(client)
        }
        PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, path, file)
                .withCannedAcl(cannedAcl)
        transferManager.upload(putObjectRequest)
    }

    /**
     *
     * @param path
     * @param file
     * @param cannedAcl
     * @return
     */
    Upload transferFile(String path,
                        File file,
                        CannedAccessControlList cannedAcl = CannedAccessControlList.PublicRead) {
        assertDefaultBucketName()
        transferFile(defaultBucketName, path, file, cannedAcl)
    }

    // PRIVATE

    boolean assertDefaultBucketName() {
        assert defaultBucketName, "Default bucket must be defined"
    }

    def getConfig() {
        grailsApplication.config.grails?.plugin?.awssdk ?: grailsApplication.config.grails?.plugins?.awssdk
    }

    def getServiceConfig() {
        config[SERVICE_NAME]
    }

    /**
     *
     * @param contentType
     * @param fileExtension
     * @return
     */
    private static Map buildContentInfo(String contentType,
                                        String fileExtension) {
        Map contentInfo
        if (HTTP_CONTENTS[contentType]) {
            contentInfo = HTTP_CONTENTS[contentType] as Map
        } else if (contentType in ['image', 'photo']) {
            contentInfo = [contentType: "image/${fileExtension == 'jpg' ? 'jpeg' : fileExtension}"] // Return image/jpeg for images to fix Safari issue (download image instead of inline display)
        } else if (fileExtension == 'swf') {
            contentInfo = [contentType: "application/x-shockwave-flash"]
        } else {
            contentInfo = [contentType: 'application/octet-stream', contentDisposition: 'attachment']
        }
        contentInfo
    }

    private Boolean getS3CnameEnabled() {
        def s3CnameEnabled = serviceConfig?.cnameEnabled ?: false
        s3CnameEnabled?.toBoolean()
    }

}
