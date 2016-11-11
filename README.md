Android Native Dependencies Resolver Gradle Plugin
==================================================

As the official Android Gradle plugin cannot resolve the native dependencies, the .so files would not be copied
to the sub-directory of `jniLibs`. This plugin is aim to solve this problem, in addition to provide file rename
and abi filtering utility functions.

Also if you are building Android projects which depends native libraries with Maven and [android-maven-plugin](http://simpligility.github.io/android-maven-plugin/), this plugin helps migration from Maven to Gradle easily.

##Usage
**1. Introduce and apply the plugin**
```groovy
buildscript {
  repositories {
    maven {
      url "https://plugins.gradle.org/m2/"
    }
  }
  dependencies {
    classpath "gradle.plugin.com.github.linsea:native-dependencies-plugin:0.2.0"
  }
}

//Apply the plugin in the build.gradle of *APP* module, this is encouraged, or you may encounter this bug:
//https://code.google.com/p/android/issues/detail?id=158630
apply plugin: "com.github.linsea.native-dependencies"
```

**2. declare Native libraries dependencies**  
in the `dependencies` block, declare Native libraries dependencies, but add classifier and ext, for example:
```groovy
dependencies {
    compile "org.ffmpeg:algorithm:1.2.3:armeabi-v7a@so"
}
```
If you are a maven user, you should understand that this corresponds to the following statement in maven:
```xml
<dependency>
    <groupId>org.ffmpeg</groupId>
    <artifactId>algorithm</artifactId>
    <version>1.2.3</version>
    <classifier>armeabi-v7a</classifier>
    <type>so</type>
</dependency>
```

**3. configure .so file filtering and renaming rules**  
**3.1 `soFilters`**  
Add a `nativeso` block in the `build.gradle` of the app module, the block contains a `soFilters`attribute, according these values, the plugin finds the .so files about that ABI and copies them to subdirectory of `jniLibs`(default value is src/main/jniLibs), thus the .so file name in the remote repository should contains the ABI value keyword, otherwise the plugin cannot handle them.
**3.2 `renaming`**  
The plugin provides a renaming chance while copying .so files, renames files based on a regular expression,
you can define some attributes in the renaming block:  
  `prefix` : optional, defines the prefix of the file name after renaming.  
  `regex` : mandatory, defines the file name pattern which would be renamed, uses java.util.regex type of regular expressions.  
  `replaceAll` : defines the file name replacement when the file name matches the `regex`, i.e. call [String.replaceAll(regex,replaceAll)](http://docs.oracle.com/javase/8/docs/api/java/lang/String.html#replaceAll-java.lang.String-java.lang.String-) on filename.  
  `replaceWith` : replacement string (use $ syntax for capture groups in the source regular expression). details reference Gradle doc:[Copy task](https://docs.gradle.org/current/javadoc/org/gradle/api/file/CopySpec.html#rename(java.lang.String,%20java.lang.String)),for example:  
```groovy
 regex = '(.*)_OEM_BLUE_(.*)'
 replaceWith = '$1$2'
 ```
**NOTE**
You should define `replaceAll` or `replaceWith`, one of them, if both, `replaceWith` will be ignored.  
The file name try to match the `renaming` rule from top to below, if one hit, skip others.
If none hit, files will not be renamed.  

Here is a `nativeso` block configuration example:
```groovy
nativeso {

//.so filename MUST contains 'armeabi-v7a' or 'x86' to identify abi types
   soFilters = ['armeabi-v7a']     //['armeabi-v7a','x86']

//rename .so file: textsearch-1.2.3-armeabi-v7a.so -> libtextsearch.so
   renaming {
        prefix = 'lib'
        regex = 'textsearch-1.2.3-armeabi-v7a.*'
        replaceAll = 'textsearch' //not include ext .so
    }

//rename .so file: libhello-1.4.10-armeabi-v7a.so -> libhello.so
   renaming {
        regex = 'libhello(.*)'
        replaceWith = 'libhello'
    }

//rename .so file: libmysdk2-v7.0-armeabi-v7a.so -> libmysdk2.so
    renaming {
        regex = 'libmysdk(\\d+)-v(.*)'
        replaceWith = 'liblocSDK$1'
    }

//default renaming rule: name-version-armeabi-v7a.so -> libname.so
    renaming {
        prefix = 'lib'
        regex = '(.*)-([\\d\\.]+)-(.*)'
        replaceWith = '$1'
    }
}
```

##NOTES
The plugin make use of Gradle incremental build feature, it would not execute if the .so files are up to date, but if the files are out of date, the plugin would delete `jniLibs` directory, this cause problem if your APP module has already contains pre-build .so files in `jniLibs` directory(default:src/main/jniLibs), to avoid this problem, you should add a dedicated directory for the plugin, below snippet can achieve this:
```
android {
    sourceSets {
        main {
            //...
            jniLibs.srcDirs += 'src/main/nativeso' //MUST add to the last of the array
        }
    }
```

##TIPS
1. The plugin create a Task named `collectso`, if you want to debug the configuration, run below command:
```
./gradlew -q --rerun-tasks collectso --info
```
2. commonly your APP support limited ABI, others ABI .so files should be excluded from APK while packaging, in android plugin 2.2 and above, below configuration achieve desired effect:
```groovy
android {
    packagingOptions { //assume your APP support armeabi-v7a only
        exclude '/lib/armeabi/**'
        exclude '/lib/arm64-v8a/**'
        exclude '/lib/x86/**'
        exclude '/lib/x86_64/**'
        exclude '/lib/mips/**'
        exclude '/lib/mips64/**'
    }
}
```

##License

Copyright 2015  Android Native Dependencies Resolver Gradle Plugin author

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.


#中文版说明

Android 原生库依赖解析Gradle插件
==============================

由于官方的Android Gradle插件无法解析在`dependencies`中声明的.so库依赖,所以编译时不会把.so文件自动拷贝到`jniLibs`目录下,这个插件主要就是为了解决这个问题的,并且提供so文件重命名和abi过滤的实用功能.  
另外如果你是使用maven和[android-maven-plugin](http://simpligility.github.io/android-maven-plugin/)
构建Android项目,并且项目里面有native依赖库的,如果现在想转移到Gradle构建系统上来,那么这个插件正好合适.

##使用说明
**1. 引入插件**
```groovy
buildscript {
  repositories {
    maven {
      url "https://plugins.gradle.org/m2/"
    }
  }
  dependencies {
    classpath "gradle.plugin.com.github.linsea:native-dependencies-plugin:0.2.0"
  }
}

//在*主模块*项目中(比如APP项目中)应用插件,
//否则可能遇到Android官方插件中的这个BUG:https://code.google.com/p/android/issues/detail?id=158630
apply plugin: "com.github.linsea.native-dependencies"
```

**2. 声明Native依赖库**  
Native依赖库的声明还是像通常一样写在`dependencies`里面,但要加上classifier和ext,比如:
```groovy
dependencies {
    compile "org.ffmpeg:algorithm:1.2.3:armeabi-v7a@so"
}
```
如果你之前使用maven,应该明白这对应maven里面如下的声明:
```xml
<dependency>
    <groupId>org.ffmpeg</groupId>
    <artifactId>algorithm</artifactId>
    <version>1.2.3</version>
    <classifier>armeabi-v7a</classifier>
    <type>so</type>
</dependency>
```

**3. 配置so文件的过滤与重命名规则**  
**3.1 `soFilters`**  
 在主模块的build.gradle文件中添加一个`nativeso`块,里面必须有一个`soFilters`属性,表示项目支持的平台架构种类, 通过这个属性区分so文件的类别属于哪个平台架构,以便拷贝时把相应类别的so文件放到`jniLibs`下面的相应目录中. 被依赖的so文件的文件名中必须包含这个类别标识符,比如所有`armeabi-v7a`平台的相应so文件,在仓库中文件的命名 必须包含有`armeabi-v7a`这个串,否则无法区别这个so文件到底是属于哪个平台的,也就无法拷贝.
**3.2 `renaming`**  
 通常Linux平台的so库文件有一定的命名规则,比如有lib前缀,但是有些开发者提供的库并没有按这些规则来命名,如果 命名与加载时没有对应起来,Android是加载不到so库的.如果APP中引用多个库,而命名又五花八门,为了能使我们把so库 拷贝到`jniLibs`下后Android可以加载到库,有时我们需要重命名so文件.  
 **`renaming`**就是定义命名规则的,一条`renaming`规则可以对应一个或多个so文件的命名,`regex`是以正则表达式的 方式匹配so文件名,如果匹配到任意一次,则so文件名不会再继续匹配下一条`renaming`规则. 重命名时可以定义 `replaceAll`或者`replaceWith`的方式,两者同时定义时, 仅`replaceAll`生效而忽略`replaceWith`. 以`replaceAll`方式重命名时,实际上是在so filename上执行[String.replaceAll(regex,replaceAll)](http://docs.oracle.com/javase/8/docs/api/java/lang/String.html#replaceAll-java.lang.String-java.lang.String-). 而以`replaceWith`方式重命名时,则支持正则表达式的占位符($+数字表示)替换,功能更加强大,几乎可以满足所有命名需求. 具体可以参考Gradle文档中[Copy的重命名](https://docs.gradle.org/current/javadoc/org/gradle/api/file/CopySpec.html#rename(java.lang.String,%20java.lang.String)).比如:
 ```
 regex = '(.*)_OEM_BLUE_(.*)'
 replaceWith = '$1$2'
 ```
 `prefix` so文件重命名后加入的前缀,一般需要lib前缀时可以配置这个.

**注意**
文件的重命名时,从上到下依次配置规则,如果有一个配置到了,则下面的规则会忽略路过,如果最终一个规则都没有匹配到,则文件不会重命名.

以下是一个`nativeso`的配置示例及说明:
```groovy
nativeso {

//.so filename MUST contains 'armeabi-v7a' or 'x86' to identify abi types
   soFilters = ['armeabi-v7a']     //['armeabi-v7a','x86']

//rename .so file: textsearch-1.2.3-armeabi-v7a.so -> libtextsearch.so
   renaming {
        prefix = 'lib'
        regex = 'textsearch-1.2.3-armeabi-v7a.*'
        replaceAll = 'textsearch' //not include ext .so
    }

//rename .so file: libhello-1.4.10-armeabi-v7a.so -> libhello.so
   renaming {
        regex = 'libhello(.*)'
        replaceWith = 'libhello'
    }

//rename .so file: libmysdk2-v7.0-armeabi-v7a.so -> libmysdk2.so
    renaming {
        regex = 'libmysdk(\\d+)-v(.*)'
        replaceWith = 'liblocSDK$1'
    }

//默认命名规则: name-version-armeabi-v7a.so -> libname.so
    renaming {
        prefix = 'lib'
        regex = '(.*)-([\\d\\.]+)-(.*)'
        replaceWith = '$1'
    }
}
```

##注意事项
插件会把so文件拷贝到jniLibs下对应的目录下,比如jniLibs/armeabi-v7a,而且会利用Gradle提供的缓存机制,提高编译速度,但是当依赖更新而过期时,需要重新下载并拷贝,在拷贝之前,插件会清空jniLibs目录.因此如果你的主模块项目已经包含了一些native依赖,并且.so文件已经放到jniLibs下的对应目录下,那么插件会同时把这些so文件也删除.为了避免这种情况发生,需要增加一个单独
的jniLibs目录给插件使用,比如:
```
android {
    sourceSets {
        main {
            //...
            jniLibs.srcDirs += 'src/main/nativeso' //一定要加在数组的最后
        }
    }
```


##使用技巧
1. 插件增加了一个名为`collectso`的Task,并且利用了gradle的增量编译机制,如果文件是新的,Task不会重复执行.如果重命名时需要调试脚本,可以运行以下命令:
```
./gradlew -q --rerun-tasks collectso --info
```
查看插件的输出日志和重命名后的文件名,以便帮助定义重命名规则.

2. 通常,如果你的APP仅支持有限的几种ABI,则其他所有不支持的ABI的so文件应该在APK打包中排除掉,否则在某些机型中无法加载
对应ABI上缺失的so,在android plugin 2.2及以上版本可以通过如下配置达到效果:
```
android {
    packagingOptions { //假设你的APP仅支持armeabi-v7a架构,则其他的所有架构的so需要排除之
        exclude '/lib/armeabi/**'
        exclude '/lib/arm64-v8a/**'
        exclude '/lib/x86/**'
        exclude '/lib/x86_64/**'
        exclude '/lib/mips/**'
        exclude '/lib/mips64/**'
    }
}
```
