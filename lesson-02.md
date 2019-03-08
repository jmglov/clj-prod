# Clojure in Production - Lesson 02

Let's start off by starting a REPL, as described in the beginning of lesson 01:
[An AWS REPL with Amazonica](lesson-01.md#an-aws-repl-with-amazonica).

According to [our design](product.md#accepting-events), the checkout system will publish events to an SQS queue of our choosing. Events are JSON objects that look like this:

```json
{
  "date": "2011-12-03T10:15:30Z",
  "amount": 4285,
  "paymentMethod": "SLICE_IT",
  "merchantId": "1bb53ed1-787b-4543-9def-ea18eef7902e"
}
```

Since we have a well-defined data format, let's start by creating specs.

## Event specs

## Creating a queue

Now, let's create a queue that the checkout system will use for publishing events. In our REPL, we need to require in Amazonica's SQS namespace:

```clj
(require '[amazonica.aws.sqs :as sqs])
```

This time, we'll refer to [Amazonica's documentation](https://github.com/mcohen01/amazonica#sqs) to figure out how to create a queue.

Unsurprisingly, there's a function called `create-queue`, which needs a queue name and a map of attributes. The attributes are actually optional--if omitted, the queue will be created with all the default settings, which is just fine for us at this point:

```clj
(sqs/create-queue :queue-name "clj-prod-YOURNAME-events")
;;=> {:queue-url "https://sqs.eu-west-1.amazonaws.com/AWSACCOUNT/clj-prod-YOURNAME-events"}
```

The function returns a map containing the URL of the queue, which we'll need to use the queue in our subsequent work. Let's go ahead and bind it for convenience:

```clj
(def queue-url (:queue-url *1))
```

In most REPLs, `*1` refers to the result of the last expression evaluated. If your REPL doesn't do this, you can copy and past for the win.

## Reading from the queue

We can now try to read messages from our newly created (and hopefully empty) queue:

```clj
(sqs/receive-message queue-url)
;;=> {:messages []}
```

`receive-message` blah blah blah defaults

If we want more control, we can instruct the client to wait a certain amount of time for messages to arrive before returning, and limit the maximum number of messages we get back.

```clj
(sqs/receive-message :queue-url queue-url
                     :wait-time-seconds 5
                     :max-number-of-messages 5)
```

Of course, this is boring without any messages to receive, so let's put a few on the queue!

## Writing to the queue

Since our messages are JSON, we'll need to be able to encode them as such. We'll use the Cheshire library for that:

```clj
(require '[cheshire.core :as json])
```

We can define a test event, then try encoding it as JSON.

```clj
(def event {:date "2011-12-03T10:15:30Z"
            :amount 4285
            :paymentMethod "SLICE_IT"
            :merchantId "1bb53ed1-787b-4543-9def-ea18eef7902e"})

(json/generate-string event)
;;=> {"date":"2011-12-03T10:15:30Z","amount":4285,"paymentMethod":"SLICE_IT","merchantId":"1bb53ed1-787b-4543-9def-ea18eef7902e"}
```

Looks good! Now to put it on the queue.

```clj
(sqs/send-message queue-url (json/generate-string event))
```

Let's see if we can read that event back.

```clj
(sqs/receive-message queue-url)
```

Sure enough, that returned something like this:

```clj
{:messages
 [{:body
   "{\"date\":\"2011-12-03T10:15:30Z\",\"amount\":4285,\"paymentMethod\":\"SLICE_IT\",\"merchantId\":\"1bb53ed1-787b-4543-9def-ea18eef7902e\"}",
   :md5of-body "e832830f9eae61cb1f0bba12a0cea3f2",
   :message-id "c4b77902-c5af-4963-9f73-f33d66bc7b31",
   :receipt-handle
   "AQEBNr4FoHL1Am5X037HwDBTArJ5ZYqCqD4E/rGm7K5KWSsOiVmP5JlqMai6Yl+mg4l/UMieL+LmHnlrXSFHMxyYoBqNJAMS+fDAXmWgFfw+U/ej1pHntBv+2Xr4KTAdT2LBuqo4XMe+5TAAwEHtA/bfT5U7tyKT+5F5oEzf7GhtqM+I885sUpsW7569QsutI1QbSOSzXwYGm5RiRw/g22sJxGa8+iaDQTPgU0pPJlJrLrVMBrrr/uHPJGunk/SgRtFp2hpamKLCDcZhzXM2GelO10+db9Nn/P3TCNRhdwvpV3a3XqS4hQBttTAyi3Ko9vtB6QBFXAQVXXe8nDnn2GJ+dy3oWBnspvHafuJD7u6gBC4fXpsA+KaHYBkFhDAM+ZTt7Y7YBp4doDJ2uPMJKV1yfw=="}]}
```

Let's bind the message so we can play around with it:

```clj
(def msg (-> *1 :messages first))
```

## Dealing with messages

Decode

Why's it still there?

```clj
(sqs/receive-message queue-url)
```

Delete

```clj
(sqs/delete-message (assoc msg :queue-url queue-url))
```

## Making a namespace

## Aggregating events

## Storing events
