首先，xbpq是用最少的参数实现网站配置，所以拿到一个网站，不要盲目的想着把所有参数都提取出来，要根据实际情况，添加需要的参数。
下边就陆续开始根据日常写源的情况，汇总一下。慢慢加强对教程的理解。

https://www.hongguodj1.cc  今天拿红果短剧来练练手。

首先，写最基本的，这个不管是简写还是全写都需要写的，写完看看情况，jar里是有一些内置模板的，如果网站结构和内置模板一样，则能正常显示，不能正常显示，则根据情况，添加相应的参数：

{

    "主页url": "https://www.hongguodj1.cc/",
    "分类url": "https://www.hongguodj1.cc/vod/type/id/{cateId}/page/{catePg}.html",	
    "分类": "短剧大全$duanjudaquan#闪婚离婚$shanhunlihun#女频恋爱$nvpinlianai#反转爽剧$fanzhuanshuangju#脑洞悬疑$naodongxuanyi#年代穿越$niandaichuanyue#现代都市$xiandaidushi",
  }

这个网站还是比较简单的，写了三个参数就能看了，没有海报图片，下边我们来添加以下这个参数，再看看效果。
![ec1b72d3f025044a24b0f6610ecd1369](https://github.com/user-attachments/assets/f4086243-36c5-4899-ab48-d33390cdb5d2)    ![8feb9110449a9183c80a99158e5ab1a0](https://github.com/user-attachments/assets/a6e2d855-875e-4d26-b5e9-75b56171c34f)

{

    "主页url": "https://www.hongguodj1.cc/",
    "分类url": "https://www.hongguodj1.cc/vod/type/id/{cateId}/page/{catePg}.html",	
    "分类": "短剧大全$duanjudaquan#闪婚离婚$shanhunlihun#女频恋爱$nvpinlianai#反转爽剧$fanzhuanshuangju#脑洞悬疑$naodongxuanyi#年代穿越$niandaichuanyue#现代都市$xiandaidushi",
    "图片": "data-src=\"&&\"",
    "图片前缀": "https://www.hongguodj1.cc"
    
  }

  现在已经能正常显示图片了，愉快的观影吧。后续会陆续汇总其它参数的添加，今天这几个参数都比较简单，就不贴网站源码了，照着教程就能实战。
  ![0a82e26cd0342dccc5fb66b97b6ddcd3](https://github.com/user-attachments/assets/f15e6c77-f805-45f2-81b2-0ea8e93161cf)

