# Clojure in Production - Lesson 03

Let's start off by starting a REPL, as described in the beginning of lesson 01:
[An AWS REPL with Amazonica](lesson-01.md#an-aws-repl-with-amazonica).

## The goal

What we'll do today is create an AWS Lambda that can process incoming events, then attach it to our SQS queue so that it will be invoked every time there's a new batch of messages published to the queue.

## The anatomy of a Lambda

AWS Lambda offers function as a service, and offers JVM, NodeJS, Python, and Go runtimes out of the box. It also has the capacity to supply a custom runtime. For us, the JVM runtime is perfect. We'll need to create a Lambda function, providing it with an uberjar containing a handler class that will be the entry point for our business logic when the Lambda is invoked.

## The Lambda handler

We'll be using the Lambada library to handle the handler. It contains a `deflambdafn` macro that will automatically generate a Java class that implements the [`com.amazonaws.services.lambda.runtime.RequestStreamHandler`](https://docs.aws.amazon.com/lambda/latest/dg/java-handler-io-type-stream.html) from the AWS Lambda Java SDK.

If you're using tools.deps, add the following to the `:deps` map in your `deps.edn`:

```clj
uswitch/lambada {:mvn/version "0.1.2"}
```

If you're using Leiningen, add the following to the `:dependencies` vector in your `project.clj`:

```clj
[uswitch/lambada "0.1.2"]
```

Now, let's create a `src/lambda.clj` file and add a `ProcessEvent` lambda handler to it:

```clj
(ns lambda
  (:require [uswitch.lambada.core :refer [deflambdafn]]))

(deflambdafn com.klarna.cljprod.ProcessEvents [in out ctx])
```

When our lambda is invoked, our handler will be called with an input stream containing the request, an output stream to which we should write the response, and a lambda context (which we can ignore for now).

## Processing the request

The request will be a batch of events, encoded in JSON. In order to do something useful, we'll parse the JSON stream into a Clojure data structure. For this, we'll need to add Cheshire and Clojure's standard Java IO library to our requires:

```clj
(ns lambda
  (:require [cheshire.core :as json]
            [clojure.java.io :as io]
            [uswitch.lambada.core :refer [deflambdafn]]))
```

Now, inside our handler, we can deal with the JSON:

```clj
(deflambdafn com.klarna.cljprod.ProcessEvents [in out ctx]
  (let [events (json/parse-stream (io/reader in))]
    (println events)))
```

For the moment, our lambda won't do anything more than print its input.

## Building an uberjar

In order to test out the functionality of our lambda, we'll need to create a lambda function. In order to create the function, we'll need to build an uberjar. Let's do that first!

### Leiningen

If you're using Leiningen, you'll need to add the following to your `:profiles` map:

```clj
             :uberjar {:aot :all}
```

And then the following to the top level of your `defproject`:

```clj
  :uberjar-name "clj-prod.jar"
```

Now, run the following from your shell:

```sh
lein uberjar
```

The compiler will whirl and hum and produce a 100+ MB `target/clj-prod.jar` file. That's waaaay too big for uploading to Lambda (the limit is [50 MB](https://docs.aws.amazon.com/lambda/latest/dg/limits.html) as of this writing). The main reason for this is Amazonica, which pulls in the full AWS SDK by default.

Let's add some exclusions to our Amazonica dependency in `project.clj`:

```clj
                 [amazonica "0.3.139" :exclusions [com.amazonaws/aws-java-sdk
                                                   com.amazonaws/amazon-kinesis-client
                                                   com.amazonaws/dynamodb-streams-kinesis-adapter]]
```

Now we can explicitly require the libraries that we need. So far, that's just DynamoDB SDK, so we can add that to the `:dependencies` vector:

```clj
                 [com.amazonaws/aws-java-sdk-dynamodb "1.11.475"]
```

We can add back the full SDK in our `:dev` profile so that we can play in the REPL to our heart's content:

```clj
                   :dependencies [[com.amazonaws/aws-java-sdk "1.11.475"]]
```

If you run `lein uberjar` again, you should get a `target/clj-prod.jar` that weighs in at around 14 MB.

### tools.deps

As tools.deps is a tool for managing dependencies, not a build tool like Leiningen, we'll need to use an external tool to build our uberjar. We'll go for [JUXT's Pack](https://github.com/juxt/pack.alpha), for which we'll add two new aliases to our `deps.edn`:

```clj
 :aliases {:aot {:extra-paths ["classes"]
                 :main-opts ["-e" "(compile,'lambda)"]}
            :pack {:extra-deps {pack/pack.alpha {:git/url "https://github.com/juxt/pack.alpha.git"
                                                 :sha "81b9e47d992b17aa3e3af1a47aed1f0287ebe9b8"}}
                  :main-opts ["-m"]}}
```

We can now build an uberjar by running:

```sh
mkdir -p classes target
clojure -A:aot
clojure -A:pack mach.pack.alpha.aws-lambda -C:aot target/clj-prod.jar
```

We'll now have a 100+ MB `target/clj-prod.jar` file sitting around. As noted in the Leiningen section, this is too big. We can do the same paring down of the AWS SDK, this time by creating a new `:uberjar` alias in `deps.edn`:

```clj
           :uberjar {:override-deps {amazonica {:mvn/version "0.3.139"
                                                :exclusions [com.amazonaws/aws-java-sdk
                                                             com.amazonaws/amazon-kinesis-client
                                                             com.amazonaws/dynamodb-streams-kinesis-adapter]}}
                     :extra-deps {com.amazonaws/aws-java-sdk-dynamodb {:mvn/version "1.11.475"}}}
```

Now, we'll need to use that alias when building our uberjar:

```sh
clojure -A:pack mach.pack.alpha.aws-lambda -A:uberjar -C:aot target/clj-prod.jar
```

The resulting `target/clj-prod.jar` should be around 12 MB.

## Creating a Lambda function

To create our lambda function, let's require in the Lambda namespace in our REPL:

```clj
(require '[amazonica.aws.lambda :as lambda])
```

Now, we can call `lambda/create-function` to create our lambda. We'll need to provide:
- A function name
- An IAM role
- A runtime
- A handler name
- Some code

The first four are easy:
- `clj-prod-YOURNAME-events`
- `arn:aws:iam::AWSACCOUNT:role/clj.prod`
- `java8`
- `com.klarna.cljprod.ProcessEvents`

But providing the code is a little more tricky. According to the AWS documentation for [CreateFunctionRequest](https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/lambda/model/CreateFunctionRequest.html), we'll need to provide a [FunctionCode](https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/lambda/model/FunctionCode.html) object, which can either point to an object in an S3 bucket or contain the "[base64-encoded contents of the deployment package](https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/lambda/model/FunctionCode.html#setZipFile-java.nio.ByteBuffer-)". We'll do the latter because it sounds cooler.

Let's write a utility function in the `user` namespace to do this exciting stuff. We'll need to require in `clojure.java.io` and import `java.io.ByteArrayOutputStream` and `java.nio.ByteBuffer`:

```clj
(ns user
  (:require [amazonica.core :as amazonica]
            [clojure.java.io :as io])
  (:import (java.io ByteArrayOutputStream)
           (java.nio ByteBuffer)))
```

Now, we can write a function that takes a filename and reads the file into a `ByteBuffer`, which is what `create-function` requires:

```clj
(defn read-uberjar [filename]
  (with-open [out (ByteArrayOutputStream.)]
    (io/copy (io/input-stream filename) out)
    (ByteBuffer/wrap (.toByteArray out))))
```

Note there's no base 64 encoding happening here. [The documentation](https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/lambda/model/FunctionCode.html#setZipFile-java.nio.ByteBuffer-) provides some small print on this:
> The AWS SDK for Java performs a Base64 encoding on this field before sending this request to the AWS service. Users of the SDK should not perform Base64 encoding on this field.

Don't forget to evaluate this file in your REPL!

Now that we have this magical function, we can get on with the business of creating the lambda function.

```clj
(lambda/create-function :function-name "clj-prod-YOURNAME-events"
                        :code {:zip-file (read-uberjar "target/clj-prod.jar")}
                        :handler "com.klarna.cljprod.ProcessEvents"
                        :role "arn:aws:iam::AWSACCOUNT:role/clj.prod"
                        :runtime "java8")
```

## Testing your function

When your function is invoked by SQS, it will receive a payload that looks something like this:

```js
{
  "Records": [
    {
      "messageId": "19dd0b57-b21e-4ac1-bd88-01bbb068cb78",
      "receiptHandle": "MessageReceiptHandle",
      "body": "Hello from SQS!",
      "attributes": {
        "ApproximateReceiveCount": "1",
        "SentTimestamp": "1523232000000",
        "SenderId": "123456789012",
        "ApproximateFirstReceiveTimestamp": "1523232000001"
      },
      "messageAttributes": {},
      "md5OfBody": "7b270e59b47ff90a553787216d55d91d",
      "eventSource": "aws:sqs",
      "eventSourceARN": "arn:aws:sqs:eu-west-1:123456789012:MyQueue",
      "awsRegion": "eu-west-1"
    }
  ]
}
```

Go ahead and save that to a file named `resources/sqs-test.json`.

We can try out our function by calling `lambda/invoke`:

```clj
(lambda/invoke :function-name "clj-prod-YOURNAME-events"
               :payload (slurp "resources/sqs-test.json"))
;; => {:executed-version "$LATEST", :payload #object[java.nio.HeapByteBuffer 0x13e43b95 "java.nio.HeapByteBuffer[pos=0 lim=0 cap=0]"], :status-code 200}
```

Looks like it succeeded! But it would be lovely to see what was printed by the lambda, right?

Luckily, lambdas write all of their output directly to Cloudwatch Logs.

```clj
(require '[amazonica.aws.logs :as logs])

(logs/describe-log-streams :log-group-name "/aws/lambda/clj-prod-YOURNAME-events")
;;=> {:log-streams [{:first-event-timestamp 1551999807068, :stored-bytes 0, :last-event-timestamp 1551999807068, :last-ingestion-time 1551999840627, :arn "arn:aws:logs:eu-west-1:AWSACCOUNT:log-group:/aws/lambda/clj-prod-YOURNAME-events:log-stream:2019/03/07/[$LATEST]5e2ee4756b7c43f4ba8559f6310cec57", :log-stream-name "2019/03/07/[$LATEST]5e2ee4756b7c43f4ba8559f6310cec57", :upload-sequence-token "49592735214926477653224958681524262503020950481350498738", :creation-time 1551999803177}]}
```

Let's write a utility function that returns all the log events from the log stream that's been most recently written to:

```clj
(defn get-latest-log-events [log-group-name]
  (let [get-log-events (fn [log-stream-name]
                         (->> (logs/get-log-events :log-group-name log-group-name
                                                   :log-stream-name log-stream-name)
                              :events
                              (map :messages)))]
    (->> (logs/describe-log-streams :log-group-name log-group-name)
         :log-streams
         (sort-by :last-event-timestamp)
         last
         get-log-events)))
```

Now we can use this function to see what's going on with our lambda:

```clj
(get-latest-log-events "/aws/lambda/clj-prod-YOURNAME-events")
;;=> ("START RequestId: 8fe3a45a-1f30-4104-8ab3-baa80d14d40f Version: $LATEST\n" "{Records [{body Hello from SQS!, md5OfBody 7b270e59b47ff90a553787216d55d91d, eventSourceARN arn:aws:sqs:eu-west-1:123456789012:MyQueue, eventSource aws:sqs, awsRegion eu-west-1, attributes {ApproximateReceiveCount 1, SentTimestamp 1523232000000, SenderId 123456789012, ApproximateFirstReceiveTimestamp 1523232000001}, messageId 19dd0b57-b21e-4ac1-bd88-01bbb068cb78, receiptHandle MessageReceiptHandle, messageAttributes {}}]}\n" "END RequestId: 8fe3a45a-1f30-4104-8ab3-baa80d14d40f\n" "REPORT RequestId: 8fe3a45a-1f30-4104-8ab3-baa80d14d40f\tDuration: 1166.87 ms\tBilled Duration: 1200 ms \tMemory Size: 256 MB\tMax Memory Used: 138 MB\t\n")
```

## Hooking it up to SQS

Create an events queue as per [lesson 02](lesson-02.md#creating-a-queue) if it doesn't already exist.

In order to have your lambda automatically invoked by SQS when new messages arrive, you'll need to create an event source mapping between your queue and your lambda:

```clj
(lambda/create-event-source-mapping :event-source-arn "arn:aws:sqs:eu-west-1:AWSACCOUNT:clj-prod-YOURNAME-events"
                                    :function-name "clj-prod-YOURNAME-events")
```

You can now test this by [following the steps from lesson 02](lesson-02.md#writing-to-the-queue) to put an event on the queue.

Re-fetching the logs should show a lambda invocation that handles this event:

```clj
(get-latest-log-events "/aws/lambda/clj-prod-YOURNAME-events")
;;=> ***
```
