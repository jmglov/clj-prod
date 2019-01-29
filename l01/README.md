# Clojure in Production - Lesson 1

## Getting started

- Install JDK 8 or higher. OpenJDK or the [Oracle JDK](https://www.oracle.com/technetwork/java/javase/downloads/index.html) are both fine.
- Install the [Clojure command line tools](https://clojure.org/guides/getting_started) or [Leiningen](https://leiningen.org/#install).

## An AWS REPL with Amazonica

First, decide whether you're using Lein or the Clojure CLI (i.e. `deps.edn`).
- If you're using Lein, copy [project.clj](project.clj) into your project root.
- If you're using the Clojure CLI, copy [deps.edn](deps.edn) into your project root.

If you open whichever file you chose, you'll see that you are adding two dependencies:
- [Amazonica](https://github.com/mcohen01/amazonica), a Clojure wrapper around the AWS Java SDK
- [Cheshire](https://github.com/dakrone/cheshire), a JSON library

Other than a `project.clj` or `deps.edn`, the only other thing we need for an amazing AWS REPL is a way to authenticate. Copy the [dev/user.clj](dev/user.clj) file from this repo into your project directory.

Before you start a REPL, make sure the `AWS_REGION` environment variable is set to `eu-west-1`. Some quirk in either Amazonica or the AWS credentials provider doesn't seem to always pick up the region from the config file.

Now, start your REPL!

### Authentication

Amazonica has a [few different ways to authenticate](https://github.com/mcohen01/amazonica#authentication), which it tries in this order:
- Environment Variables - `AWS_ACCESS_KEY_ID` and `AWS_SECRET_ACCESS_KEY`
- Java System Properties - `aws.accessKeyId` and `aws.secretKey`
- Credential profiles file at the default location (`~/.aws/credentials`) shared by all AWS SDKs and the AWS CLI
- Instance profile credentials delivered through the Amazon EC2 metadata service

We'll use the profiles method, as it works well with certain tools that manage AWS sessions (such as `aws-adfs-tool`).

If you open [dev/user.clj](dev/user.clj), you'll see that the only thing in it is a magic `refresh-aws-credentials` function. It takes a profile and uses the credentials associated with that profile to authenticate.

Let's go ahead and create a profile in `~/.aws/credentials`. We need to add an entry like this:
```
[clj-prod]
region = eu-west-1
aws_access_key_id = AKIAXXXXXXXXXXXXXXXX
aws_secret_access_key = XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
```

The actual values for the access key and secret key will be provided to you during the workshop.

Once you've created the profile, you can call the `(refresh-aws-credentials)` function in your REPL to authenticate. Note that the function won't report any errors to you--that will happen instead when you try to use one of the service clients. Fun!

## Creating and using a DynamoDB table
## Creating and using an SQS queue
## Hooking it all together with AWS Lambda

## License

See [README](README.md)
