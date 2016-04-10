Grails AWS SDK S3 Plugin
========================

[![Build Status](https://travis-ci.org/agorapulse/grails-aws-sdk-s3.svg?token=BpxbA1UyYnNoUwrDNXtN&branch=master)](https://travis-ci.org/agorapulse/grails-aws-sdk-s3)

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
                accessKey: {ACCESS_KEY} # Service setting (optional)
                secretKey: {SECRET_KEY} # Service setting (optional)
                region: eu-west-1       # Service setting (optional)
                bucket: my-bucket
                // If you plan to upload files
                uploadPath = '/var/tmp' # Service setting (optional)
            
```

**bucket** allows you define the bucket to use for all requests when using **AmazonS3Service**.

If you use multiple buckets, you can create a new service for each bucket that inherits from **AmazonS3Service**.

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

The plugin provides the following Grails artefacts:

* **AmazonS3Service**
* **UploadService**

To generate a pre-signed URL valid during 24h:

```groovy 
String url = amazonS3Service.generatePresignedUrl('assets/devops-simulator/deployment/fail-0.gif', new Date() + 1)
```

To check if an object exists in config bucket:

```groovy 
boolean found = amazonS3Service.exists('assets/devops-simulator/deployment/fail-0.gif')
```

To delete a file in config bucket:

```groovy 
boolean deleted = amazonS3Service.deleteFile('assets/devops-simulator/deployment/fail-0.gif')
```

To store a file:

```groovy
String fileUrl = amazonS3Service.storeFile(multipartFile.inputStream, 'file', 'assets/myfile', 'txt')   
```

Supported content types:

* audio,
* csv,
* excel,
* flash,
* image,
* pdf,
* file,
* video

If contentType param is empty or blank, S3 will try to set the appropriate content type.

If required, you can also directly use **AmazonS3Client** instance available at **amazonS3Service.client**.

For example, to list objects in a bucket:

```groovy 
// List all buckets
amazonS3Service.client.listBuckets()

// List all objects
amazonS3Service.client.listObjects('my-bucket', 'assets/').objectSummaries?.each {
  println it.key
}
```

For more info, AWS SDK for Java documentation is located here:

* [AWS SDK for Java](http://docs.amazonwebservices.com/AWSJavaSDK/latest/javadoc/index.html)


## UploadService

To download a file from a URL:

```groovy
File file = uploadService.downloadFile('https://s3-eu-west-1.amazonaws.com/benorama/assets/devops-simulator/deployment/fail-0.gif')
```

To upload a file in config `uploadPath`:

```groovy
UploadResult result = new UploadResult()
MultipartFile multipartFile = request.getFile(params.fileName)
if (multipartFile && !multipartFile.empty) {
    try {
        String imageExtension = multipartFile.originalFilename.tokenize('.').last()
        if (imageExtension.toLowerCase() in ['gif', 'jpg', 'jpeg', 'png']) {
            result.fileField = params.fileName
            result.fileName = multipartFile.originalFilename
            result.storeId = "${UUID.randomUUID()}.${imageExtension}"
            uploadService.uploadFile(multipartFile, result.storeId)
        }
    } catch (Exception e) {
        System.out.println("Problem serializing: " + e);
    }
}
```


# Bugs

To report any bug, please use the project [Issues](http://github.com/agorapulse/grails-aws-sdk-s3/issues) section on GitHub.

Feedback and pull requests are welcome!