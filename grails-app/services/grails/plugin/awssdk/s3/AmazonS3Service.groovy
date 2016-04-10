package grails.plugin.awssdk.s3

import com.amazonaws.AmazonClientException
import com.amazonaws.ClientConfiguration
import com.amazonaws.regions.Region
import com.amazonaws.regions.ServiceAbbreviations
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.*
import grails.core.GrailsApplication
import grails.plugin.awssdk.AwsClientUtil
import org.springframework.beans.factory.InitializingBean

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

    protected void init(String streamName) {
        defaultBucketName = streamName
    }

    /**
     * 
     * @param key
     * @return
     */
    boolean deleteFile(String key) {
        assertDefaultBucketName()
        boolean deleted = false
        try {
            client.deleteObject(defaultBucketName, key)
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
        if (prefix.tokenize('/').size() < 2) return false // Multiple delete are only allowed in sub/sub directories
        boolean deleted = false
        try {
            ObjectListing objectListing = client.listObjects(defaultBucketName, prefix)
            objectListing.objectSummaries?.each { S3ObjectSummary summary ->
                client.deleteObject(defaultBucketName, summary.key)
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
    boolean exists(String prefix) {
        assertDefaultBucketName()
        boolean exists = false
        if (!prefix) {
            return false
        }
        try {
            ObjectListing objectListing = client.listObjects(defaultBucketName, prefix)
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
     * @param key
     * @param expirationDate
     * @return
     */
    public String generatePresignedUrl(String key,
                                       Date expirationDate) {
        assertDefaultBucketName()
        client.generatePresignedUrl(defaultBucketName, key, expirationDate).toString()
    }

    /**
     *
     * @param input
     * @param contentType
     * @param filePrefix
     * @param fileExtension
     * @param fileSuffix
     * @param cannedAcl
     * @return
     */
    public String storeFile(Object input,
                            String contentType,
                            String filePrefix,
                            String fileExtension,
                            String fileSuffix = '',
                            CannedAccessControlList cannedAcl = CannedAccessControlList.PublicRead) {
        assertDefaultBucketName()
        if (fileExtension == 'jpeg') {
            fileExtension = 'jpg'
        }
        String path = "${filePrefix}.${fileExtension}"
        if (fileSuffix) {
            path = "${filePrefix}.${fileSuffix}.${fileExtension}"
        }
        ObjectMetadata objectMetadata = new ObjectMetadata()
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

        try {
            if (input instanceof File) {
                PutObjectRequest por = new PutObjectRequest(defaultBucketName, path, input)
                por.setCannedAcl(cannedAcl)
                client.putObject(por)
            } else {
                if (contentInfo.contentDisposition) {
                    objectMetadata.setContentDisposition(contentInfo.contentDisposition)
                }
                objectMetadata.setContentType(contentInfo.contentType)
                if (cannedAcl == CannedAccessControlList.PublicRead) {
                    objectMetadata.setHeader('x-amz-acl', 'public-read')
                }
                client.putObject(defaultBucketName, path, input, objectMetadata)
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
            return "https://${region.name == AwsClientUtil.DEFAULT_REGION ? 's3' : "s3-${region.name}"}.amazonaws.com/${defaultBucketName}/${path}"
        } else {
            return "${client.endpoint}/${defaultBucketName}/${path}".replace('http://', 'https://')
        }
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

    private Boolean getS3CnameEnabled() {
        def s3CnameEnabled = serviceConfig?.cnameEnabled ?: false
        s3CnameEnabled?.toBoolean()
    }

}
