package grails.plugin.awssdk.s3

import com.amazonaws.AmazonClientException
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.AmazonS3Exception
import com.amazonaws.services.s3.model.CannedAccessControlList
import com.amazonaws.services.s3.model.ObjectListing
import com.amazonaws.services.s3.model.S3ObjectSummary
import grails.test.mixin.TestFor
import spock.lang.Specification

@TestFor(AmazonS3Service)
class AmazonS3ServiceSpec extends Specification {

    static String BUCKET_NAME = 'bucket'

    static doWithConfig(conf) {
        conf.grails.plugin.awssdk.s3.bucket = BUCKET_NAME
        conf.grails.plugin.awssdk.s3.cnameEnabled = true
        conf.grails.plugin.awssdk.s3.region = 'eu-west-1'
    }

    def setup() {
        // Mock collaborator
        service.client = Mock(AmazonS3Client)
        service.init(BUCKET_NAME)
    }

    /**
     * Tests for deleteFile(String key)
     */
    void "Deleting a file "() {
        when:
        boolean deleted = service.deleteFile('key')

        then:
        deleted
        1 * service.client.deleteObject(BUCKET_NAME, 'key')
    }

    void "Deleting a file with service exception"() {
        when:
        boolean deleted = service.deleteFile('key')

        then:
        !deleted
        1 * service.client.deleteObject(_, _) >> {
            throw new AmazonS3Exception('some exception')
        }
    }

    void "Deleting a file with client exception"() {
        when:
        boolean deleted = service.deleteFile('key')

        then:
        !deleted
        1 * service.client.deleteObject(_, _) >> {
            throw new AmazonClientException('some exception')
        }
    }

    /**
     * Tests for deleteFiles(String prefix)
     */
    void "Deleting files"() {
        when:
        boolean deleted = service.deleteFiles('dir/subdir/*')

        then:
        deleted
        1 * service.client.listObjects(BUCKET_NAME, 'dir/subdir/*') >> {
            def objectListing = [objectSummaries: []]
            3.times {
                S3ObjectSummary summary = new S3ObjectSummary()
                summary.setKey("key$it")
                objectListing.objectSummaries << summary
            }
            objectListing as ObjectListing
        }
        3 * service.client.deleteObject(BUCKET_NAME, _)
    }

    void "Deleting files with invalid prefix"() {
        when:
        boolean deleted = service.deleteFiles('prefix')

        then:
        !deleted
        0 * service.client
    }

    void "Deleting files with service exception"() {
        when:
        boolean deleted = service.deleteFiles('dir/subdir/*')

        then:
        !deleted
        1 * service.client.listObjects(_, _) >> {
            throw new AmazonS3Exception('some exception')
        }
    }

    void "Deleting files with client exception"() {
        when:
        boolean deleted = service.deleteFiles('dir/subdir/*')

        then:
        !deleted
        1 * service.client.listObjects(_, _) >> {
            throw new AmazonClientException('some exception')
        }
    }

    /**
     * Tests for exists(String prefix)
     */
    void "Checking if a file exists"() {
        when:
        boolean exists = service.exists('key')

        then:
        exists
        1 * service.client.listObjects(BUCKET_NAME, 'key') >> {
            S3ObjectSummary summary = new S3ObjectSummary()
            summary.setKey('key')
            [objectSummaries: [summary]] as ObjectListing
        }
    }

    void "Checking if a file does not exists"() {
        when:
        boolean exists = service.exists('key')

        then:
        !exists
        1 * service.client.listObjects(BUCKET_NAME, 'key') >> {
            [] as ObjectListing
        }
    }

    void "Checking if a file exists with invalid key parameter"() {
        when:
        boolean exists = service.exists('')

        then:
        !exists
        0 * service.client
    }

    void "Checking if a file exists with service exception"() {
        when:
        boolean exists = service.exists('prefix')

        then:
        !exists
            1 * service.client.listObjects(_, _) >> {
                throw new AmazonS3Exception('some exception')
            }
    }

    void "Checking if a file exists with client exception"() {
        when:
        boolean exists = service.exists('prefix')

        then:
        !exists
        1 * service.client.listObjects(BUCKET_NAME, _) >> {
            throw new AmazonClientException('some exception')
        }
    }

    /**
     * Tests for generatePresignedUrl(String key, Date expiration)
     */
    void "Generating presigned url"() {
        when:
        String presignedUrl = service.generatePresignedUrl('key', new Date() + 1)

        then:
        presignedUrl == 'http://some.domaine.com/some/path'
        1 * service.client.generatePresignedUrl(BUCKET_NAME, 'key', _) >> new URL('http://some.domaine.com/some/path')
    }

    /**
     * Tests for storeFile(Object input, String type, String filePrefix, String fileExtension, String fileSuffix = '', CannedAccessControlList cannedAcl = CannedAccessControlList.PublicRead)
     */
    InputStream mockInputStream() {
        new InputStream() {
            @Override
            int read() throws IOException {
                return 0
            }
        }
    }

    void "Storing file"() {
        given:
        File file = Mock(File)
        String path = "filePrefix.txt"

        when:
        String url = service.storeFile(file, 'file', 'filePrefix', 'txt')

        then:
        url == "https://s3-eu-west-1.amazonaws.com/${BUCKET_NAME}/${path}"
        1 * service.client.putObject(_)
    }

    void "Storing input"() {
        given:
        InputStream input = mockInputStream()
        String path = "filePrefix.txt"

        when:
        String url = service.storeFile(input, 'file', 'filePrefix', 'txt')

        then:
        url == "https://s3-eu-west-1.amazonaws.com/${BUCKET_NAME}/${path}"
        1 * service.client.putObject(BUCKET_NAME, path, input, _)
    }

    void "Storing pdf input with private ACL"() {
        given:
        InputStream input = mockInputStream()
        String path = "filePrefix.fileSuffix.csv"

        when:
        String url = service.storeFile(input, 'pdf', 'filePrefix', 'csv', 'fileSuffix', CannedAccessControlList.Private)

        then:
        url == "https://s3-eu-west-1.amazonaws.com/${BUCKET_NAME}/${path}"
        1 * service.client.putObject(BUCKET_NAME, path, input, _)
    }

    void "Storing image input"() {
        given:
        InputStream input = mockInputStream()
        String path = "filePrefix.fileSuffix.jpg"

        when:
        String url = service.storeFile(input, 'image', 'filePrefix', 'jpeg', 'fileSuffix')

        then:
        url == "https://s3-eu-west-1.amazonaws.com/${BUCKET_NAME}/${path}"
        1 * service.client.putObject(BUCKET_NAME, path, input, _)
    }

    void "Storing flash input"() {
        given:
        InputStream input = mockInputStream()
        String path = "filePrefix.fileSuffix.swf"

        when:
        String url = service.storeFile(input, 'flash', 'filePrefix', 'swf', 'fileSuffix')

        then:
        url == "https://s3-eu-west-1.amazonaws.com/${BUCKET_NAME}/${path}"
        1 * service.client.putObject(BUCKET_NAME, path, input, _)
    }

    void "Storing input with service exception"() {
        given:
        InputStream input = mockInputStream()

        when:
        String url = service.storeFile(input, 'file', 'filePrefix', 'txt')

        then:
        !url
        1 * service.client.putObject(BUCKET_NAME, _, _, _) >> {
            throw new AmazonS3Exception('some exception')
        }
    }

    void "Storing input with client exception"() {
        given:
        InputStream input = mockInputStream()

        when:
        String url = service.storeFile(input, 'file', 'filePrefix', 'txt')

        then:
        !url
        1 * service.client.putObject(BUCKET_NAME, _, _, _) >> {
            throw new AmazonClientException('some exception')
        }
    }

}