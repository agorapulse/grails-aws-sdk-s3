Grails AWS SDK S3 Plugin
========================

[![Build Status](https://travis-ci.org/agorapulse/grails-aws-sdk-s3.svg?branch=master)](https://travis-ci.org/agorapulse/grails-aws-sdk-s3)

# Introduction

The AWS SDK Plugins allow your [Grails](http://grails.org) application to use the [Amazon Web Services](http://aws.amazon.com/) infrastructure services.
The aim is to provide lightweight utility Grails service wrappers around the official [AWS SDK for Java](http://aws.amazon.com/sdkforjava/).

The following services are currently supported:

* [AWS SDK CloudSearch Grails Plugin](http://github.com/agorapulse/grails-aws-sdk-cloudsearch)
* [AWS SDK DynamoDB Grails Plugin](http://github.com/agorapulse/grails-aws-sdk-dynamodb)
* [AWS SDK Kinesis Grails Plugin](http://github.com/agorapulse/grails-aws-sdk-kinesis)
* [AWS SDK S3 Grails Plugin](http://github.com/agorapulse/grails-aws-sdk-s3)
* [AWS SDK SES Grails Plugin](http://github.om/agorapulse/grails-aws-sdk-ses)
* [AWS SDK SQS Grails Plugin](http://github.com/agorapulse/grails-aws-sdk-sqs)

This plugin encapsulates **Amazon S3** related logic.


# Installation

Add plugin dependency to your `build.gradle`:

```groovy
dependencies {
  ...
  compile 'org.grails.plugins:aws-sdk-s3:2.0.0-beta1'
  ...
```

# Config

Create an AWS account [Amazon Web Services](http://aws.amazon.com/), in order to get your own credentials accessKey and secretKey.


## AWS SDK for Java version

You can override the default AWS SDK for Java version by setting it in your _gradle.properties_:

```
awsJavaSdkVersion=1.10.66
```

## Credentials

Add your AWS credentials parameters to your _grails-app/conf/application.yml_:

```yml
grails:
    plugin:
        awssdk:
            accessKey: {ACCESS_KEY}
            secretKey: {SECRET_KEY}
```

If you do not provide credentials, a credentials provider chain will be used that searches for credentials in this order:

* Environment Variables - `AWS_ACCESS_KEY_ID` and `AWS_SECRET_KEY`
* Java System Properties - `aws.accessKeyId and `aws.secretKey`
* Instance profile credentials delivered through the Amazon EC2 metadata service (IAM role)

## Region

The default region used is **us-east-1**. You might override it in your config:

```yml
grails:
    plugins:
        awssdk:
            region: eu-west-1
```

If you're using multiple AWS SDK Grails plugins, you can define specific settings for each services.

```yml
grails:
    plugin:
        awssdk:
            accessKey: {ACCESS_KEY} # Global default setting 
            secretKey: {SECRET_KEY} # Global default setting
            region: us-east-1       # Global default setting
            s3:
                accessKey: {ACCESS_KEY} # (optional)
                secretKey: {SECRET_KEY} # (optional)
                region: eu-west-1       # (optional)
                bucket: my-bucket       # (optional)
            
```

**bucket**: default bucket to use when calling methods without `bucketName`.

TIP: if you use multiple buckets, you can create a new service for each bucket that inherits from **AmazonS3Service**.

```groovy
class MyBucketService extends AmazonS3Service {

    static final BUCKET_NAME = 'my-bucket'

    @PostConstruct
    def init() {
        init(BUCKET_NAME)
    }

}
```


# Usage

The plugin provides the following Grails artefact:

* **AmazonS3Service**

## Bucket management

```groovy 
// Create bucket
amazonS3Service.createBucket(bucketName)

// List bucket names
amazonS3Service.listBucketNames()

// List objects
amazonS3Service.listObjects('my-bucket', 'some-prefix')

// Delete bucket
amazonS3Service.deleteBucket(bucketName)
```

## File management

```groovy 
// Store a file
MultipartFile multipartFile = request.getFile('file')
if (multipartFile && !multipartFile.empty) {
    amazonS3Service.storeFile('my-bucket', 'asset/foo/' + multipartFile.originalFilename, multipartFile.inputStream, CannedAccessControlList.PublicRead)
    // Or if you have define default bucket
    amazonS3Service.storeFile('asset/foo/' + multipartFile.originalFilename, multipartFile.inputStream, CannedAccessControlList.PublicRead)
}

// Check if an object exists in bucket
found = amazonS3Service.exists('my-bucket', 'assets/foo/fail-0.gif')
// Or if you have define default bucket
found = amazonS3Service.exists('assets/foo/fail-0.gif')

// Generate a pre-signed URL valid during 24h
url = amazonS3Service.generatePresignedUrl('my-bucket', 'assets/foo/fail-0.gif', new Date() + 1)
// Or if you have define default bucket
url = amazonS3Service.generatePresignedUrl('assets/foo/fail-0.gif', new Date() + 1)

// delete a file
deleted = amazonS3Service.deleteFile('my-bucket', 'assets/foo/fail-0.gif')
// Or if you have define default bucket
deleted = amazonS3Service.deleteFile('assets/foo/fail-0.gif') 
```

Supported content types when storing a file:

* audio,
* csv,
* excel,
* flash,
* image,
* pdf,
* file,
* video

If contentType param is empty or blank, S3 will try to set the appropriate content type.

## Advanced usage

If required, you can also directly use **AmazonS3Client** instance available at **amazonS3Service.client**.

For more info, AWS SDK for Java documentation is located here:

* [AWS SDK for Java](http://docs.amazonwebservices.com/AWSJavaSDK/latest/javadoc/index.html)


# Bugs

To report any bug, please use the project [Issues](http://github.com/agorapulse/grails-aws-sdk-s3/issues) section on GitHub.

Feedback and pull requests are welcome!