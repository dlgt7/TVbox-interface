name: Spider

on:
  workflow_dispatch:

jobs:
  build:

    runs-on: ubuntu-latest

    steps:
    - uses: actions/checkout@v4
    - name: set up JDK
      uses: actions/setup-java@v3.11.0
      with:
        java-version: '18'
        distribution: 'temurin'
        
    - name: Clone project
      run: |
        rm -rf project
        rm -rf jar/custom_spider.jar
        git clone --recurse-submodules https://github.com/FongMi/CatVodSpider project
      
    - name: Customize Spider
      working-directory: ./project
      run: |
         sed -i 's/gradle-7.4.2-all/gradle-7.5-bin/g' gradle/wrapper/gradle-wrapper.properties
         
        
         
         
         #简体化阿里云盘
         sed -i 's/原畫/原画/g;s/普畫/普画/g;s/轉存/转存/g;s/極速/极速/g' app/src/main/java/com/github/catvod/spider/Ali.java
         sed -i 's/原畫/原画/g;s/普畫/普画/g;s/轉存/转存/g;s/極速/极速/g;s/阿里雲盤/阿里云盘/g' app/src/main/java/com/github/catvod/api/AliYun.java
         curl -L https://github.com/oiltea/CatVodSpider/raw/9ab5cc627be4096db11bca9ee9177c13014ddadd/app/src/main/java/com/github/catvod/spider/Wogg.java > app/src/main/java/com/github/catvod/spider/Wogg.java
         sed -i 's/thread = 10/thread = 64/g' app/src/main/java/com/github/catvod/api/AliYun.java


    - name: Build the app
      working-directory: ./project
      run: |        
         chmod +x gradlew
         ./gradlew assemblerelease --build-cache --parallel --daemon --warning-mode all
         
    - name: Customize Spider Jar
      working-directory: ./project
      run: |        
         rm -rf jar/custom_spider.jar
         rm -rf jar/spider.jar/original/META-INF
         curl -L https://github.com/iBotPeaches/Apktool/releases/download/v2.7.0/apktool_2.7.0.jar > jar/3rd/apktool_2.7.0.jar
         java -jar jar/3rd/baksmali-2.5.2.jar d app/build/intermediates/dex/release/minifyReleaseWithR8/classes.dex -o jar/Smali_classes
         mkdir -p jar/spider.jar/smali/com/github/catvod/
         mv jar/Smali_classes/com/github/catvod/spider jar/spider.jar/smali/com/github/catvod/         
         java -jar jar/3rd/apktool_2.7.0.jar b jar/spider.jar -c
         mv jar/spider.jar/dist/dex.jar ../jar/custom_spider.jar
         md5=($(md5sum ../jar/custom_spider.jar))
         echo $md5 > ../jar/custom_spider.jar.md5
    - name: Upload APK
      uses: actions/upload-artifact@v3.1.2
      with:
        name: Spider
        path: ./jar/custom_spider.jar

    - name: Update spider jar      
      uses: EndBug/add-and-commit@v9.1.3
      with:
        default_author: github_actions
        message: 'update spider jar'
        add: "['./jar/custom_spider.jar', './jar/custom_spider.jar.md5']"        
