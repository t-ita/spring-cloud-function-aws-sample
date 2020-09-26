# Spring Cloud Function を使って AWS Lambda 関数を作る

# Spring Initializer を使ってプロジェクトを作成

* IntelliJ IDEA -> New Project 
* Spring Initializr を選択
* Project SDK -> 11
* Group / Artifact を適当に指定
* Dependencies に Spring Cloud 系は「入れない」
* Lombok は入れておく
* プロジェクトができたら、build.gradle を編集
  * org.springframework.boot:spring-boot-starter は不要なので削除
  * spring cloud function aws adapter を追加
  * aws-lambda-java-event / aws-lambda-java-core を追加
  * AWS Lambda にはFatJarが必要なので、shadowJarプラグインおよび設定を追加
  * できる限りJarサイズを小さくしたいので、thin-launcher プラグインを追加
  * Gradle ではメインクラスの指定が必要なので、manifest も追加

```
plugins {
    id 'org.springframework.boot' version '2.3.4.RELEASE'
    id 'io.spring.dependency-management' version '1.0.10.RELEASE'
    id 'java'
    id 'com.github.johnrengelman.shadow' version '6.0.0' // 追加
    id 'maven' // 追加
    id "org.springframework.boot.experimental.thin-launcher" version "1.0.24.RELEASE" // 追加
}

group = 'com.myexample'
version = '0.0.1-SNAPSHOT'
sourceCompatibility = '11'

configurations {
    compileOnly {
        extendsFrom annotationProcessor
    }
}

repositories {
    mavenCentral()
}

// 以下を追加
assemble.dependsOn = [shadowJar, thinJar]

jar {
    manifest {
        attributes 'Main-Class': 'com.myexample.serverless.aws.SpringCloudFunctionAwsSampleApplication'
    }
}

import com.github.jengelman.gradle.plugins.shadow.transformers.*

shadowJar {
    archiveClassifier = 'aws'
//    dependencies {
//        exclude(
//                dependency("org.springframework.cloud:spring-cloud-function-web:3.0.10.RELEASE"))
//    }
    // Required for Spring
    mergeServiceFiles()
    append 'META-INF/spring.handlers'
    append 'META-INF/spring.schemas'
    append 'META-INF/spring.tooling'
    transform(PropertiesFileTransformer) {
        paths = ['META-INF/spring.factories']
        mergeStrategy = "append"
    }
}
// 以上

dependencies {
    compile 'org.springframework.cloud:spring-cloud-function-adapter-aws:3.0.10.RELEASE' // 追加
    implementation 'com.amazonaws:aws-lambda-java-events:3.3.1' // 追加
    implementation 'com.amazonaws:aws-lambda-java-core:1.2.1' // 追加
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'
    testImplementation('org.springframework.boot:spring-boot-starter-test') {
        exclude group: 'org.junit.vintage', module: 'junit-vintage-engine'
    }
}

test {
    useJUnitPlatform()
}
```

# 関数作成
* functions 以下に Greet.java を作成
```
package com.myexample.serverless.aws.functions;

import lombok.Data;

import java.util.function.Function;

public class Greet implements Function<Greet.Greeting, Greet.Greeting> {

    @Data
    public static class Greeting {
        String name;
        String message;
    }

    @Override
    public Greeting apply(Greeting greeting) {
        var res = new Greeting();
        res.name = "Spring Cloud Function";
        res.message = String.format("Hello, %s", greeting.name);
        return res;
    }
}
```

# Handler を作成
* メインクラスと同じパッケージに、Lambdaのリクエストを受け取るためのハンドラ、EventHandlerを作成する
```
package com.myexample.serverless.aws;

import com.myexample.serverless.aws.functions.Greet;
import org.springframework.cloud.function.adapter.aws.SpringBootRequestHandler;

public class EventHandler extends SpringBootRequestHandler<Greet.Greeting, Greet.Greeting> {
}
```
* 基本的な機能は継承元に実装されているので、空白で構わない
* 今回は、Greeting クラスを受け取って Greeting クラスを返す関数なので、型引数に Greeting を指定

# application.properties を編集
* 関数クラスを読み込むように、以下を追加
```
spring.cloud.function.scan.packages=com.myexample.serverless.aws.functions
```

# jar 生成
* shadowJar タスクを実行して jar を作成

# AWS Lambda で関数を作成して、Jar を登録
* コールドスタートに時間がかかるのでタイムアウトを30秒に
* ランタイムは Java11
* メモリを 256MB
* ハンドラは `com.myexample.serverless.aws.EventHandler`

# テスト
* AWS Lambda コンソールの「テスト」を使う
* 以下のリクエストを投げる
```
{
  "name": "t-ita",
  "message": "Hello"
}
```
* 以下の結果が返る
```
{
  "name": "Spring Cloud Function",
  "message": "Hello, t-ita"
}
```

# API Gateway の設定
* このままでは、API Gateway のテストは失敗する
