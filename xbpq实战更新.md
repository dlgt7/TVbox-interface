首先，xbpq是用最少的参数实现网站配置，所以拿到一个网站，不要盲目的想着把所有参数都提取出来，要根据实际情况，添加需要的参数。
下边就陆续开始根据日常写源的情况，汇总一下。慢慢加强对教程的理解。

https://www.hongguodj1.cc  今天拿红果短剧来练练手。

首先，写最基本的，这个不管是简写还是全写都需要写的，写完看看情况，jar里是有一些内置模板的，如果网站结构和内置模板一样，则能正常显示，不能正常显示，则根据情况，添加相应的参数：

{

    "主页url": "https://www.hongguodj1.cc/",
    "分类url": "https://www.hongguodj1.cc/vod/type/id/{cateId}/page/{catePg}.html",	
    "分类": "短剧大全$duanjudaquan#闪婚离婚$shanhunlihun#女频恋爱$nvpinlianai#反转爽剧$fanzhuanshuangju#脑洞悬疑$naodongxuanyi#年代穿越$niandaichuanyue#现代都市$xiandaidushi",
  }
  
{

    "主页url": "https://www.hongguodj1.cc/",
    "分类url": "https://www.hongguodj1.cc/vod/type/id/{cateId}/page/{catePg}.html",	
    "分类": "短剧大全$duanjudaquan#闪婚离婚$shanhunlihun#女频恋爱$nvpinlianai#反转爽剧$fanzhuanshuangju#脑洞悬疑$naodongxuanyi#年代穿越$niandaichuanyue#现代都市$xiandaidushi",
    "图片": "data-src=\"&&\"",
    "图片前缀": "https://www.hongguodj1.cc",
    "嗅探词": "m3u8#mp4#flv"
  
  }
