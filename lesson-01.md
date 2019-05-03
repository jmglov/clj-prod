# Clojure in Production - Lesson 1

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

### List you a table

Let's start by seeing what Dynamo tables already exist in our AWS account. In your `user` namespace, require in Amazonica's Dynamo client:

```clj
(require '[amazonica.aws.dynamodbv2 :as dynamo])
```

Now we can call Dynamo client functions, so let's go ahead and list those tables!

```clj
(dynamo/list-tables)
;;=> {:table-names ["maybe-some-stuff-here"]}
```

So, how did we know that the function that lists tables is called `list-tables`? Well, we have three ways of knowing:
- A lucky guess
- The [Amazonica documentation for Dynamo](https://github.com/mcohen01/amazonica#dynamodbv2) (which actually doesn't mention `list-tables`)
- The [AWS Java SDK documenation for Dynamo](https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/dynamodbv2/AmazonDynamoDBClient.html)

I typically opt for the official AWS docs, as they are the ultimate source of truth for Amazonica. To understand why that is, we need to understand a bit more about what Amazonica is.

Whilst preparing this workshop, I discovered that Amazonica is now included on the excellent [cljdoc website](https://cljdoc.org/d/amazonica/amazonica/0.3.139/doc/readme). cljdoc doesn't give the level of detail that the official AWS docs do, but it gives you an easy way to see what functions exist in each Amazonica namespace.

[Amazonica's documentation](https://github.com/mcohen01/amazonica#documentation) reveals that:
> Amazonica reflectively delegates to the Java client library, as such it supports the complete set of remote service calls implemented by each of the service-specific AWS client classes (e.g. AmazonEC2Client, AmazonS3Client, etc.) [...] Reflection is used to create idiomatically named Clojure Vars in the library namespaces corresponding to the AWS service. camelCase Java methods become lower-case, hyphenated Clojure functions.

What all this means is that if you see an instance method like [AmazonDynamoDBClient.listTables](https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/dynamodbv2/AmazonDynamoDBClient.html#listTables--), Amazonica will have a function called [list-tables](https://cljdoc.org/d/amazonica/amazonica/0.3.139/api/amazonica.aws.dynamodbv2#list-tables).

Amazonica also [converts return types](https://github.com/mcohen01/amazonica#conversion-of-returned-types) into good old Clojure maps (and other collections, as needed). This is why `list-tables` returned a map instead of a [ListTablesResult](https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/dynamodbv2/model/ListTablesResult.html) object.

Finally, Amazonica [coerces arguments](https://github.com/mcohen01/amazonica#argument-coercion), so you can use normal Clojure values and collections, and all will be right with the world. We'll see this in action when we...

### Create a table

Let's consult the AWS documentation to see how to create a table. We quickly find the [AmazonDynamoDBClient.createTable()](https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/dynamodbv2/AmazonDynamoDBClient.html#createTable-com.amazonaws.services.dynamodbv2.model.CreateTableRequest-) method, which takes as its argument a [CreateTableRequest](https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/dynamodbv2/model/CreateTableRequest.html). We know that Amazonica is going to give us an idiomatically named Clojure function called `create-table`, but what arguments does it need?

To determine that, we could cheat and look at the [Amazonica documentation](https://github.com/mcohen01/amazonica#dynamodbv2), but since we want to learn how to fish, let's do it the hard way.

Take a look at the [CreateTableRequest](https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/dynamodbv2/model/CreateTableRequest.html) class, remembering that it is nothing but a POJO (Plain Old Java Object--also known as a Java Bean). The sole purpose of a POJO is to carry data around. Does that sound like anything we use all the time in Clojure?

Amazonica does a neat trick of letting you substitute a map for any POJO in the AWS SDK. Fields become idiomatically named Clojure keywords, so the `tableName` field of the [CreateTableRequest](https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/dynamodbv2/model/CreateTableRequest.html) class becomes the keyword `:table-name`. And we know there's a field called `tableName` because there's a getter called [getTableName()](https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/dynamodbv2/model/CreateTableRequest.html#getTableName--).

So, in order to see what fields are in an AWS POJO, look at the getters. In the case of [CreateTableRequest](https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/dynamodbv2/model/CreateTableRequest.html), we have a few:
- `getAttributeDefinitions()`
- `getBillingMode()`
- `getGlobalSecondaryIndexes()`
- `getKeySchema()`
- `getLocalSecondaryIndexes()`
- `getProvisionedThroughput()`
- `getSSESpecification()`
- `getStreamSpecification()`
- `getTableName()`

Out of those, some are optional. To determine which ones are required, we can take a look at `CreateTableRequest`'s [convenience constructor](https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/dynamodbv2/model/CreateTableRequest.html#CreateTableRequest-java.util.List-java.lang.String-java.util.List-com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput-). We can see that it takes only four arguments:
- `attributeDefinitions`
- `tableName`
- `keySchema`
- `provisionedThroughput`

And now we know what the required arguments are.

Before we start building up a map to create a table, let's take a step back and think about what we need to put in there.

Dynamo is a key/value store at heart (though it has some cool document features as well). Each Dynamo table **must** have a _partition key_, which **must** be unique. A Dynamo table may also have a _sort key_, in which case the combination of partition key and sort key must be unique.

For example, imagine a table in which we're keeping a list of people. We can choose `id` as our partition key, and stuff a personal number or UUID or something in there.

Now imagine the same table, except that we want to restrict people lookups to the company they work for. We can now choose `company_id` as our partition key, and `id` as our sort key. Now, we need to look people up by providing a `company_id` and an `id`. We can also query the table for all people that work for a specific company.

Our choice of keys becomes the list of [KeySchemaElement](https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/dynamodbv2/model/KeySchemaElement.html)s required to create a table.

Furthermore, Dynamo needs to know a little bit about the types of the attributes used as keys, which is what the list of [AttributeDefinition](https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/dynamodbv2/model/AttributeDefinition.html)s is all about. The types supported for keys are strings, numbers, and binaries, but we'll only be using strings.

The last thing Dynamo needs to know in order to create a table is how many reads and writes per second you're expecting. Read and write throughput are provisioned independently in Dynamo, so you can optimise for write-heavy or read-heavy workloads. This means that the [ProvisionedThroughput](https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/dynamodbv2/model/ProvisionedThroughput.html) class has two fields we'll need to set:
- `readCapacityUnits`
- `writeCapacityUnits`

And how did we know this? By looking at its getters, naturally.

We're about ready to create a table now, but first we need to figure out what we're planning to keep in it. How about we build a reporting service, where we'll get a stream of incoming events and aggregate them based on a set of report definitions. We'll store those aggregated datapoints in a table called `clj-prod-YOURNAME-aggregates`, which will look like this:

| datapoint                    | events |
| ---------------------------- | ------ |
| 2011-12-03:10,10-50,SLICE_IT |     27 |
| merchant1,SLICE_IT           |     42 |

So we have a simple partition key called `datapoint`, which is a string, and another attribute called `events`, which is a number. When we create a table, Dynamo actually only needs information about attributes that are keys, so we can forget about events entirely.

After all of our detective and design work, we can now assemble a Clojure map to represent a [CreateTableRequest](https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/dynamodbv2/model/CreateTableRequest.html) POJO:

```clj
{:table-name "clj-prod-YOURNAME-aggregates"
 :key-schema [{:attribute-name "datapoint"
               :key-type "HASH"}]
 :attribute-definitions [{:attribute-name "datapoint"
                          :attribute-type "S"}]
 :provisioned-throughput {:read-capacity-units 1
                          :write-capacity-units 1}}
```

Note that _hash key_ is the old name for _partition key_, and thus lives on as the `HASH` key type in the key schema.

Now that we have our map, we need to feed it to `create-table`. However, we don't need the hard `{}` shell around the map, we can just hand the inner goodies to Amazonica:

```clj
(dynamo/create-table :table-name "clj-prod-YOURNAME-aggregates"
                     :key-schema [{:attribute-name "datapoint"
                                   :key-type "HASH"}]
                     :attribute-definitions [{:attribute-name "datapoint"
                                              :attribute-type "S"}]
                     :provisioned-throughput {:read-capacity-units 1
                                              :write-capacity-units 1})
```

If we list our tables again, we should now see the one we just created:

```clj
(dynamo/list-tables)
;;=> {:table-names ["clj-prod-YOURNAME-aggregates"]}
```

We can see if the table is finished creating by calling `describe-table` on it:

```clj
(dynamo/describe-table "clj-prod-YOURNAME-aggregates")
;;=> {:table {:table-name "clj-prod-YOURNAME-aggregates", :table-status "ACTIVE", }}
```

Yay! It's done creating (status `ACTIVE`), so let's go ahead and

### Put an item in the table

This part is a snap with Clojure, as maps just slide right into Dynamo. Let's add the two sample entries from above:

| datapoint                    | events |
| ---------------------------- | ------ |
| 2011-12-03:10,10-50,SLICE_IT |     27 |
| merchant1,SLICE_IT           |     42 |

We can use the `put-item` function to make this happen in a jiffy!

```clj
(dynamo/put-item :table-name "clj-prod-YOURNAME-aggregates"
                 :item {:datapoint "2011-12-03:10,10-50,SLICE_IT"
                        :items 27})
(dynamo/put-item :table-name "clj-prod-YOURNAME-aggregates"
                 :item {:datapoint "merchant1,SLICE_IT"
                        :items 42})
```

That was easy! Maybe a bit too easy... how can we be sure that those items made it into Dynamo? Well, we can use the `scan` function to list all items in the table:

```clj
(dynamo/scan :table-name "clj-prod-YOURNAME-aggregates")
;;=>
#_{:items
   [{:datapoint "merchant1,SLICE_IT", :items 42}
    {:datapoint "2011-12-03:10,10-50,SLICE_IT", :items 27}],
   :scanned-count 2,
   :count 2}
```

Miracle of miracles, they're both there!

Scanning, by the way, is not a thing you should be doing very often in production, as it consumes massive amounts of read capacity for a table that contains a lot of data. It's fine during development, as those tables tend to be pretty small.

Also note that `scan` may not return all items in the table; if there are many items, Dynamo will page results, and you'll have to iterate through pages manually.

In our reporting system, we'll assume that we'll be looking up aggregates by datapoint. That will use `get-item`:

```clj
(dynamo/get-item :table-name "clj-prod-YOURNAME-aggregates"
                 :key {:datapoint {:s "2011-12-03:10,10-50,SLICE_IT"}})
```

### The db namespace

We've done a lot of exploring in the REPL, and now seems like a good time to persist some of those experiments as source code.

Create a `src/db.clj` file, and require in Amazonica's DynamoDB namespace:

```clj
(ns db
  (:require [amazonica.aws.dynamodbv2 :as dynamo]))
```

As the table name shouldn't be changing very often, we can define it in this namespace:

```clj
(def table-name "clj-prod-YOURNAME-aggregates")
```

Let's start with a `put!` function. We can copy the `dynamo/put-item` expression from our REPL right into the body of the function, then fix up the table name and argument:

```clj
(defn put! [aggregate]
  (dynamo/put-item :table-name table-name
                   :item aggregate))
```

If we evaluate the file and enter the namespace in our REPL, we can try it:

```clj
(put! {:datapoint "merchant1,SLICE_IT", :items 42})
```

Next, let's make our `dynamo/get-item` REPL experiment into a function. First, we should modify our namespace to exclude `clojure.core/get` so that we don't get a warning about shadowing it:

```clj
(ns db
  (:require [amazonica.aws.dynamodbv2 :as dynamo])
  (:refer-clojure :exclude [get]))
```

Now, we can write a `get` function:

```clj
(defn get [datapoint]
  (dynamo/get-item :table-name table-name
                   :key {:datapoint {:s datapoint}}))
```

And test it in the REPL (don't forget to evaluate the file first):

```clj
(get "merchant1,SLICE_IT")
```

## Spec to the rescue

One complaint about maintaining a large Clojure codebase is that, absent types, it can be challenging to remember what the data various functions take and return should look like.

Starting in Clojure 1.9, the [spec library](https://clojure.org/about/spec) was released, which gives us a way to describe the shape of data, assert that functions are receiving the correct thing, and even generate sample data in the right shape for testing functions.

### Getting spec

Let's start by adding `clojure.spec` (and, whilst we're at it, `test.check`) to our dependencies. If you're using `deps.edn`, add the following to your `:deps` map:

```clj
org.clojure/spec.alpha {:mvn/version "0.2.176"}
org.clojure/test.check {:mvn/version "0.10.0-alpha3"}
```

For Leiningen, you'll need this in your `:dependencies` vector:

```clj
[org.clojure/spec.alpha "0.2.176"]
[org.clojure/test.check "0.10.0-alpha3"]
```

If your Clojure environment has a way to hotload dependencies, do so now. Otherwise, you'll need to restart your REPL. :(

If you do restart your REPL, remember to do `(user/refresh-aws-credentials)` before you start playing with AWS again.

### An aggregate spec

Let's create a new namespace for our specs. In a file called `src/models.clj`, add an `ns` definition:

```clj
(ns models
  (:require [clojure.spec.alpha :as s]))
```

Let's think about the aggregate map that our `db/get` and `db/put!` functions use:

```clj
{:datapoint "merchant1,SLICE_IT"
 :items 42}
```

It has two keys:
- `:datapoint`, which is a string
- `:items`, which is an integer

In spec, we define maps using the [`s/keys` macro](https://clojure.org/guides/spec#_entity_maps), which specifies the required and optional keys in our map:

```clj
(s/keys :namespaced/keyword :req [:namespaced/keyword1
                                  :namespaced/keyword2]
                            :opt [:namespaced/keyword3])
```

The interesting thing about keys specs is that they simply name the keys in a map, but say nothing about the values. We need to define specs for the values independently.

So let's do that. We know that datapoints are strings. The simplest way to define a spec is to use a predicate, and Clojure has a perfect one for asking if a value is a string: `string?`. To define a spec, we use `s/def`:

```clj
(s/def :aggregate/datapoint string?)
```

Note that spec requires us to use namespaced keywords. We'll use the namespace `aggregate`, since we're describing properties of aggregates.

Now that we've specified what a datapoint should look like, we can use the spec to validate data:

```clj
(s/valid? :aggregate/datapoint 1) ; => false
(s/explain :aggregate/datapoint 1) ; prints "1 - failed: string? spec: :aggregate/datapoint"
(s/explain-data :aggregate/datapoint 1)
;;=>
#:clojure.spec.alpha{:problems
                     [{:path [],
                       :pred clojure.core/string?,
                       :val 1,
                       :via [:aggregate/datapoint],
                       :in []}],
                     :spec :aggregate/datapoint,
                     :value 1}
```

We'll define a spec for `:items` as well:

```clj
(s/def :aggregate/items pos-int?)
```

The `pos-int?` predicate returns true for any positive integral number, not just numbers that can be represented by the Java `Integer` class.

```clj
(pos-int? 43676312979512) ;=> true
```

Now that we have specs for both of the elements in our aggregate map, we can create a spec for the map itself.

```clj
(s/def :aggregate/aggregate (s/keys :req-un [:aggregate/datapoint
                                             :aggregate/items]))
```

Time for a test in the REPL:

```clj
(s/valid? :aggregate/aggregate {:datapoint "foo", :items 42}) ;=> true
(s/explain-data :aggregate/aggregate {:datapoint "foo"})
;;=>
#:clojure.spec.alpha{:problems
                     ({:path [],
                       :pred
                       (clojure.core/fn [%] (clojure.core/contains? % :items)),
                       :val {:datapoint "foo"},
                       :via [:aggregate/aggregate],
                       :in []}),
                     :spec :aggregate/aggregate,
                     :value {:datapoint "foo"}}
```

### Function specs

Now that we have specs for our aggregate, we can use them to document our functions in a way that Clojure can enforce.

Let's switch back to our `db` namespace and spec out the `get` function. First, we'll need to require spec itself and also our `models` namespace:

```clj
(ns db
  (:require [amazonica.aws.dynamodbv2 :as dynamo]
            [clojure.spec.alpha :as s]
            [models])
  (:refer-clojure :exclude [get]))
```

We won't be actually using the `models` namespace, but we need to make sure that it is evaluated before `db` so that the specs we define in `models` will be entered into the global spec registry by the time we use them.

Spec has an `fdef` macro that is used to spec the arguments and return value of a function, as well as any invariants that must hold.

Our `get` function takes a datapoint as its argument and returns an aggregate. Here's how we can say that with spec:

```clj
(s/fdef get
        :args (s/cat :datapoint :aggregate/datapoint)
        :ret :aggregate/aggregate)
```

The `:args` key to the `fdef` macro provides a spec for arguments. Function arguments are always a list in Clojure, and spec's `cat` is pretty much always used for argument specs. `cat` technically constructs a regular expression that applies to a list (regular expressions are actually a concept in Computer Science that apply to any sequence, not just strings of text), but we'll use it here as a glorified key-value list. `cat` wants a list of pairs of names and value specs. It is conventional to use the same names as your arguments in the `cat` spec.

`:ret` is rather more straightforward. Since functions can only return a single value, `:ret` only needs a single spec.

We can test this out by using spec's testing namespace.

```clj
(require '[clojure.spec.test.alpha :as stest])
(stest/instrument 'db/get)
(get 99)
```

Trying to call `get` with something that isn't a datapoint will throw an exception like this:

```
1. Unhandled clojure.lang.ExceptionInfo
   Spec assertion failed.

         Spec: #object[clojure.spec.alpha$regex_spec_impl$reify__2509 0x382cbd60 "clojure.spec.alpha$regex_spec_impl$reify__2509@382cbd60"]
        Value: (99)

     Problems: 

            val: 99
             in: [0]
         failed: string?
           spec: :aggregate/datapoint
             at: [:datapoint]

                 alpha.clj:  132  clojure.spec.test.alpha/spec-checking-fn/conform!
                 alpha.clj:  140  clojure.spec.test.alpha/spec-checking-fn/fn
               RestFn.java:  408  clojure.lang.RestFn/invoke
                      REPL:  467  db/eval19168
```

Unfortunately, `instrument` doesn't result in the function's return value being checked. There are libraries that do that, however, and I recommend [Orchestra](https://github.com/jeaye/orchestra)

Now that we know how to spec, we can add a spec to `put!` as well. `put!` takes an aggregate and returns, well, whatever `dynamo/put-item` does. We don't really care, so we can use the `any?` predicate, which returns true for any argument it's given. However, that might be confusing to the reader, so we can add a helper to spec to our `models` namespace that communicates our intent better:

```clj
(s/def :return/unit any?)
```

Now, back in `db`, we can spec `put!`:

```clj
(s/fdef put!
        :args (s/cat :aggregate :aggregate/aggregate)
        :ret :return/unit)
```

### Generating data from specs

One of spec's killer features is its integration with `test.check`, a Clojure port of Quickcheck, Haskell's amazing generative testing library.

But generative testing is for more than testing. It can level up your REPL game as well!

When you define a spec, it actually comes with a generator. Let's see what the default generators give us for the specs we've defined so far:

```clj
(require '[clojure.spec.gen.alpha :as sgen])
(sgen/generate (s/gen :aggregate/datapoint)) ;=> FCV4h
(sgen/generate (s/gen :aggregate/items))     ;=> 1352045
(sgen/generate (s/gen :aggregate/aggregate)) ;=> {:datapoint "2RE6m66xM7Nr2U9L", :items 6}
```

What's going on here? Well, the `sgen/generate` function takes a generator as its argument and returns a single generated value. The `s/gen` function takes a spec as an argument and returns a generator. Put them together and you get such magic!


## What's next?

Now we have a table, and a few pieces of test data in it. Let's move on to figuring out how to let our clients put lots of data into it!
