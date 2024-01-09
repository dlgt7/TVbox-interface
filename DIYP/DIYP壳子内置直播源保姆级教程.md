大家首先在安装mt管理器和np管理器，然后，进去np，点击左上角三杠，提取安装包（加固状态得是未加固的），！
![1](https://github.com/dlgt7/TVbox-interface/assets/102397160/30d54077-4fad-4bde-a0e8-621f1a0cf706)
![2](https://github.com/dlgt7/TVbox-interface/assets/102397160/47ecf72b-3a1c-4242-b56c-2ec698cba436)

点击查看，这时候会有四个东西，res文件夹是软件的图标、UI界面等一些东西，大家要是改图标的话，直接搜索png，然后图片替换就行（一般是png，部分是jpg）！
要想要改名字的话，AndroidManifest.xml，点开这个文件，然后在label中更改，看右下图片教程！
![3](https://github.com/dlgt7/TVbox-interface/assets/102397160/d71aa564-c54a-41d3-aa61-dfc0ae978182)
![4](https://github.com/dlgt7/TVbox-interface/assets/102397160/edb720fe-477a-48a7-98fb-364d53593ce8)

接下来就是内置源了，点击后缀为.dex文件（推荐mt管理器），然后
方式①搜索const-string v1, "CHANNEL_URL"，类型选择代码
方式②直接在浏览板块下，打开setting下的MySettings
![5](https://github.com/dlgt7/TVbox-interface/assets/102397160/1b639e78-c968-405a-a226-12f873c2b292)
![6](https://github.com/dlgt7/TVbox-interface/assets/102397160/8e27dfe7-f4d4-4a29-860e-ee1731afea64)
最后，大家更改下图中这两个位置的链接地址就行了（EPG地址是126行，直播源地址是300行），一定要记得在退出的时候保存它，还有就是全部更改完成后，重新签名一下（用mt管理器）。
![7](https://github.com/dlgt7/TVbox-interface/assets/102397160/422b290a-d614-4231-9105-5e2eb875d3e8)
![8](https://github.com/dlgt7/TVbox-interface/assets/102397160/61469beb-277f-499d-ae50-cc489d9b1d24)
